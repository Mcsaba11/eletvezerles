package hu.lifeos.app.mods.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.mods.MOVE_TYPES
import hu.lifeos.app.mods.MoveDoc
import hu.lifeos.app.mods.MoveRec
import hu.lifeos.app.mods.ModsViewModel
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.EmptyHint
import hu.lifeos.app.ui.Fmt
import hu.lifeos.app.ui.KpiTile
import hu.lifeos.app.ui.MetricRing
import hu.lifeos.app.ui.PrimaryButton
import hu.lifeos.app.ui.SectionLabel
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.iconFor
import hu.lifeos.app.ui.theme.Noc
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Composable
fun moveTypeName(id: String): String = stringResource(
    when (id) {
        "walk" -> R.string.mv_walk; "run" -> R.string.mv_run; "bike" -> R.string.mv_bike; "gym" -> R.string.mv_gym
        "yoga" -> R.string.mv_yoga; "swim" -> R.string.mv_swim; "sport" -> R.string.mv_sport; else -> R.string.mv_other
    }
)

fun moveIcon(id: String): String = MOVE_TYPES.firstOrNull { it.first == id }?.second ?: "bolt"

object MoveStats {
    fun weekStart(d: LocalDate = LocalDate.now()): LocalDate = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    fun weekMinutes(m: MoveDoc, start: LocalDate = weekStart()): Int =
        m.items.filter { it.date >= start.toString() && it.date <= start.plusDays(6).toString() }.sumOf { it.moderateMinutes() }
    /** Egymást követő aktív napok (legalább 10 perc), a mai napot nem kötelező már teljesíteni. */
    fun streak(m: MoveDoc): Int {
        val days = m.items.groupBy { it.date }.mapValues { (_, l) -> l.sumOf { it.minutes } }
        var d = LocalDate.now().let { if ((days[it.toString()] ?: 0) >= 10) it else it.minusDays(1) }
        var n = 0
        while ((days[d.toString()] ?: 0) >= 10 && n < 3650) { n++; d = d.minusDays(1) }
        return n
    }
}

@Composable
fun MoveScreen(vm: ModsViewModel, onBack: () -> Unit) {
    val d by vm.move.state.collectAsState()
    var add by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf<MoveRec?>(null) }
    var settings by remember { mutableStateOf(false) }
    ModScaffold(
        stringResource(R.string.mod_move), stringResource(R.string.move_kicker), Noc.good, onBack, onAdd = { add = true },
        headerExtra = { IconButton(onClick = { settings = true }) { Icon(Icons.Outlined.Tune, stringResource(R.string.settings), tint = Noc.text2) } },
    ) {
        val wk = MoveStats.weekMinutes(d)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = ModListPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                AppCard(glow = Noc.good) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.move_this_week), style = MaterialTheme.typography.labelLarge, color = Noc.text2)
                            Text(stringResource(R.string.move_min_of, wk, d.weeklyGoal), style = MaterialTheme.typography.headlineSmall)
                            Text(stringResource(R.string.move_who), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                        }
                        MetricRing(wk.toFloat() / d.weeklyGoal.coerceAtLeast(1), Noc.good, Modifier.size(84.dp)) {
                            Text("${(wk * 100 / d.weeklyGoal.coerceAtLeast(1)).coerceAtMost(999)}%", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
            item {
                val start = MoveStats.weekStart()
                val days = (0..6).map { start.plusDays(it.toLong()) }
                val vals = days.map { day -> d.items.filter { it.date == day.toString() }.sumOf { it.minutes }.toDouble() }
                AppCard {
                    Text(stringResource(R.string.move_days), style = MaterialTheme.typography.titleMedium)
                    VSpace(10)
                    MiniBars(vals, days.map { Fmt.weekdayShort(it.dayOfWeek) }, Noc.good, goal = 30.0)
                }
            }
            item {
                val last30 = d.items.filter { it.date >= LocalDate.now().minusDays(29).toString() }
                val km = last30.mapNotNull { it.km }.sum()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiTile(stringResource(R.string.move_streak), stringResource(R.string.n_days, MoveStats.streak(d)), modifier = Modifier.weight(1f), accent = Noc.good)
                    KpiTile(stringResource(R.string.move_30), stringResource(R.string.move_hours, last30.sumOf { it.minutes } / 60), if (km > 0) String.format(Fmt.hu, "%.1f km", km) else "", Modifier.weight(1f), accent = Noc.cyan)
                }
            }
            if (d.items.isEmpty()) item { EmptyHint(stringResource(R.string.move_empty)) }
            else item { SectionLabel(stringResource(R.string.move_log)) }
            items(d.items.sortedWith(compareByDescending<MoveRec> { it.date }.thenByDescending { it.t }).take(60), key = { it.id }) { r ->
                AppCard(onClick = { edit = r }, padding = PaddingValues(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(iconFor(moveIcon(r.type)), Noc.good, 34)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(moveTypeName(r.type) + if (r.note.isNotBlank()) " · ${r.note}" else "", style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                            Text(runCatching { Fmt.dayLong(LocalDate.parse(r.date)) }.getOrDefault(r.date), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.n_min, r.minutes), style = MaterialTheme.typography.titleSmall)
                            r.km?.let { Text(String.format(Fmt.hu, "%.1f km", it), style = MaterialTheme.typography.bodySmall, color = Noc.text2) }
                        }
                    }
                }
            }
        }
    }
    if (add || edit != null) MoveSheet(vm, edit, onDismiss = { add = false; edit = null })
    if (settings) {
        var g by remember { mutableStateOf(d.weeklyGoal.toString()) }
        FormSheet(stringResource(R.string.move_goal), onDismiss = { settings = false }) {
            NumIn(g, { g = it }, stringResource(R.string.move_goal), suffix = stringResource(R.string.min_week))
            Text(stringResource(R.string.move_who), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
            PrimaryButton(stringResource(R.string.save), onClick = {
                g.toIntOrNull()?.let { v -> vm.move.update { it.copy(weeklyGoal = v.coerceIn(10, 2000)) } }; settings = false
            }, modifier = Modifier.fillMaxWidth(), color = Noc.good)
        }
    }
}

@Composable
private fun MoveSheet(vm: ModsViewModel, r: MoveRec?, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf(r?.type ?: "walk") }
    var minutes by remember { mutableStateOf((r?.minutes ?: 30).toString()) }
    var intensity by remember { mutableStateOf(r?.intensity ?: 2) }
    var km by remember { mutableStateOf(r?.km?.toString() ?: "") }
    var note by remember { mutableStateOf(r?.note ?: "") }
    var date by remember { mutableStateOf(r?.date ?: LocalDate.now().toString()) }
    FormSheet(stringResource(R.string.move_add), onDismiss) {
        ChipRow(MOVE_TYPES.map { it.first }, type, { moveTypeName(it) }, Noc.good) { type = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumIn(minutes, { minutes = it }, stringResource(R.string.duration), Modifier.weight(1f), suffix = stringResource(R.string.min_short))
            NumIn(km, { km = it }, stringResource(R.string.distance_opt), Modifier.weight(1f), suffix = "km", decimal = true)
        }
        Text(stringResource(R.string.move_intensity), style = MaterialTheme.typography.labelLarge, color = Noc.text2)
        val l1 = stringResource(R.string.int_light); val l2 = stringResource(R.string.int_mid); val l3 = stringResource(R.string.int_hard)
        ChipRow(listOf(1, 2, 3), intensity, { when (it) { 1 -> l1; 2 -> l2; else -> l3 } }, Noc.good) { intensity = it }
        val todayT = stringResource(R.string.today); val yT = stringResource(R.string.yesterday)
        val t0 = LocalDate.now().toString(); val t1 = LocalDate.now().minusDays(1).toString()
        ChipRow(listOf(t0, t1), date, { if (it == t0) todayT else yT }, Noc.good) { date = it }
        TextIn(note, { note = it }, stringResource(R.string.field_note))
        PrimaryButton(stringResource(R.string.save), onClick = {
            val rec = (r ?: MoveRec()).copy(type = type, minutes = minutes.toIntOrNull() ?: 0, intensity = intensity, km = km.toNum(), note = note.trim(), date = date,
                t = r?.t?.ifBlank { null } ?: java.time.LocalTime.now().format(Fmt.time))
            vm.move.update { m -> m.copy(items = if (r == null) m.items + rec else m.items.map { if (it.id == r.id) rec else it }) }
            onDismiss()
        }, enabled = (minutes.toIntOrNull() ?: 0) > 0, modifier = Modifier.fillMaxWidth(), color = Noc.good)
        if (r != null) TextButton(onClick = { vm.move.update { m -> m.copy(items = m.items.filter { it.id != r.id }) }; onDismiss() }) {
            Text(stringResource(R.string.delete), color = Noc.coral)
        }
    }
}

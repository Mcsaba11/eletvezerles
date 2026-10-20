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
import androidx.compose.material.icons.outlined.Bedtime
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
import hu.lifeos.app.mods.ModsViewModel
import hu.lifeos.app.mods.SleepDoc
import hu.lifeos.app.mods.SleepRec
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.EmptyHint
import hu.lifeos.app.ui.Fmt
import hu.lifeos.app.ui.KpiTile
import hu.lifeos.app.ui.MetricRing
import hu.lifeos.app.ui.PrimaryButton
import hu.lifeos.app.ui.SectionLabel
import hu.lifeos.app.ui.TimePickDialog
import hu.lifeos.app.ui.TonalButton
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.theme.Noc
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.sqrt

fun hm(min: Int): String = "${min / 60} ó ${(min % 60).toString().padStart(2, '0')} p"

object SleepStats {
    fun recent(d: SleepDoc, days: Int): List<SleepRec> {
        val from = LocalDate.now().minusDays(days.toLong() - 1).toString()
        return d.nights.filter { it.date >= from }
    }
    fun avgMin(l: List<SleepRec>): Int = if (l.isEmpty()) 0 else l.sumOf { it.minutes() } / l.size

    /** A lefekvési idő szórása percben (éjfél körül is helyesen). */
    fun bedSpread(l: List<SleepRec>): Int? {
        if (l.size < 3) return null
        val xs = l.map { r -> runCatching { LocalTime.parse(r.bed) }.getOrNull()?.let { t -> val m = t.hour * 60 + t.minute; if (m < 12 * 60) m + 1440 else m } ?: return null }
        val avg = xs.average()
        return sqrt(xs.sumOf { (it - avg) * (it - avg) } / xs.size).toInt()
    }
}

@Composable
fun SleepScreen(vm: ModsViewModel, onBack: () -> Unit) {
    val d by vm.sleep.state.collectAsState()
    var edit by remember { mutableStateOf<SleepRec?>(null) }
    var add by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    val target = (d.targetHours * 60).toInt()
    ModScaffold(
        stringResource(R.string.mod_sleep), stringResource(R.string.sleep_kicker), Noc.violet, onBack,
        onAdd = { add = true },
        headerExtra = { IconButton(onClick = { settings = true }) { Icon(Icons.Outlined.Tune, stringResource(R.string.settings), tint = Noc.text2) } },
    ) {
        val week = SleepStats.recent(d, 7)
        val last = d.nights.maxByOrNull { it.date }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = ModListPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                AppCard(glow = Noc.violet, onClick = { if (last != null) edit = last else add = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.sleep_last_night), style = MaterialTheme.typography.labelLarge, color = Noc.text2)
                            if (last == null) Text(stringResource(R.string.sleep_empty), style = MaterialTheme.typography.bodyMedium, color = Noc.text2)
                            else {
                                Text(hm(last.minutes()), style = MaterialTheme.typography.headlineMedium)
                                Text("${last.bed} – ${last.wake}" + (last.quality?.let { " · " + stringResource(R.string.sleep_quality_n, it) } ?: ""),
                                    style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                            }
                        }
                        MetricRing(if (last != null) last.minutes().toFloat() / target else 0f, Noc.violet, Modifier.size(80.dp)) {
                            Icon(Icons.Outlined.Bedtime, null, tint = Noc.violet)
                        }
                    }
                }
            }
            item {
                val days = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
                val vals = days.map { day -> (d.nights.firstOrNull { it.date == day.toString() }?.minutes() ?: 0) / 60.0 }
                AppCard {
                    Text(stringResource(R.string.sleep_week), style = MaterialTheme.typography.titleMedium)
                    VSpace(10)
                    MiniBars(vals, days.map { Fmt.weekdayShort(it.dayOfWeek) }, Noc.violet, goal = d.targetHours)
                }
            }
            item {
                val avg = SleepStats.avgMin(week)
                val debt = week.sumOf { (target - it.minutes()).coerceAtLeast(0) }
                val q = week.mapNotNull { it.quality }
                val spread = SleepStats.bedSpread(week)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiTile(stringResource(R.string.sleep_avg), if (week.isEmpty()) "–" else hm(avg), modifier = Modifier.weight(1f), accent = Noc.violet)
                    KpiTile(stringResource(R.string.sleep_debt), if (week.isEmpty()) "–" else hm(debt), modifier = Modifier.weight(1f), accent = if (debt > 120) Noc.amber else Noc.good)
                }
                VSpace(8)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiTile(stringResource(R.string.sleep_quality), if (q.isEmpty()) "–" else String.format(Fmt.hu, "%.1f / 5", q.average()), modifier = Modifier.weight(1f), accent = Noc.violet)
                    KpiTile(stringResource(R.string.sleep_rhythm), spread?.let { if (it <= 30) stringResource(R.string.sleep_rhythm_ok) else "± ${it} p" } ?: "–",
                        stringResource(R.string.sleep_rhythm_sub), Modifier.weight(1f), accent = if ((spread ?: 0) <= 30) Noc.good else Noc.amber)
                }
            }
            item {
                AppCard {
                    Text(stringResource(R.string.sleep_tip_title), style = MaterialTheme.typography.titleSmall)
                    VSpace(4)
                    val avg = SleepStats.avgMin(week)
                    val spread = SleepStats.bedSpread(week) ?: 0
                    Text(
                        stringResource(
                            when {
                                week.isEmpty() -> R.string.sleep_tip_start
                                spread > 60 -> R.string.sleep_tip_rhythm
                                avg < target - 45 -> R.string.sleep_tip_short
                                else -> R.string.sleep_tip_good
                            }
                        ), style = MaterialTheme.typography.bodyMedium, color = Noc.text2,
                    )
                }
            }
            if (d.nights.isNotEmpty()) item { SectionLabel(stringResource(R.string.sleep_history)) }
            items(d.nights.sortedByDescending { it.date }.take(30), key = { it.date }) { n ->
                AppCard(onClick = { edit = n }, padding = PaddingValues(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(runCatching { Fmt.dayLong(LocalDate.parse(n.date)) }.getOrDefault(n.date), style = MaterialTheme.typography.bodyLarge)
                            Text("${n.bed} – ${n.wake}" + if (n.note.isNotBlank()) " · ${n.note}" else "", style = MaterialTheme.typography.bodySmall, color = Noc.text2, maxLines = 1)
                        }
                        Text(hm(n.minutes()), style = MaterialTheme.typography.titleSmall, color = if (abs(n.minutes() - target) <= 45) Noc.good else Noc.text)
                    }
                }
            }
        }
    }
    if (add || edit != null) SleepSheet(vm, edit, onDismiss = { add = false; edit = null })
    if (settings) {
        var h by remember { mutableStateOf(d.targetHours.toString().removeSuffix(".0")) }
        FormSheet(stringResource(R.string.sleep_settings), onDismiss = { settings = false }) {
            NumIn(h, { h = it }, stringResource(R.string.sleep_target), suffix = stringResource(R.string.hours_short), decimal = true)
            Text(stringResource(R.string.sleep_target_hint), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
            var bedGoal by remember { mutableStateOf(d.bedGoal) }
            var pickGoal by remember { mutableStateOf(false) }
            Text(stringResource(R.string.sleep_bed_goal), style = MaterialTheme.typography.labelLarge, color = Noc.text2)
            TonalButton(bedGoal, onClick = { pickGoal = true })
            Text(stringResource(R.string.sleep_bed_goal_hint), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
            PrimaryButton(stringResource(R.string.save), onClick = {
                h.toNum()?.let { v -> vm.sleep.update { it.copy(targetHours = v.coerceIn(4.0, 12.0), bedGoal = bedGoal) } }
                settings = false
            }, modifier = Modifier.fillMaxWidth(), color = Noc.violet)
            if (pickGoal) TimePickDialog(runCatching { LocalTime.parse(bedGoal) }.getOrDefault(LocalTime.of(23, 0)), { pickGoal = false }) { bedGoal = it.format(Fmt.time) }
        }
    }
}

@Composable
private fun SleepSheet(vm: ModsViewModel, r: SleepRec?, onDismiss: () -> Unit) {
    val prev = vm.sleep.state.value.nights.maxByOrNull { it.date }
    var bed by remember { mutableStateOf(r?.bed ?: prev?.bed ?: "23:00") }
    var wake by remember { mutableStateOf(r?.wake ?: prev?.wake ?: "07:00") }
    var quality by remember { mutableStateOf(r?.quality) }
    var note by remember { mutableStateOf(r?.note ?: "") }
    val date = r?.date ?: LocalDate.now().toString()
    var pickBed by remember { mutableStateOf(false) }
    var pickWake by remember { mutableStateOf(false) }
    FormSheet(stringResource(R.string.sleep_log), onDismiss) {
        Text(runCatching { Fmt.dayLong(LocalDate.parse(date)) }.getOrDefault(date), style = MaterialTheme.typography.bodyMedium, color = Noc.text2)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.sleep_bed), style = MaterialTheme.typography.labelLarge, color = Noc.text2)
                TonalButton(bed, onClick = { pickBed = true }, modifier = Modifier.fillMaxWidth())
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.sleep_wake), style = MaterialTheme.typography.labelLarge, color = Noc.text2)
                TonalButton(wake, onClick = { pickWake = true }, modifier = Modifier.fillMaxWidth())
            }
        }
        Text(hm(SleepRec(date, bed, wake).minutes()), style = MaterialTheme.typography.titleLarge, color = Noc.violet)
        Text(stringResource(R.string.sleep_quality_q), style = MaterialTheme.typography.labelLarge, color = Noc.text2)
        ChipRow((1..5).toList(), quality, { "$it" }, Noc.violet) { quality = if (quality == it) null else it }
        TextIn(note, { note = it }, stringResource(R.string.field_note), placeholder = stringResource(R.string.sleep_note_hint))
        PrimaryButton(stringResource(R.string.save), onClick = {
            val rec = SleepRec(date, bed, wake, quality, note.trim())
            vm.sleep.update { s -> s.copy(nights = s.nights.filter { it.date != date } + rec) }
            onDismiss()
        }, modifier = Modifier.fillMaxWidth(), color = Noc.violet)
        if (r != null) TextButton(onClick = { vm.sleep.update { s -> s.copy(nights = s.nights.filter { it.date != r.date }) }; onDismiss() }) {
            Text(stringResource(R.string.delete), color = Noc.coral)
        }
    }
    if (pickBed) TimePickDialog(runCatching { LocalTime.parse(bed) }.getOrDefault(LocalTime.of(23, 0)), { pickBed = false }) { bed = it.format(Fmt.time) }
    if (pickWake) TimePickDialog(runCatching { LocalTime.parse(wake) }.getOrDefault(LocalTime.of(7, 0)), { pickWake = false }) { wake = it.format(Fmt.time) }
}

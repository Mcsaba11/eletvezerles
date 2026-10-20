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
import hu.lifeos.app.mods.LEARN_KINDS
import hu.lifeos.app.mods.LearnDoc
import hu.lifeos.app.mods.LearnItem
import hu.lifeos.app.mods.LearnSession
import hu.lifeos.app.mods.ModsViewModel
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.EmptyHint
import hu.lifeos.app.ui.Fmt
import hu.lifeos.app.ui.KpiTile
import hu.lifeos.app.ui.Meter
import hu.lifeos.app.ui.MetricRing
import hu.lifeos.app.ui.PrimaryButton
import hu.lifeos.app.ui.SectionLabel
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.iconFor
import hu.lifeos.app.ui.theme.Noc
import java.time.LocalDate

@Composable
private fun kindName(k: String) = stringResource(when (k) { "book" -> R.string.lk_book; "course" -> R.string.lk_course; "language" -> R.string.lk_language; else -> R.string.lk_skill })

@Composable
private fun unitName(k: String) = stringResource(when (k) { "book" -> R.string.unit_pages; "course" -> R.string.unit_lessons; else -> R.string.unit_units })

object LearnStats {
    fun minutesOn(d: LearnDoc, date: String) = d.sessions.filter { it.date == date }.sumOf { it.minutes }
    fun streak(d: LearnDoc): Int {
        val days = d.sessions.groupBy { it.date }
        var day = LocalDate.now().let { if (days.containsKey(it.toString())) it else it.minusDays(1) }
        var n = 0
        while (days.containsKey(day.toString()) && n < 3650) { n++; day = day.minusDays(1) }
        return n
    }
}

@Composable
fun LearnScreen(vm: ModsViewModel, onBack: () -> Unit) {
    val d by vm.learn.state.collectAsState()
    var add by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf<String?>(null) }
    var settings by remember { mutableStateOf(false) }
    ModScaffold(
        stringResource(R.string.mod_learn), stringResource(R.string.learn_kicker), Noc.ice, onBack, onAdd = { add = true },
        headerExtra = { IconButton(onClick = { settings = true }) { Icon(Icons.Outlined.Tune, stringResource(R.string.settings), tint = Noc.text2) } },
    ) {
        val today = LearnStats.minutesOn(d, LocalDate.now().toString())
        LazyColumn(Modifier.fillMaxSize(), contentPadding = ModListPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                AppCard(glow = Noc.ice) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.learn_today), style = MaterialTheme.typography.labelLarge, color = Noc.text2)
                            Text(stringResource(R.string.move_min_of, today, d.dailyGoal), style = MaterialTheme.typography.headlineSmall)
                            Text(stringResource(R.string.learn_streak, LearnStats.streak(d)), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                        }
                        MetricRing(today.toFloat() / d.dailyGoal.coerceAtLeast(1), Noc.ice, Modifier.size(80.dp)) {
                            Text("${(today * 100 / d.dailyGoal.coerceAtLeast(1)).coerceAtMost(999)}%", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
            item {
                val days = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
                AppCard {
                    Text(stringResource(R.string.learn_week), style = MaterialTheme.typography.titleMedium)
                    VSpace(10)
                    MiniBars(days.map { LearnStats.minutesOn(d, it.toString()).toDouble() }, days.map { Fmt.weekdayShort(it.dayOfWeek) }, Noc.ice, goal = d.dailyGoal.toDouble())
                }
            }
            val active = d.items.filter { !it.done }
            if (active.isEmpty()) item { EmptyHint(stringResource(R.string.learn_empty)) }
            else item { SectionLabel(stringResource(R.string.learn_in_progress)) }
            items(active, key = { it.id }) { it -> LearnRow(it) { open = it.id } }
            val done = d.items.filter { it.done }
            if (done.isNotEmpty()) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KpiTile(stringResource(R.string.learn_finished), "${done.size}", modifier = Modifier.weight(1f), accent = Noc.good)
                        KpiTile(stringResource(R.string.learn_total), stringResource(R.string.move_hours, d.sessions.sumOf { it.minutes } / 60), modifier = Modifier.weight(1f), accent = Noc.ice)
                    }
                }
                item { SectionLabel(stringResource(R.string.learn_done)) }
                items(done, key = { it.id }) { it -> LearnRow(it) { open = it.id } }
            }
        }
    }
    if (add) LearnEditSheet(vm, onDismiss = { add = false })
    open?.let { id -> d.items.firstOrNull { it.id == id }?.let { item -> LearnSheet(vm, item, onDismiss = { open = null }) } }
    if (settings) {
        var g by remember { mutableStateOf(d.dailyGoal.toString()) }
        FormSheet(stringResource(R.string.learn_goal), onDismiss = { settings = false }) {
            NumIn(g, { g = it }, stringResource(R.string.learn_goal), suffix = stringResource(R.string.min_day))
            PrimaryButton(stringResource(R.string.save), onClick = { g.toIntOrNull()?.let { v -> vm.learn.update { it.copy(dailyGoal = v.coerceIn(5, 600)) } }; settings = false },
                modifier = Modifier.fillMaxWidth(), color = Noc.ice)
        }
    }
}

@Composable
private fun LearnRow(it: LearnItem, onClick: () -> Unit) {
    AppCard(onClick = onClick, padding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(iconFor(LEARN_KINDS.firstOrNull { k -> k.first == it.kind }?.second ?: "school"), Noc.ice, 34)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(it.title, style = MaterialTheme.typography.bodyLarge)
                Text(kindName(it.kind) + if (it.total > 0) " · ${it.progress} / ${it.total} ${unitName(it.kind)}" else "", style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                if (it.total > 0) { VSpace(4); Meter(it.progress.toFloat() / it.total, if (it.done) Noc.good else Noc.ice, height = 4.dp) }
            }
        }
    }
}

@Composable
private fun LearnSheet(vm: ModsViewModel, item: LearnItem, onDismiss: () -> Unit) {
    var minutes by remember { mutableStateOf("20") }
    var units by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    fun save(fn: (LearnItem) -> LearnItem) = vm.learn.update { d -> d.copy(items = d.items.map { if (it.id == item.id) fn(it) else it }) }
    FormSheet(item.title, onDismiss) {
        Text(kindName(item.kind) + if (item.total > 0) " · ${item.progress} / ${item.total} ${unitName(item.kind)}" else "", style = MaterialTheme.typography.bodyMedium, color = Noc.text2)
        SectionLabel(stringResource(R.string.learn_session))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumIn(minutes, { minutes = it }, stringResource(R.string.duration), Modifier.weight(1f), suffix = stringResource(R.string.min_short))
            if (item.total > 0) NumIn(units, { units = it }, stringResource(R.string.learn_units_done, unitName(item.kind)), Modifier.weight(1f))
        }
        TextIn(note, { note = it }, stringResource(R.string.learn_takeaway), lines = 2, placeholder = stringResource(R.string.learn_takeaway_hint))
        PrimaryButton(stringResource(R.string.learn_log), onClick = {
            val m = minutes.toIntOrNull() ?: 0; val u = units.toIntOrNull() ?: 0
            vm.learn.update { d ->
                d.copy(
                    sessions = if (m > 0) d.sessions + LearnSession(item = item.id, minutes = m, units = u) else d.sessions,
                    items = d.items.map {
                        if (it.id != item.id) it else {
                            val p = (it.progress + u).let { v -> if (it.total > 0) v.coerceAtMost(it.total) else v }
                            it.copy(progress = p, notes = if (note.isNotBlank()) it.notes + note.trim() else it.notes, done = it.done || (it.total > 0 && p >= it.total))
                        }
                    },
                )
            }
            onDismiss()
        }, enabled = (minutes.toIntOrNull() ?: 0) > 0 || note.isNotBlank(), modifier = Modifier.fillMaxWidth(), color = Noc.ice)
        if (item.notes.isNotEmpty()) {
            SectionLabel(stringResource(R.string.learn_notes))
            item.notes.reversed().forEach { n -> Text("• $n", style = MaterialTheme.typography.bodyMedium) }
        }
        Row {
            TextButton(onClick = { save { it.copy(done = !item.done) }; onDismiss() }) { Text(stringResource(if (item.done) R.string.goals_reopen else R.string.goals_complete)) }
            TextButton(onClick = { vm.learn.update { d -> d.copy(items = d.items.filter { it.id != item.id }) }; onDismiss() }) { Text(stringResource(R.string.delete), color = Noc.coral) }
        }
    }
}

@Composable
private fun LearnEditSheet(vm: ModsViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("book") }
    var total by remember { mutableStateOf("") }
    FormSheet(stringResource(R.string.learn_add), onDismiss) {
        ChipRow(LEARN_KINDS.map { it.first }, kind, { kindName(it) }, Noc.ice) { kind = it }
        TextIn(title, { title = it }, stringResource(R.string.goals_title), placeholder = stringResource(R.string.learn_title_hint))
        NumIn(total, { total = it }, stringResource(R.string.learn_total_units, unitName(kind)))
        PrimaryButton(stringResource(R.string.save), onClick = {
            vm.learn.update { d -> d.copy(items = d.items + LearnItem(title = title.trim(), kind = kind, total = total.toIntOrNull() ?: 0)) }
            onDismiss()
        }, enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth(), color = Noc.ice)
    }
}

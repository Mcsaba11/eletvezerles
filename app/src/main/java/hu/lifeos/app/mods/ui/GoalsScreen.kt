package hu.lifeos.app.mods.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.mods.Goal
import hu.lifeos.app.mods.GoalStep
import hu.lifeos.app.mods.ModsViewModel
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.DatePickDialog
import hu.lifeos.app.ui.EmptyHint
import hu.lifeos.app.ui.Fmt
import hu.lifeos.app.ui.Meter
import hu.lifeos.app.ui.PrimaryButton
import hu.lifeos.app.ui.SectionLabel
import hu.lifeos.app.ui.TonalButton
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.rememberThump
import hu.lifeos.app.ui.screens.WHEEL_DIMS
import hu.lifeos.app.ui.theme.Noc
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun GoalsScreen(vm: ModsViewModel, onBack: () -> Unit) {
    val d by vm.goals.state.collectAsState()
    var add by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf<String?>(null) }
    var showDone by remember { mutableStateOf(false) }
    ModScaffold(stringResource(R.string.mod_goals), stringResource(R.string.goals_kicker), Noc.amber, onBack, onAdd = { add = true }) {
        val active = d.goals.filter { !it.done }
        val done = d.goals.filter { it.done }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = ModListPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text(stringResource(R.string.goals_sub), style = MaterialTheme.typography.bodyMedium, color = Noc.text2) }
            if (active.isEmpty()) item { EmptyHint(stringResource(R.string.goals_empty)) }
            items(active, key = { it.id }) { g -> GoalCard(g) { open = g.id } }
            if (done.isNotEmpty()) item {
                TextButton(onClick = { showDone = !showDone }) { Text(stringResource(R.string.goals_done_n, done.size)) }
            }
            if (showDone) items(done, key = { it.id }) { g -> GoalCard(g) { open = g.id } }
        }
    }
    if (add) GoalEditSheet(vm, null, onDismiss = { add = false })
    open?.let { id -> d.goals.firstOrNull { it.id == id }?.let { g -> GoalSheet(vm, g, onDismiss = { open = null }) } }
}

@Composable
private fun GoalCard(g: Goal, onClick: () -> Unit) {
    val area = stringResource(WHEEL_DIMS.getOrElse(g.area) { WHEEL_DIMS.first() })
    AppCard(onClick = onClick, glow = if (g.done) null else Noc.amber) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Outlined.Flag, if (g.done) Noc.good else Noc.amber, 34)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(g.title, style = MaterialTheme.typography.titleMedium, textDecoration = if (g.done) TextDecoration.LineThrough else null)
                val dl = g.deadline?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                Text(area + (dl?.let { " · " + stringResource(R.string.goals_days_left, ChronoUnit.DAYS.between(LocalDate.now(), it)) } ?: ""),
                    style = MaterialTheme.typography.bodySmall, color = Noc.text2)
            }
            Text("${(g.progress() * 100).toInt()}%", style = MaterialTheme.typography.titleSmall, color = Noc.amber)
        }
        VSpace(8)
        Meter(g.progress(), if (g.done) Noc.good else Noc.amber, height = 5.dp)
        g.steps.firstOrNull { !it.done }?.let { s ->
            VSpace(6)
            Text(stringResource(R.string.goals_next, s.text), style = MaterialTheme.typography.bodySmall, color = Noc.text)
        }
    }
}

@Composable
private fun GoalSheet(vm: ModsViewModel, g: Goal, onDismiss: () -> Unit) {
    var newStep by remember { mutableStateOf("") }
    var edit by remember { mutableStateOf(false) }
    val thump = rememberThump()
    if (edit) { GoalEditSheet(vm, g, onDismiss = { edit = false; onDismiss() }); return }
    fun save(fn: (Goal) -> Goal) = vm.goals.update { d -> d.copy(goals = d.goals.map { if (it.id == g.id) fn(it) else it }) }
    FormSheet(g.title, onDismiss) {
        if (g.why.isNotBlank()) Text(stringResource(R.string.goals_why_is, g.why), style = MaterialTheme.typography.bodyMedium, color = Noc.text2)
        Meter(g.progress(), Noc.amber, height = 6.dp)
        SectionLabel(stringResource(R.string.goals_steps))
        g.steps.forEach { s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(s.done, { on ->
                    thump()
                    save { gg -> gg.copy(steps = gg.steps.map { if (it.id == s.id) it.copy(done = on) else it }) }
                }, colors = CheckboxDefaults.colors(checkedColor = Noc.amber, checkmarkColor = Noc.abyss, uncheckedColor = Noc.text3))
                Text(s.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textDecoration = if (s.done) TextDecoration.LineThrough else null)
                IconButton(onClick = { save { gg -> gg.copy(steps = gg.steps.filter { it.id != s.id }) } }) { Icon(Icons.Outlined.Close, stringResource(R.string.delete), tint = Noc.text3) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextIn(newStep, { newStep = it }, stringResource(R.string.goals_new_step), Modifier.weight(1f))
            IconButton(onClick = {
                if (newStep.isNotBlank()) { save { gg -> gg.copy(steps = gg.steps + GoalStep(text = newStep.trim())) }; newStep = "" }
            }) { Icon(Icons.Outlined.Add, stringResource(R.string.add), tint = Noc.amber) }
        }
        PrimaryButton(
            stringResource(if (g.done) R.string.goals_reopen else R.string.goals_complete),
            onClick = { thump(); save { it.copy(done = !g.done, doneAt = if (g.done) null else LocalDate.now().toString()) }; onDismiss() },
            modifier = Modifier.fillMaxWidth(), color = if (g.done) Noc.amber else Noc.good,
        )
        Row {
            TextButton(onClick = { edit = true }) { Text(stringResource(R.string.edit)) }
            TextButton(onClick = { vm.goals.update { d -> d.copy(goals = d.goals.filter { it.id != g.id }) }; onDismiss() }) { Text(stringResource(R.string.delete), color = Noc.coral) }
        }
    }
}

@Composable
private fun GoalEditSheet(vm: ModsViewModel, g: Goal?, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(g?.title ?: "") }
    var area by remember { mutableStateOf(g?.area ?: 1) }
    var why by remember { mutableStateOf(g?.why ?: "") }
    var steps by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf(g?.deadline?.let { runCatching { LocalDate.parse(it) }.getOrNull() }) }
    var pick by remember { mutableStateOf(false) }
    FormSheet(stringResource(if (g == null) R.string.goals_add else R.string.edit), onDismiss) {
        TextIn(title, { title = it }, stringResource(R.string.goals_title), placeholder = stringResource(R.string.goals_title_hint))
        Text(stringResource(R.string.goals_area), style = MaterialTheme.typography.labelLarge, color = Noc.text2)
        ChipRow(WHEEL_DIMS.indices.toList(), area, { stringResource(WHEEL_DIMS[it]) }, Noc.amber) { area = it }
        TextIn(why, { why = it }, stringResource(R.string.goals_why), lines = 2, placeholder = stringResource(R.string.goals_why_hint))
        if (g == null) TextIn(steps, { steps = it }, stringResource(R.string.goals_steps_init), lines = 3, placeholder = stringResource(R.string.goals_steps_hint))
        TonalButton(deadline?.let { stringResource(R.string.money_deadline, Fmt.dayLong(it)) } ?: stringResource(R.string.money_deadline_set), onClick = { pick = true })
        PrimaryButton(stringResource(R.string.save), onClick = {
            val init = steps.lines().map { it.trim() }.filter { it.isNotBlank() }.map { GoalStep(text = it) }
            val item = (g ?: Goal(title = "")).let { it.copy(title = title.trim(), area = area, why = why.trim(), deadline = deadline?.toString(), steps = it.steps + init) }
            vm.goals.update { d -> d.copy(goals = if (g == null) d.goals + item else d.goals.map { if (it.id == g.id) item else it }) }
            onDismiss()
        }, enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth(), color = Noc.amber)
    }
    if (pick) DatePickDialog(deadline ?: LocalDate.now().plusMonths(1), { pick = false }) { deadline = it }
}


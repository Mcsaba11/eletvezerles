package hu.lifeos.app.mods.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.mods.ModsViewModel
import hu.lifeos.app.mods.Tracker
import hu.lifeos.app.mods.TrackersDoc
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.EmptyHint
import hu.lifeos.app.ui.PrimaryButton
import hu.lifeos.app.ui.TRACKER_ICONS
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.iconFor
import hu.lifeos.app.ui.rememberThump
import hu.lifeos.app.ui.theme.Noc
import java.time.LocalDate

@Composable
fun trackerColor(i: Int): Color = when (i % 6) { 0 -> Noc.cyan; 1 -> Noc.amber; 2 -> Noc.good; 3 -> Noc.violet; 4 -> Noc.coral; else -> Noc.sapphire }

private val KINDS = listOf("check", "count", "number", "scale")

@Composable
private fun kindLabel(k: String) = stringResource(when (k) { "check" -> R.string.tk_check; "count" -> R.string.tk_count; "number" -> R.string.tk_number; else -> R.string.tk_scale })

fun fmtVal(v: Double): String = if (v % 1 == 0.0) v.toLong().toString() else String.format(java.util.Locale.getDefault(), "%.1f", v)

@Composable
fun TrackersScreen(vm: ModsViewModel, onBack: () -> Unit) {
    val d by vm.trackers.state.collectAsState()
    var add by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf<Tracker?>(null) }
    ModScaffold(stringResource(R.string.mod_trackers), stringResource(R.string.trackers_kicker), Noc.cyan, onBack, onAdd = { add = true }) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = ModListPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text(stringResource(R.string.trackers_sub), style = MaterialTheme.typography.bodyMedium, color = Noc.text2) }
            if (d.trackers.isEmpty()) item {
                EmptyHint(stringResource(R.string.trackers_empty))
                TrackerTemplates(vm)
            }
            items(d.trackers, key = { it.id }) { t -> TrackerCard(vm, d, t, onEdit = { edit = t }) }
        }
    }
    if (add || edit != null) TrackerSheet(vm, edit, onDismiss = { add = false; edit = null })
}

/** Gyors sablonok az első követőkhöz. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrackerTemplates(vm: ModsViewModel) {
    val water = stringResource(R.string.tpl_water); val read = stringResource(R.string.tpl_read); val vit = stringResource(R.string.tpl_vitamin)
    val med = stringResource(R.string.tpl_meditate); val glass = stringResource(R.string.unit_glass); val pages = stringResource(R.string.unit_pages)
    val min = stringResource(R.string.min_short)
    val tpl = listOf(
        Tracker(name = water, icon = "water", kind = "count", unit = glass, goal = 8.0, color = 0),
        Tracker(name = read, icon = "book", kind = "number", unit = pages, goal = 20.0, color = 1),
        Tracker(name = vit, icon = "medication", kind = "check", goal = 1.0, color = 2),
        Tracker(name = med, icon = "self_improvement", kind = "number", unit = min, goal = 10.0, color = 3),
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tpl.forEach { t ->
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Noc.card).border(1.dp, Noc.stroke, RoundedCornerShape(50))
                    .clickable { vm.trackers.update { it.copy(trackers = it.trackers + t.copy(id = hu.lifeos.app.mods.newId())) } }
                    .padding8(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(iconFor(t.icon), null, Modifier.size(16.dp), tint = trackerColor(t.color))
                Spacer(Modifier.width(6.dp))
                Text("+ " + t.name, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

private fun Modifier.padding8() = this.then(Modifier.padding(horizontal = 12.dp, vertical = 8.dp))

/** Egy követő kártyája a mai értékkel és gyors beviteli elemekkel (a kezdőlapon is használható). */
@Composable
fun TrackerCard(vm: ModsViewModel, d: TrackersDoc, t: Tracker, onEdit: (() -> Unit)? = null) {
    val col = trackerColor(t.color)
    val today = LocalDate.now().toString()
    val v = d.value(t.id, today)
    val thump = rememberThump()
    fun set(x: Double) { thump(); vm.trackers.update { it.set(t.id, today, x.coerceAtLeast(0.0)) } }
    var numOpen by remember { mutableStateOf(false) }
    AppCard(onClick = onEdit, glow = if (t.kind != "scale" && v >= t.goal) col else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(iconFor(t.icon), col, 36)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(t.name, style = MaterialTheme.typography.titleSmall)
                val streak = d.streak(t)
                Text(
                    (if (t.kind == "count" || t.kind == "number") "${fmtVal(v)} / ${fmtVal(t.goal)} ${t.unit}".trim() + " · " else "") +
                        stringResource(R.string.trackers_streak, streak),
                    style = MaterialTheme.typography.bodySmall, color = Noc.text2,
                )
            }
            when (t.kind) {
                "check" -> IconButton(onClick = { set(if (v >= 1) 0.0 else 1.0) }) {
                    Icon(if (v >= 1) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null, Modifier.size(30.dp), tint = if (v >= 1) col else Noc.text3)
                }
                "count" -> Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { set(v - 1) }, enabled = v > 0) { Icon(Icons.Outlined.Remove, null, tint = Noc.text2) }
                    Text(fmtVal(v), style = MaterialTheme.typography.titleMedium, color = col)
                    IconButton(onClick = { set(v + 1) }) { Icon(Icons.Outlined.Add, null, tint = col) }
                }
                "number" -> TextButton(onClick = { numOpen = true }) { Text(if (v > 0) fmtVal(v) + " " + t.unit else stringResource(R.string.trackers_enter), color = col) }
                else -> {}
            }
        }
        if (t.kind == "scale") {
            VSpace(8)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..5).forEach { n ->
                    val on = v.toInt() == n
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (on) col.copy(alpha = 0.2f) else Noc.well)
                            .border(1.dp, if (on) col else Noc.stroke, RoundedCornerShape(12.dp)).clickable { set(if (on) 0.0 else n.toDouble()) },
                        contentAlignment = Alignment.Center,
                    ) { Text("$n", style = MaterialTheme.typography.titleSmall, color = if (on) col else Noc.text2, modifier = Modifier.padding8()) }
                }
            }
        }
        VSpace(8)
        // Az elmúlt 14 nap pöttyei
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (13 downTo 0).forEach { i ->
                val day = LocalDate.now().minusDays(i.toLong()).toString()
                val x = d.value(t.id, day)
                val hit = if (t.kind == "scale") x > 0 else x >= t.goal
                val part = !hit && x > 0
                Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(if (hit) col else if (part) col.copy(alpha = 0.35f) else Noc.well))
            }
        }
    }
    if (numOpen) {
        var s by remember { mutableStateOf(if (v > 0) fmtVal(v) else "") }
        FormSheet(t.name, onDismiss = { numOpen = false }) {
            NumIn(s, { s = it }, stringResource(R.string.trackers_today_value), suffix = t.unit.ifBlank { null }, decimal = true)
            PrimaryButton(stringResource(R.string.save), onClick = { set(s.toNum() ?: 0.0); numOpen = false }, modifier = Modifier.fillMaxWidth(), color = col)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrackerSheet(vm: ModsViewModel, t: Tracker?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(t?.name ?: "") }
    var icon by remember { mutableStateOf(t?.icon ?: "star") }
    var kind by remember { mutableStateOf(t?.kind ?: "check") }
    var unit by remember { mutableStateOf(t?.unit ?: "") }
    var goal by remember { mutableStateOf(t?.goal?.let { fmtVal(it) } ?: "1") }
    var color by remember { mutableStateOf(t?.color ?: 0) }
    var onHome by remember { mutableStateOf(t?.onHome ?: true) }
    FormSheet(stringResource(if (t == null) R.string.trackers_new else R.string.edit), onDismiss) {
        TextIn(name, { name = it }, stringResource(R.string.habit_name), placeholder = stringResource(R.string.trackers_name_hint))
        Text(stringResource(R.string.trackers_kind), style = MaterialTheme.typography.labelLarge, color = Noc.text2)
        ChipRow(KINDS, kind, { kindLabel(it) }, Noc.cyan) { kind = it; if (it == "check" || it == "scale") goal = "1" }
        if (kind == "count" || kind == "number") Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumIn(goal, { goal = it }, stringResource(R.string.trackers_goal), Modifier.weight(1f), decimal = true)
            TextIn(unit, { unit = it }, stringResource(R.string.trackers_unit), Modifier.weight(1f), placeholder = stringResource(R.string.trackers_unit_hint))
        }
        Text(stringResource(R.string.habit_icon), style = MaterialTheme.typography.labelLarge, color = Noc.text2)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TRACKER_ICONS.forEach { ic ->
                val on = ic == icon
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(if (on) trackerColor(color).copy(alpha = 0.18f) else Noc.well)
                        .border(1.dp, if (on) trackerColor(color) else Noc.stroke, RoundedCornerShape(12.dp)).clickable { icon = ic },
                    contentAlignment = Alignment.Center,
                ) { Icon(iconFor(ic), null, tint = if (on) trackerColor(color) else Noc.text2) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            (0..5).forEach { i ->
                Box(Modifier.size(30.dp).clip(CircleShape).background(trackerColor(i)).border(3.dp, if (i == color) Noc.text else Color.Transparent, CircleShape).clickable { color = i })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.trackers_on_home), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(onHome, { onHome = it })
        }
        PrimaryButton(stringResource(R.string.save), onClick = {
            val item = (t ?: Tracker(name = "")).copy(name = name.trim(), icon = icon, kind = kind, unit = unit.trim(), goal = (goal.toNum() ?: 1.0).coerceAtLeast(0.1), color = color, onHome = onHome)
            vm.trackers.update { d -> d.copy(trackers = if (t == null) d.trackers + item else d.trackers.map { if (it.id == t.id) item else it }) }
            onDismiss()
        }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth(), color = Noc.cyan)
        if (t != null) TextButton(onClick = { vm.trackers.update { d -> d.copy(trackers = d.trackers.filter { it.id != t.id }, logs = d.logs - t.id) }; onDismiss() }) {
            Text(stringResource(R.string.delete), color = Noc.coral)
        }
    }
}

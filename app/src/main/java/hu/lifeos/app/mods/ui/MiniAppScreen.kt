package hu.lifeos.app.mods.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.mods.Block
import hu.lifeos.app.mods.BlockData
import hu.lifeos.app.mods.BlockTypes
import hu.lifeos.app.mods.JEntry
import hu.lifeos.app.mods.MiniApp
import hu.lifeos.app.mods.MiniAppsDoc
import hu.lifeos.app.mods.MiniLogic
import hu.lifeos.app.mods.ModsViewModel
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.EmptyHint
import hu.lifeos.app.ui.Fmt
import hu.lifeos.app.ui.Meter
import hu.lifeos.app.ui.MetricRing
import hu.lifeos.app.ui.PrimaryButton
import hu.lifeos.app.ui.TonalButton
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.iconFor
import hu.lifeos.app.ui.rememberThump
import hu.lifeos.app.ui.theme.Noc
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime

/** Egy saját mini-app futtatása: a kockák egymás alatt, élő adatokkal. */
@Composable
fun MiniAppScreen(vm: ModsViewModel, appId: String, onBack: () -> Unit, onEdit: () -> Unit, onBreath: () -> Unit) {
    val doc by vm.miniapps.state.collectAsState()
    val app = doc.app(appId)
    if (app == null) { LaunchedEffect(Unit) { onBack() }; return }
    val col = trackerColor(app.color)
    var sos by remember { mutableStateOf(false) }
    val sosBlock = app.blocks.firstOrNull { it.type == BlockTypes.SOS }
    ModScaffold(
        app.name, stringResource(R.string.mini_kicker), col, onBack,
        headerExtra = {
            if (sosBlock != null) TextButton(onClick = { sos = true }) { Text(stringResource(R.string.sos), color = Noc.sos) }
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, stringResource(R.string.edit), tint = Noc.text2) }
        },
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = ModListPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (app.blocks.isEmpty()) item { EmptyHint(stringResource(R.string.mini_no_blocks)) }
            items(app.blocks, key = { it.id }) { b -> BlockView(vm, doc, app, b, col, onBreath) }
        }
    }
    if (sos && sosBlock != null) SosSheet(sosBlock, col, onBreath = { sos = false; onBreath() }, onDismiss = { sos = false })
}

@Composable
fun BlockView(vm: ModsViewModel, doc: MiniAppsDoc, app: MiniApp, b: Block, col: Color, onBreath: () -> Unit) {
    val d = doc.data(b.id)
    fun save(fn: (BlockData) -> BlockData) = vm.miniapps.update { it.withData(b.id, fn) }
    when (b.type) {
        BlockTypes.TEXT -> AppCard {
            if (b.title.isNotBlank()) Text(b.title, style = MaterialTheme.typography.titleMedium)
            Text(b.cfg.text, style = MaterialTheme.typography.bodyLarge, color = Noc.text2, fontStyle = FontStyle.Italic)
        }
        BlockTypes.STREAK -> StreakBlock(b, d, col, ::save)
        BlockTypes.TRACKER -> TrackerBlock(b, d, col, ::save)
        BlockTypes.CHECKLIST -> ChecklistBlock(b, d, col, ::save)
        BlockTypes.JOURNAL -> JournalBlock(b, d, col, ::save)
        BlockTypes.SAVINGS -> {
            val days = MiniLogic.savingsDays(app, doc)
            AppCard(glow = Noc.good) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Outlined.Savings, Noc.good)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(b.title.ifBlank { stringResource(R.string.bt_savings) }, style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.mini_savings_sub, Fmt.money(b.cfg.perDay), days), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                    }
                    Text(Fmt.money(b.cfg.perDay * days), style = MaterialTheme.typography.titleLarge, color = Noc.good)
                }
            }
        }
        BlockTypes.SOS -> AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Outlined.Spa, Noc.sos)
                Spacer(Modifier.width(12.dp))
                Text(b.title.ifBlank { stringResource(R.string.bt_sos) }, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            }
            VSpace(6)
            b.cfg.items.take(3).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = Noc.text2) }
            VSpace(8)
            TonalButton(stringResource(R.string.sc_breath), onClick = onBreath, tint = Noc.amber)
        }
        BlockTypes.TIMER -> TimerBlock(b, d, col, ::save)
        BlockTypes.CHART -> {
            val src = app.blocks.firstOrNull { it.id == b.cfg.source }
            val days = (b.cfg.days.coerceIn(7, 31) - 1 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
            AppCard {
                Text(b.title.ifBlank { stringResource(R.string.bt_chart) }, style = MaterialTheme.typography.titleMedium)
                VSpace(10)
                if (src == null) Text(stringResource(R.string.mini_chart_nosrc), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                else MiniBars(
                    days.map { MiniLogic.seriesValue(src, doc.data(src.id), it.toString()) },
                    days.mapIndexed { i, dd -> if (days.size <= 7 || i % 2 == 0) dd.dayOfMonth.toString() else "" }, col,
                    goal = if (src.type == BlockTypes.TRACKER && src.cfg.kind != "scale") src.cfg.goal else null,
                )
            }
        }
        BlockTypes.REMINDER -> AppCard(padding = PaddingValues(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Outlined.Notifications, col, 34)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(b.title.ifBlank { stringResource(R.string.bt_reminder) } + " · " + b.cfg.time, style = MaterialTheme.typography.bodyLarge)
                    if (b.cfg.text.isNotBlank()) Text(b.cfg.text, style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun StreakBlock(b: Block, d: BlockData, col: Color, save: ((BlockData) -> BlockData) -> Unit) {
    val days = MiniLogic.streakDays(d)
    val lvl = MiniLogic.level(b.cfg, days)
    val next = MiniLogic.nextMilestone(b.cfg, days)
    var ask by remember { mutableStateOf(false) }
    val thump = rememberThump()
    AppCard(glow = col) {
        Text(b.title.ifBlank { stringResource(R.string.bt_streak) }, style = MaterialTheme.typography.labelLarge, color = Noc.text2)
        if (d.start == null) {
            VSpace(6)
            Text(stringResource(R.string.mini_streak_start_q), style = MaterialTheme.typography.bodyMedium)
            VSpace(8)
            PrimaryButton(stringResource(R.string.mini_streak_start), onClick = { thump(); save { it.copy(start = LocalDate.now().toString()) } }, color = col)
            return@AppCard
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$days", style = MaterialTheme.typography.displaySmall, color = Noc.text)
                    Text(" " + stringResource(R.string.satori_day_word), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
                }
                val name = if (lvl >= 0) b.cfg.levelNames.getOrNull(lvl) ?: stringResource(R.string.mini_level_n, lvl + 1) else stringResource(R.string.mini_level_0)
                Text(name, style = MaterialTheme.typography.titleSmall, color = col)
                Text(stringResource(R.string.mini_best, maxOf(d.best, days)), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
            }
            val prev = if (lvl >= 0) b.cfg.milestones[lvl] else 0
            val frac = if (next != null) (days - prev).toFloat() / (next - prev).coerceAtLeast(1) else 1f
            MetricRing(frac, col, Modifier.size(84.dp)) {
                Text(next?.let { stringResource(R.string.mini_next_short, it) } ?: "★", style = MaterialTheme.typography.labelMedium)
            }
        }
        VSpace(8)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            b.cfg.milestones.forEachIndexed { i, _ ->
                Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(if (i <= lvl) col else Noc.well))
            }
        }
        VSpace(8)
        TextButton(onClick = { ask = true }) {
            Icon(Icons.Outlined.Replay, null, Modifier.size(16.dp), tint = Noc.text3)
            Text("  " + stringResource(R.string.mini_reset), color = Noc.text3)
        }
    }
    if (ask) AlertDialog(
        onDismissRequest = { ask = false },
        title = { Text(stringResource(R.string.mini_reset)) },
        text = { Text(stringResource(R.string.mini_reset_text)) },
        confirmButton = { TextButton(onClick = { save { MiniLogic.reset(it) }; ask = false }) { Text(stringResource(R.string.mini_reset_yes)) } },
        dismissButton = { TextButton(onClick = { ask = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun TrackerBlock(b: Block, d: BlockData, col: Color, save: ((BlockData) -> BlockData) -> Unit) {
    val today = LocalDate.now().toString()
    val v = MiniLogic.value(d, today)
    val thump = rememberThump()
    fun set(x: Double) { thump(); save { MiniLogic.setValue(it, today, x.coerceAtLeast(0.0)) } }
    var numOpen by remember { mutableStateOf(false) }
    val streak = MiniLogic.runStreak { MiniLogic.hit(b.cfg, MiniLogic.value(d, it)) }
    AppCard(glow = if (MiniLogic.hit(b.cfg, v)) col else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(b.title.ifBlank { stringResource(R.string.bt_tracker) }, style = MaterialTheme.typography.titleSmall)
                Text(
                    (if (b.cfg.kind == "count" || b.cfg.kind == "number") "${fmtVal(v)} / ${fmtVal(b.cfg.goal)} ${b.cfg.unit}".trim() + " · " else "") +
                        stringResource(R.string.trackers_streak, streak),
                    style = MaterialTheme.typography.bodySmall, color = Noc.text2,
                )
            }
            when (b.cfg.kind) {
                "check" -> IconButton(onClick = { set(if (v >= 1) 0.0 else 1.0) }) {
                    Icon(if (v >= 1) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null, Modifier.size(30.dp), tint = if (v >= 1) col else Noc.text3)
                }
                "count" -> Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { set(v - 1) }, enabled = v > 0) { Icon(Icons.Outlined.Remove, null, tint = Noc.text2) }
                    Text(fmtVal(v), style = MaterialTheme.typography.titleMedium, color = col)
                    IconButton(onClick = { set(v + 1) }) { Icon(Icons.Outlined.Add, null, tint = col) }
                }
                "number" -> TextButton(onClick = { numOpen = true }) { Text(if (v > 0) fmtVal(v) + " " + b.cfg.unit else stringResource(R.string.trackers_enter), color = col) }
                else -> {}
            }
        }
        if (b.cfg.kind == "scale") {
            VSpace(8)
            ChipRow((1..5).toList(), v.toInt().takeIf { it > 0 }, { "$it" }, col) { n -> set(if (v.toInt() == n) 0.0 else n.toDouble()) }
        }
        if (b.cfg.kind == "count" || b.cfg.kind == "number") { VSpace(8); Meter((v / b.cfg.goal).toFloat(), col, height = 5.dp) }
    }
    if (numOpen) {
        var s by remember { mutableStateOf(if (v > 0) fmtVal(v) else "") }
        FormSheet(b.title, onDismiss = { numOpen = false }) {
            NumIn(s, { s = it }, stringResource(R.string.trackers_today_value), suffix = b.cfg.unit.ifBlank { null }, decimal = true)
            PrimaryButton(stringResource(R.string.save), onClick = { set(s.toNum() ?: 0.0); numOpen = false }, modifier = Modifier.fillMaxWidth(), color = col)
        }
    }
}

@Composable
private fun ChecklistBlock(b: Block, d: BlockData, col: Color, save: ((BlockData) -> BlockData) -> Unit) {
    val today = LocalDate.now().toString()
    val done = d.checks[today].orEmpty()
    val thump = rememberThump()
    val n = b.cfg.items.size
    val streak = MiniLogic.runStreak { day -> n > 0 && (d.checks[day]?.size ?: 0) >= n }
    AppCard(glow = if (n > 0 && done.size >= n) col else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(b.title.ifBlank { stringResource(R.string.bt_checklist) }, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text("${done.size}/$n", style = MaterialTheme.typography.labelLarge, color = col)
        }
        Text(stringResource(R.string.mini_full_days, streak), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
        b.cfg.items.forEachIndexed { i, it ->
            val on = i in done
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(on, { c ->
                    thump()
                    save { bd -> bd.copy(checks = bd.checks + (today to (if (c) (bd.checks[today].orEmpty() + i).distinct() else bd.checks[today].orEmpty() - i))) }
                }, colors = CheckboxDefaults.colors(checkedColor = col, checkmarkColor = Noc.abyss, uncheckedColor = Noc.text3))
                Text(it, style = MaterialTheme.typography.bodyLarge, color = if (on) Noc.text2 else Noc.text, textDecoration = if (on) TextDecoration.LineThrough else null)
            }
        }
    }
}

@Composable
private fun JournalBlock(b: Block, d: BlockData, col: Color, save: ((BlockData) -> BlockData) -> Unit) {
    var add by remember { mutableStateOf(false) }
    var all by remember { mutableStateOf(false) }
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(b.title.ifBlank { stringResource(R.string.bt_journal) }, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = { add = true }) { Text(stringResource(R.string.mini_journal_add), color = col) }
        }
        val list = d.entries.sortedWith(compareByDescending<JEntry> { it.date }.thenByDescending { it.time })
        if (list.isEmpty()) Text(stringResource(R.string.mini_journal_empty), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
        (if (all) list else list.take(3)).forEach { e ->
            Column(Modifier.padding(vertical = 5.dp)) {
                Text(
                    runCatching { Fmt.dayShort(LocalDate.parse(e.date)) }.getOrDefault(e.date) + " " + e.time +
                        (e.tag?.let { " · $it" } ?: "") + (e.mood?.let { " · $it/5" } ?: ""),
                    style = MaterialTheme.typography.labelMedium, color = col,
                )
                if (e.text.isNotBlank()) Text(e.text, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (list.size > 3) TextButton(onClick = { all = !all }) { Text(stringResource(if (all) R.string.mini_less else R.string.mini_all, list.size)) }
    }
    if (add) {
        var text by remember { mutableStateOf("") }
        var mood by remember { mutableStateOf<Int?>(null) }
        var tag by remember { mutableStateOf<String?>(null) }
        FormSheet(b.title.ifBlank { stringResource(R.string.bt_journal) }, onDismiss = { add = false }) {
            if (b.cfg.items.isNotEmpty()) ChipRow(b.cfg.items, tag, { it }, col) { tag = if (tag == it) null else it }
            TextIn(text, { text = it }, stringResource(R.string.mini_journal_text), lines = 3)
            Text(stringResource(R.string.mood_title), style = MaterialTheme.typography.labelLarge, color = Noc.text2)
            ChipRow((1..5).toList(), mood, { "$it" }, col) { mood = if (mood == it) null else it }
            PrimaryButton(stringResource(R.string.save), onClick = {
                save { it.copy(entries = it.entries + JEntry(time = LocalTime.now().format(Fmt.time), text = text.trim(), mood = mood, tag = tag)) }
                add = false
            }, enabled = text.isNotBlank() || mood != null || tag != null, modifier = Modifier.fillMaxWidth(), color = col)
        }
    }
}

@Composable
private fun TimerBlock(b: Block, d: BlockData, col: Color, save: ((BlockData) -> BlockData) -> Unit) {
    val total = b.cfg.minutes.coerceAtLeast(1) * 60
    var left by remember { mutableIntStateOf(total) }
    var running by remember { mutableStateOf(false) }
    val thump = rememberThump()
    LaunchedEffect(running) {
        while (running && left > 0) { delay(1000); left-- }
        if (running && left <= 0) {
            running = false; thump()
            val today = LocalDate.now().toString()
            save { it.copy(sessions = it.sessions + (today to ((it.sessions[today] ?: 0) + b.cfg.minutes))) }
            left = total
        }
    }
    val todayMin = d.sessions[LocalDate.now().toString()] ?: 0
    AppCard(glow = if (running) col else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(b.title.ifBlank { stringResource(R.string.bt_timer) }, style = MaterialTheme.typography.titleSmall)
                Text("%d:%02d".format(left / 60, left % 60), style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"))
                Text(stringResource(R.string.mini_timer_today, todayMin), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
            }
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(col.copy(alpha = 0.16f)).border(1.dp, col, CircleShape).clickable { thump(); running = !running },
                contentAlignment = Alignment.Center,
            ) { Icon(if (running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, null, tint = col, modifier = Modifier.size(30.dp)) }
        }
        if (left < total) {
            VSpace(8)
            Meter(1f - left.toFloat() / total, col, height = 5.dp)
            TextButton(onClick = { running = false; left = total }) { Text(stringResource(R.string.mini_timer_reset), color = Noc.text3) }
        }
    }
}

@Composable
private fun SosSheet(b: Block, col: Color, onBreath: () -> Unit, onDismiss: () -> Unit) {
    FormSheet(b.title.ifBlank { stringResource(R.string.bt_sos) }, onDismiss) {
        b.cfg.items.forEach { tip ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Noc.card).border(1.dp, Noc.stroke, RoundedCornerShape(16.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(iconFor("spa"), null, tint = col)
                Spacer(Modifier.width(10.dp))
                Text(tip, style = MaterialTheme.typography.bodyLarge)
            }
        }
        PrimaryButton(stringResource(R.string.sc_breath), onClick = onBreath, modifier = Modifier.fillMaxWidth(), color = Noc.amber)
    }
}

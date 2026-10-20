package hu.lifeos.app.mods.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.mods.Block
import hu.lifeos.app.mods.BlockCfg
import hu.lifeos.app.mods.BlockTypes
import hu.lifeos.app.mods.MiniApp
import hu.lifeos.app.mods.MiniLogic
import hu.lifeos.app.mods.MiniShare
import hu.lifeos.app.mods.MiniTemplates
import hu.lifeos.app.mods.ModsViewModel
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.CUSTOM_HABIT_ICONS
import hu.lifeos.app.ui.EmptyHint
import hu.lifeos.app.ui.Fmt
import hu.lifeos.app.ui.PrimaryButton
import hu.lifeos.app.ui.SectionLabel
import hu.lifeos.app.ui.TRACKER_ICONS
import hu.lifeos.app.ui.TimePickDialog
import hu.lifeos.app.ui.TonalButton
import hu.lifeos.app.ui.iconFor
import hu.lifeos.app.ui.theme.Noc
import kotlinx.coroutines.launch
import java.time.LocalTime

val MINI_ICONS = (TRACKER_ICONS + CUSTOM_HABIT_ICONS + listOf("shield", "fitness_center", "self_improvement", "spa", "school", "home")).distinct()

@Composable
fun blockName(t: String) = stringResource(
    when (t) {
        BlockTypes.STREAK -> R.string.bt_streak; BlockTypes.TRACKER -> R.string.bt_tracker; BlockTypes.CHECKLIST -> R.string.bt_checklist
        BlockTypes.JOURNAL -> R.string.bt_journal; BlockTypes.SAVINGS -> R.string.bt_savings; BlockTypes.SOS -> R.string.bt_sos
        BlockTypes.TIMER -> R.string.bt_timer; BlockTypes.CHART -> R.string.bt_chart; BlockTypes.TEXT -> R.string.bt_text
        else -> R.string.bt_reminder
    }
)

@Composable
fun blockDesc(t: String) = stringResource(
    when (t) {
        BlockTypes.STREAK -> R.string.bd_streak; BlockTypes.TRACKER -> R.string.bd_tracker; BlockTypes.CHECKLIST -> R.string.bd_checklist
        BlockTypes.JOURNAL -> R.string.bd_journal; BlockTypes.SAVINGS -> R.string.bd_savings; BlockTypes.SOS -> R.string.bd_sos
        BlockTypes.TIMER -> R.string.bd_timer; BlockTypes.CHART -> R.string.bd_chart; BlockTypes.TEXT -> R.string.bd_text
        else -> R.string.bd_reminder
    }
)

fun blockIcon(t: String): String = when (t) {
    BlockTypes.STREAK -> "auto_awesome"; BlockTypes.TRACKER -> "check_circle"; BlockTypes.CHECKLIST -> "checklist"
    BlockTypes.JOURNAL -> "edit_note"; BlockTypes.SAVINGS -> "savings"; BlockTypes.SOS -> "spa"; BlockTypes.TIMER -> "timer"
    BlockTypes.CHART -> "trending_down"; BlockTypes.TEXT -> "psychology"; else -> "bolt"
}

// ======================= Modulépítő: saját appok listája, sablonok, importálás =======================

@Composable
fun BuilderScreen(vm: ModsViewModel, onBack: () -> Unit, onOpen: (String) -> Unit, onEdit: (String) -> Unit, onTrackers: () -> Unit) {
    val doc by vm.miniapps.state.collectAsState()
    val ctx = LocalContext.current
    val templates = remember { MiniTemplates.load(ctx) }
    var importOpen by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    ModScaffold(
        stringResource(R.string.builder_title), stringResource(R.string.trackers_kicker), Noc.cyan, onBack,
        onAdd = {
            val a = MiniApp(name = ctx.getString(R.string.builder_new_name))
            vm.miniapps.update { it.copy(apps = it.apps + a) }
            onEdit(a.id)
        },
        snackbar = snackbar,
        headerExtra = { IconButton(onClick = { importOpen = true }) { Icon(Icons.Outlined.ContentPaste, stringResource(R.string.builder_import), tint = Noc.text2) } },
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = ModListPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text(stringResource(R.string.builder_sub), style = MaterialTheme.typography.bodyMedium, color = Noc.text2) }
            if (doc.apps.isNotEmpty()) item { SectionLabel(stringResource(R.string.builder_my_apps)) }
            items(doc.apps, key = { it.id }) { a ->
                val col = trackerColor(a.color)
                AppCard(onClick = { onOpen(a.id) }, padding = PaddingValues(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(iconFor(a.icon), col, 40)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.name, style = MaterialTheme.typography.titleSmall)
                            Text(a.blocks.map { b -> b.type }.distinct().take(4).let { ts -> ts.map { blockName(it) }.joinToString(" · ") }.ifBlank { stringResource(R.string.mini_no_blocks_short) },
                                style = MaterialTheme.typography.bodySmall, color = Noc.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        TextButton(onClick = { onEdit(a.id) }) { Text(stringResource(R.string.edit), color = col) }
                    }
                }
            }
            item {
                AppCard(onClick = onTrackers, padding = PaddingValues(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Outlined.Widgets, Noc.cyan, 40)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.trackers_simple), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.builder_trackers_sub), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                        }
                    }
                }
            }
            item { SectionLabel(stringResource(R.string.builder_templates)) }
            items(templates, key = { "t" + it.app.id }) { t ->
                val col = trackerColor(t.app.color)
                AppCard(padding = PaddingValues(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(iconFor(t.app.icon), col, 40)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.app.name, style = MaterialTheme.typography.titleSmall)
                            Text(t.desc, style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                        }
                    }
                    Row {
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            val a = MiniLogic.instantiate(t.app)
                            vm.miniapps.update { it.copy(apps = it.apps + a) }
                            onOpen(a.id)
                        }) { Text(stringResource(R.string.builder_use), color = col) }
                    }
                }
            }
        }
    }
    if (importOpen) {
        val clip = LocalClipboardManager.current
        var text by remember { mutableStateOf(clip.getText()?.text.orEmpty().takeIf { it.contains("meder_miniapp") || it.contains("\"blocks\"") } ?: "") }
        val bad = stringResource(R.string.builder_import_bad)
        FormSheet(stringResource(R.string.builder_import), onDismiss = { importOpen = false }) {
            Text(stringResource(R.string.builder_import_sub), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
            TextIn(text, { text = it }, stringResource(R.string.builder_import_field), lines = 4)
            PrimaryButton(stringResource(R.string.builder_import_go), onClick = {
                val a = MiniShare.import(text)
                if (a == null) scope.launch { snackbar.showSnackbar(bad) }
                else {
                    val inst = MiniLogic.instantiate(a)
                    vm.miniapps.update { it.copy(apps = it.apps + inst) }
                    importOpen = false; onOpen(inst.id)
                }
            }, enabled = text.isNotBlank(), modifier = Modifier.fillMaxWidth(), color = Noc.cyan)
        }
    }
}

// ======================= Egy mini-app szerkesztője =======================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MiniAppEditor(vm: ModsViewModel, appId: String, onDone: () -> Unit, onDeleted: () -> Unit) {
    val doc by vm.miniapps.state.collectAsState()
    val app = doc.app(appId)
    if (app == null) { LaunchedEffect(Unit) { onDeleted() }; return }
    val ctx = LocalContext.current
    var addOpen by remember { mutableStateOf(false) }
    var editBlock by remember { mutableStateOf<Block?>(null) }
    var delAsk by remember { mutableStateOf(false) }
    val col = trackerColor(app.color)
    fun upd(fn: (MiniApp) -> MiniApp) = vm.miniapps.update { d -> d.copy(apps = d.apps.map { if (it.id == appId) fn(it) else it }) }
    val shareTitle = stringResource(R.string.builder_share)
    ModScaffold(
        stringResource(R.string.builder_edit), app.name, col, onDone,
        onAdd = { addOpen = true },
        headerExtra = {
            IconButton(onClick = {
                val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, MiniShare.export(app))
                runCatching { ctx.startActivity(Intent.createChooser(i, shareTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }) { Icon(Icons.Outlined.Share, shareTitle, tint = Noc.text2) }
        },
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = ModListPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                AppCard {
                    var name by remember(appId) { mutableStateOf(app.name) }
                    TextIn(name, { name = it; upd { a -> a.copy(name = it) } }, stringResource(R.string.habit_name))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.habit_icon), style = MaterialTheme.typography.labelLarge, color = Noc.text2)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MINI_ICONS.forEach { ic ->
                            val on = ic == app.icon
                            Box(
                                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(if (on) col.copy(alpha = 0.18f) else Noc.well)
                                    .border(1.dp, if (on) col else Noc.stroke, RoundedCornerShape(12.dp)).clickable { upd { it.copy(icon = ic) } },
                                contentAlignment = Alignment.Center,
                            ) { Icon(iconFor(ic), null, tint = if (on) col else Noc.text2) }
                        }
                    }
                    Spacer(Modifier.size(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        (0..5).forEach { i ->
                            Box(Modifier.size(30.dp).clip(CircleShape).background(trackerColor(i)).border(3.dp, if (i == app.color) Noc.text else Color.Transparent, CircleShape)
                                .clickable { upd { it.copy(color = i) } })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.trackers_on_home), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Switch(app.onHome, { on -> upd { it.copy(onHome = on) } })
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(stringResource(R.string.builder_blocks), Modifier.weight(1f))
                    TextButton(onClick = { addOpen = true }) { Text("+ " + stringResource(R.string.builder_add_block), color = col) }
                }
            }
            if (app.blocks.isEmpty()) item { EmptyHint(stringResource(R.string.builder_blocks_empty)) }
            itemsIndexed(app.blocks, key = { _, b -> b.id }) { i, b ->
                AppCard(onClick = { editBlock = b }, padding = PaddingValues(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(iconFor(blockIcon(b.type)), col, 34)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(b.title.ifBlank { blockName(b.type) }, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(blockName(b.type), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                        }
                        IconButton(onClick = { upd { a -> a.copy(blocks = move(a.blocks, i, -1)) } }, enabled = i > 0) { Icon(Icons.Outlined.KeyboardArrowUp, stringResource(R.string.move_up)) }
                        IconButton(onClick = { upd { a -> a.copy(blocks = move(a.blocks, i, 1)) } }, enabled = i < app.blocks.size - 1) { Icon(Icons.Outlined.KeyboardArrowDown, stringResource(R.string.move_down)) }
                    }
                }
            }
            item {
                Row {
                    TonalButton(stringResource(R.string.builder_done), onClick = onDone, modifier = Modifier.weight(1f), tint = col)
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { delAsk = true }) { Icon(Icons.Outlined.Delete, null, tint = Noc.coral); Text(" " + stringResource(R.string.delete), color = Noc.coral) }
                }
            }
        }
    }
    if (addOpen) FormSheet(stringResource(R.string.builder_add_block), onDismiss = { addOpen = false }) {
        BlockTypes.ALL.forEach { t ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Noc.card).border(1.dp, Noc.stroke, RoundedCornerShape(16.dp))
                    .clickable {
                        val nb = Block(type = t, cfg = defaultCfg(t, app))
                        upd { it.copy(blocks = it.blocks + nb) }
                        addOpen = false; editBlock = nb
                    }.padding12(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(iconFor(blockIcon(t)), col, 36)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(blockName(t), style = MaterialTheme.typography.titleSmall)
                    Text(blockDesc(t), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                }
            }
        }
    }
    editBlock?.let { b -> BlockConfigSheet(app, b, col, onSave = { nb -> upd { a -> a.copy(blocks = a.blocks.map { if (it.id == nb.id) nb else it }) }; editBlock = null },
        onDelete = { upd { a -> a.copy(blocks = a.blocks.filter { it.id != b.id }) }; vm.miniapps.update { d -> d.copy(data = d.data - b.id) }; editBlock = null },
        onDismiss = { editBlock = null }) }
    if (delAsk) AlertDialog(
        onDismissRequest = { delAsk = false },
        title = { Text(stringResource(R.string.builder_delete_q, app.name)) },
        text = { Text(stringResource(R.string.builder_delete_text)) },
        confirmButton = {
            TextButton(onClick = {
                vm.miniapps.update { d -> d.copy(apps = d.apps.filter { it.id != appId }, data = d.data - app.blocks.map { it.id }.toSet()) }
                delAsk = false; onDeleted()
            }) { Text(stringResource(R.string.delete), color = Noc.coral) }
        },
        dismissButton = { TextButton(onClick = { delAsk = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

private fun Modifier.padding12() = this.then(Modifier.padding(12.dp))

private fun move(l: List<Block>, i: Int, dir: Int): List<Block> {
    val j = i + dir
    if (j !in l.indices) return l
    return l.toMutableList().also { val t = it[i]; it[i] = it[j]; it[j] = t }
}

private fun defaultCfg(t: String, app: MiniApp): BlockCfg = when (t) {
    BlockTypes.CHART -> BlockCfg(source = app.blocks.lastOrNull { it.type in listOf(BlockTypes.TRACKER, BlockTypes.TIMER, BlockTypes.CHECKLIST, BlockTypes.JOURNAL) }?.id ?: "")
    BlockTypes.SAVINGS -> BlockCfg(perDay = 1000.0)
    else -> BlockCfg()
}

@Composable
private fun BlockConfigSheet(app: MiniApp, b: Block, col: Color, onSave: (Block) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(b.title) }
    var text by remember { mutableStateOf(b.cfg.text) }
    var items by remember { mutableStateOf(b.cfg.items.joinToString("\n")) }
    var milestones by remember { mutableStateOf(b.cfg.milestones.joinToString(", ")) }
    var levels by remember { mutableStateOf(b.cfg.levelNames.joinToString("\n")) }
    var kind by remember { mutableStateOf(b.cfg.kind) }
    var unit by remember { mutableStateOf(b.cfg.unit) }
    var goal by remember { mutableStateOf(fmtVal(b.cfg.goal)) }
    var perDay by remember { mutableStateOf(b.cfg.perDay.toLong().toString()) }
    var minutes by remember { mutableStateOf(b.cfg.minutes.toString()) }
    var time by remember { mutableStateOf(b.cfg.time) }
    var source by remember { mutableStateOf(b.cfg.source) }
    var days by remember { mutableStateOf(b.cfg.days) }
    var pickTime by remember { mutableStateOf(false) }
    FormSheet(blockName(b.type), onDismiss) {
        Text(blockDesc(b.type), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
        TextIn(title, { title = it }, stringResource(R.string.builder_block_title), placeholder = blockName(b.type))
        when (b.type) {
            BlockTypes.TEXT -> TextIn(text, { text = it }, stringResource(R.string.builder_text), lines = 4)
            BlockTypes.STREAK -> {
                TextIn(milestones, { milestones = it }, stringResource(R.string.builder_milestones))
                TextIn(levels, { levels = it }, stringResource(R.string.builder_levels), lines = 4, placeholder = stringResource(R.string.builder_levels_hint))
            }
            BlockTypes.TRACKER -> {
                val l1 = stringResource(R.string.tk_check); val l2 = stringResource(R.string.tk_count); val l3 = stringResource(R.string.tk_number); val l4 = stringResource(R.string.tk_scale)
                ChipRow(listOf("check", "count", "number", "scale"), kind, { when (it) { "check" -> l1; "count" -> l2; "number" -> l3; else -> l4 } }, col) { kind = it }
                if (kind == "count" || kind == "number") Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumIn(goal, { goal = it }, stringResource(R.string.trackers_goal), Modifier.weight(1f), decimal = true)
                    TextIn(unit, { unit = it }, stringResource(R.string.trackers_unit), Modifier.weight(1f))
                }
            }
            BlockTypes.CHECKLIST -> TextIn(items, { items = it }, stringResource(R.string.builder_items), lines = 5, placeholder = stringResource(R.string.builder_items_hint))
            BlockTypes.SOS -> TextIn(items, { items = it }, stringResource(R.string.builder_tips), lines = 5, placeholder = stringResource(R.string.builder_tips_hint))
            BlockTypes.JOURNAL -> TextIn(items, { items = it }, stringResource(R.string.builder_tags), lines = 3, placeholder = stringResource(R.string.builder_tags_hint))
            BlockTypes.SAVINGS -> {
                NumIn(perDay, { perDay = it }, stringResource(R.string.habit_cost), suffix = "Ft")
                Text(stringResource(R.string.builder_savings_hint), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
            }
            BlockTypes.TIMER -> NumIn(minutes, { minutes = it }, stringResource(R.string.duration), suffix = stringResource(R.string.min_short))
            BlockTypes.CHART -> {
                val srcs = app.blocks.filter { it.type in listOf(BlockTypes.TRACKER, BlockTypes.TIMER, BlockTypes.CHECKLIST, BlockTypes.JOURNAL, BlockTypes.STREAK) }
                Text(stringResource(R.string.builder_chart_src), style = MaterialTheme.typography.labelLarge, color = Noc.text2)
                if (srcs.isEmpty()) Text(stringResource(R.string.mini_chart_nosrc), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                else ChipRow(srcs.map { it.id }, source, { id -> srcs.first { it.id == id }.let { it.title.ifBlank { blockName(it.type) } } }, col) { source = it }
                ChipRow(listOf(7, 14, 30), days, { stringResource(R.string.n_days, it) }, col) { days = it }
            }
            BlockTypes.REMINDER -> {
                TonalButton(stringResource(R.string.reminder_time, time), onClick = { pickTime = true })
                TextIn(text, { text = it }, stringResource(R.string.builder_reminder_text))
                Text(stringResource(R.string.builder_reminder_hint), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
            }
        }
        PrimaryButton(stringResource(R.string.save), onClick = {
            val lines = items.lines().map { it.trim() }.filter { it.isNotBlank() }
            onSave(
                b.copy(
                    title = title.trim(),
                    cfg = b.cfg.copy(
                        text = text.trim(), items = lines,
                        milestones = milestones.split(',', ' ', ';').mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }.distinct().sorted().ifEmpty { b.cfg.milestones },
                        levelNames = levels.lines().map { it.trim() }.filter { it.isNotBlank() },
                        kind = kind, unit = unit.trim(), goal = (goal.toNum() ?: 1.0).coerceAtLeast(0.1),
                        perDay = perDay.toNum() ?: 0.0, minutes = (minutes.toIntOrNull() ?: 10).coerceIn(1, 600),
                        time = time, source = source, days = days,
                    ),
                )
            )
        }, modifier = Modifier.fillMaxWidth(), color = col)
        TextButton(onClick = onDelete) { Text(stringResource(R.string.builder_remove_block), color = Noc.coral) }
    }
    if (pickTime) TimePickDialog(runCatching { LocalTime.parse(time) }.getOrDefault(LocalTime.of(20, 0)), { pickTime = false }) { time = it.format(Fmt.time) }
}


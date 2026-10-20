package hu.lifeos.app.ui.screens

import hu.lifeos.app.food.packFlags

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.data.EntryEntity
import hu.lifeos.app.data.EntryKind
import hu.lifeos.app.food.FoodLogic
import hu.lifeos.app.food.FoodViewModel
import hu.lifeos.app.insight.Insights
import hu.lifeos.app.satori.SatoriViewModel
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.EmptyHint
import hu.lifeos.app.ui.Fmt
import hu.lifeos.app.ui.MainViewModel
import hu.lifeos.app.ui.MetricRing
import hu.lifeos.app.ui.Pill
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.rememberTick
import hu.lifeos.app.ui.theme.Noc
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.IsoFields

private data class TItem(
    val minute: Int, val time: String, val icon: ImageVector, val color: Color, val title: String, val sub: String?,
    val tags: List<String>, val filter: String, val entry: EntryEntity? = null, val onTap: (() -> Unit)? = null, val done: Boolean? = null,
)

@Composable
fun TimelineScreen(
    vm: MainViewModel, satori: SatoriViewModel, food: FoodViewModel, contentPadding: PaddingValues,
    onOpenSatori: () -> Unit, onOpenFood: () -> Unit, onEditEvent: (Long) -> Unit,
) {
    val selected by vm.selectedDay.collectAsState()
    val agenda by vm.monthAgenda.collectAsState()
    val entries by vm.recentEntries.collectAsState()
    val moods by vm.moodHistory.collectAsState()
    val todos by vm.todos.collectAsState()
    val st by satori.state.collectAsState()
    val fs by food.state.collectAsState()
    val settingsS by vm.settings.collectAsState()
    val foodOn = settingsS?.moduleOn(hu.lifeos.app.data.Modules.FOOD) ?: true
    val satoriOn = settingsS?.moduleOn(hu.lifeos.app.data.Modules.SATORI) ?: true
    val packs = fs.packFlags()
    val c = satori.content
    var filter by rememberSaveable { mutableStateOf("all") }
    var toDelete by remember { mutableStateOf<EntryEntity?>(null) }
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val isToday = selected == today
    val nowMin = LocalTime.now().let { it.hour * 60 + it.minute }
    val tick = rememberTick()
    val dk = selected.toString()

    val moodWords = listOf(R.string.mood_1, R.string.mood_2, R.string.mood_3, R.string.mood_4, R.string.mood_5).map { stringResource(it) }
    val stateLabels = c.stateDefs.associate { it.id to it.label }
    val tagHealth = stringResource(R.string.tag_health); val tagMoney = stringResource(R.string.filter_money)
    val tagCal = stringResource(R.string.tab_calendar); val tagTodo = stringResource(R.string.type_todo); val tagNote = stringResource(R.string.type_note)
    val dayCloseLabel = stringResource(R.string.satori_dayclose)
    val windowOpen = stringResource(R.string.window_open_title); val windowClose = stringResource(R.string.window_close_title)

    val items = buildList {
        fun hm(t: String) = t.split(":").let { (it.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (it.getOrNull(1)?.toIntOrNull() ?: 0) }
        vm.itemsOn(agenda, selected).forEach { a ->
            val t = Instant.ofEpochMilli(a.start).atZone(zone).toLocalTime()
            val te = Instant.ofEpochMilli(a.end).atZone(zone).toLocalTime()
            add(TItem(if (a.allDay) -1 else t.hour * 60 + t.minute, if (a.allDay) "" else t.format(Fmt.time), Icons.Outlined.Event, a.color?.let { Color(it) } ?: Noc.sapphire,
                a.title, (if (a.allDay) "" else t.format(Fmt.time) + " – " + te.format(Fmt.time)) + if (a.location.isNotBlank()) " · ${a.location}" else "",
                listOf(tagCal), "cal", onTap = a.own?.let { o -> { onEditEvent(o.id) } }))
        }
        entries.filter { it.date == dk }.forEach { e ->
            val t = Instant.ofEpochMilli(e.ts).atZone(zone).toLocalTime()
            val money = e.kind == EntryKind.EXPENSE || e.kind == EntryKind.INCOME
            val title = when (e.kind) {
                EntryKind.EXPENSE -> "− " + Fmt.money(e.amount ?: 0.0)
                EntryKind.INCOME -> "+ " + Fmt.money(e.amount ?: 0.0)
                else -> e.text
            }
            add(TItem(t.hour * 60 + t.minute, t.format(Fmt.time), if (money) Icons.Outlined.AttachMoney else Icons.Outlined.Description,
                if (money) Noc.sapphire else Noc.ice, title, if (money) e.text.ifBlank { null } else null,
                listOf(if (money) tagMoney else tagNote), if (money) "money" else "note", entry = e))
        }
        if (foodOn) fs.day(dk).items.groupBy { it.meal }.forEach { (meal, its) ->
            val tot = food.logic.totals(its)
            val first = its.minByOrNull { it.t }?.t ?: "12:00"
            val tags = when {
                packs.numbers -> listOf("${FoodLogic.fmt(tot.kcal)} kcal", "F ${FoodLogic.fmt(tot.p)} g")
                packs.diab -> listOf("CH ${FoodLogic.fmt(tot.c)} g")
                else -> listOf(tagHealth)
            }
            add(TItem(hm(first), first, Icons.Outlined.Restaurant, Noc.good, meal,
                its.joinToString(", ") { it.name }, tags, "health", onTap = onOpenFood))
        }
        if (foodOn && packs.fast && fs.setupDone) {
            add(TItem(hm(fs.profile.winStart), fs.profile.winStart, Icons.Outlined.Timer, Noc.cyan, windowOpen, null, listOf(tagHealth), "health"))
            add(TItem(hm(fs.profile.winEnd), fs.profile.winEnd, Icons.Outlined.Timer, Noc.cyan, windowClose, null, listOf(tagHealth), "health"))
        }
        moods.firstOrNull { it.date == dk }?.let { m ->
            if (m.mood != null) add(TItem(Instant.ofEpochMilli(m.updatedAt).atZone(zone).toLocalTime().let { it.hour * 60 + it.minute }, "", Icons.Outlined.Mood, Noc.violet,
                moodWords.getOrElse(m.mood - 1) { "" }, null, listOf(tagHealth), "health"))
        }
        if (satoriOn) st.data.forEach { (typeId, td) ->
            td.journal.filter { it.date == dk }.forEach { j ->
                add(TItem(hm(j.time), j.time, Icons.Outlined.Spa, Noc.amber,
                    (if (j.source == "dayclose") "$dayCloseLabel · " else "") + (stateLabels[j.state] ?: j.state),
                    j.note.ifBlank { null }, listOf("Satori · " + c.type(typeId).label), "satori", onTap = onOpenSatori))
            }
        }
        todos.filter { (it.done && it.doneAt != null && Instant.ofEpochMilli(it.doneAt).atZone(zone).toLocalDate() == selected) || (!it.done && it.dueDate == dk) }.forEach { t ->
            val tm = if (t.done) Instant.ofEpochMilli(t.doneAt!!).atZone(zone).toLocalTime() else t.dueTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
            add(TItem(tm?.let { it.hour * 60 + it.minute } ?: 23 * 60 + 59, tm?.format(Fmt.time) ?: "", if (t.done) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                if (t.done) Noc.good else Noc.text2, t.title, null, listOf(tagTodo), "cal", onTap = { vm.toggleTodo(t) }, done = t.done))
        }
    }.filter { filter == "all" || it.filter == filter }.sortedBy { it.minute }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(stringResource(R.string.timeline_kicker).uppercase(), style = MaterialTheme.typography.labelSmall, color = Noc.cyan)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.timeline_title), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Noc.card).border(1.dp, Noc.stroke, RoundedCornerShape(50)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { tick(); vm.selectDay(selected.minusWeeks(1)) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, tint = Noc.text2) }
                    Text(stringResource(R.string.week_n, selected.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)), style = MaterialTheme.typography.labelLarge)
                    IconButton(onClick = { tick(); vm.selectDay(selected.plusWeeks(1)) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Noc.text2) }
                }
            }
        }
        // ---------- Heti sáv ----------
        item {
            val monday = selected.with(DayOfWeek.MONDAY)
            AppCard(padding = PaddingValues(8.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    (0..6).forEach { i ->
                        val d = monday.plusDays(i.toLong())
                        val on = d == selected
                        val has = vm.itemsOn(agenda, d).isNotEmpty() || fs.day(d.toString()).items.isNotEmpty() || entries.any { it.date == d.toString() }
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                .background(if (on) Noc.cardElevated else Color.Transparent)
                                .let { if (on) it.border(1.dp, Noc.cyan.copy(alpha = 0.7f), RoundedCornerShape(14.dp)) else it }
                                .clickable { tick(); vm.selectDay(d) }.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(Fmt.weekdayShort(d.dayOfWeek), style = MaterialTheme.typography.labelSmall, color = if (on) Noc.cyan else Noc.text3)
                            Text("${d.dayOfMonth}", style = MaterialTheme.typography.titleMedium, color = if (on) Noc.text else if (d == today) Noc.cyan else Noc.text2)
                            Box(Modifier.padding(top = 3.dp).size(4.dp).clip(CircleShape).background(if (has) (if (on) Noc.cyan else Noc.text3) else Color.Transparent))
                        }
                    }
                }
            }
        }
        // ---------- Szűrők ----------
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "all" to R.string.filter_all, "health" to R.string.tag_health, "cal" to R.string.tab_calendar,
                    "money" to R.string.filter_money, "satori" to R.string.satori_name, "note" to R.string.type_note,
                ).forEach { (id, l) ->
                    val on = filter == id
                    Text(
                        stringResource(l), style = MaterialTheme.typography.labelLarge, color = if (on) Noc.abyss else Noc.text2,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) Noc.cyan else Noc.card)
                            .border(1.dp, if (on) Noc.cyan else Noc.stroke, RoundedCornerShape(50))
                            .clickable { tick(); filter = id }.padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
            }
        }
        if (items.isEmpty()) item { EmptyHint(stringResource(R.string.timeline_empty)) }

        var nowShown = !isToday
        items.forEachIndexed { i, it ->
            if (!nowShown && it.minute > nowMin) {
                nowShown = true
                item { NowCard(food, nowMin, foodOn) }
            }
            val past = selected.isBefore(today) || (isToday && it.minute in 0..nowMin)
            item { TimelineRow(it, past, isFirst = i == 0, isLast = i == items.lastIndex && nowShown, onLongPress = { e -> toDelete = e }) }
        }
        if (!nowShown) item { NowCard(food, nowMin, foodOn) }
    }

    toDelete?.let { e ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.delete_entry_q)) },
            text = { Text(e.text.ifBlank { Fmt.money(e.amount ?: 0.0) }) },
            confirmButton = { TextButton(onClick = { vm.deleteEntry(e); toDelete = null }) { Text(stringResource(R.string.delete), color = Noc.coral) } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun TimelineRow(it: TItem, past: Boolean, isFirst: Boolean, isLast: Boolean, onLongPress: (EntryEntity) -> Unit) {
    val line = Noc.stroke
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(Modifier.width(26.dp).fillMaxHeight()) {
            Canvas(Modifier.fillMaxSize()) {
                val x = size.width / 2
                drawLine(line, Offset(x, if (isFirst) 26f else 0f), Offset(x, if (isLast) 26f else size.height), 2f)
            }
            Box(
                Modifier.padding(top = 18.dp).align(Alignment.TopCenter).size(14.dp).clip(CircleShape)
                    .background(Noc.canvas).border(2.dp, if (past) it.color.copy(alpha = 0.6f) else it.color, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Box(Modifier.size(5.dp).clip(CircleShape).background(it.color)) }
        }
        Spacer(Modifier.width(8.dp))
        AppCard(
            Modifier.weight(1f).padding(bottom = 2.dp),
            onClick = it.onTap ?: it.entry?.let { e -> { onLongPress(e) } },
            padding = PaddingValues(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (it.time.isNotBlank()) Text(
                    it.time, style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"), color = it.color,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(it.color.copy(alpha = 0.12f)).border(1.dp, it.color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (past) R.string.past else R.string.future), style = MaterialTheme.typography.labelMedium, color = Noc.text3, modifier = Modifier.weight(1f))
                Icon(it.icon, null, Modifier.size(18.dp), tint = it.color)
            }
            VSpace(8)
            Text(it.title, style = MaterialTheme.typography.titleMedium, color = if (it.done == true) Noc.text2 else Noc.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!it.sub.isNullOrBlank()) Text(it.sub, style = MaterialTheme.typography.bodyMedium, color = Noc.text2, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (it.tags.isNotEmpty()) {
                VSpace(8)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { it.tags.forEach { t -> Pill(t, textColor = Noc.text2, color = Noc.well) } }
            }
        }
    }
}

@Composable
private fun NowCard(food: FoodViewModel, nowMin: Int, foodOn: Boolean) {
    val fs by food.state.collectAsState()
    val packs = fs.packFlags()
    val f = Insights.fast(fs, food.logic)
    val r = food.logic.targets(fs)
    val t = food.logic.totals(fs.day(LocalDate.now().toString()).items)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(Modifier.width(26.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.padding(top = 20.dp).size(18.dp).clip(CircleShape).background(Noc.cyan.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(Noc.cyan))
            }
        }
        Spacer(Modifier.width(8.dp))
        AppCard(Modifier.weight(1f), glow = Noc.cyan, padding = PaddingValues(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(Noc.cyan))
                Text("  " + stringResource(R.string.now_label) + " · %02d:%02d".format(nowMin / 60, nowMin % 60), style = MaterialTheme.typography.labelLarge, color = Noc.cyan, modifier = Modifier.weight(1f))
                if (foodOn && packs.fast) Text(
                    if (f.eating) stringResource(R.string.window_left_short, f.minutesLeft / 60, f.minutesLeft % 60)
                    else stringResource(R.string.fast_short, f.minutesIn / 60, f.minutesIn % 60),
                    style = MaterialTheme.typography.labelLarge, color = Noc.amber,
                )
            }
            if (foodOn && packs.numbers && fs.setupDone) VSpace(8)
            if (foodOn && packs.numbers && fs.setupDone) Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(if (t.kcal <= r.target && (!packs.ir || t.gl < 80)) R.string.zone_balanced else R.string.zone_watch), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.zone_kcal, FoodLogic.fmt(t.kcal), FoodLogic.fmt(r.target)), style = MaterialTheme.typography.bodySmall, color = Noc.cyan)
                }
                val frac = if (r.target > 0) (t.kcal / r.target).toFloat() else 0f
                MetricRing(frac, Noc.cyan, Modifier.size(52.dp), stroke = 5.dp) {
                    Text("${(frac * 100).toInt().coerceAtMost(999)}%", style = MaterialTheme.typography.labelMedium, color = Noc.text)
                }
            }
        }
    }
}

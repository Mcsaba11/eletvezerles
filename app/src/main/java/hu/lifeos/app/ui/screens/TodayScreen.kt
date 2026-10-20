package hu.lifeos.app.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.SentimentVeryDissatisfied
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.data.AppSettings
import hu.lifeos.app.data.HomeCards
import hu.lifeos.app.data.Modules
import hu.lifeos.app.data.EntryKind
import hu.lifeos.app.data.TodoEntity
import hu.lifeos.app.food.FoodViewModel
import hu.lifeos.app.insight.Insights
import hu.lifeos.app.satori.SatoriLogic
import hu.lifeos.app.satori.SatoriViewModel
import hu.lifeos.app.ui.AgendaItem
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.CardHeader
import hu.lifeos.app.ui.Fmt
import hu.lifeos.app.ui.MainViewModel
import hu.lifeos.app.ui.Meter
import hu.lifeos.app.ui.MetricRing
import hu.lifeos.app.ui.Pill
import hu.lifeos.app.ui.PrimaryButton
import hu.lifeos.app.ui.StatusChip
import hu.lifeos.app.ui.TonalButton
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.iconFor
import hu.lifeos.app.ui.rememberTick
import hu.lifeos.app.ui.theme.Noc
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun TodayScreen(
    vm: MainViewModel,
    satori: SatoriViewModel,
    food: FoodViewModel,
    contentPadding: PaddingValues,
    onOpenSatori: () -> Unit,
    onOpenFood: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenTodos: () -> Unit,
    onOpenSensei: () -> Unit,
    onOpenPatterns: () -> Unit,
    onOpenSettings: () -> Unit,
    onBreath: () -> Unit,
    onCapture: () -> Unit,
    onVoice: () -> Unit,
) {
    val settings by vm.settings.collectAsState()
    val today by vm.today.collectAsState()
    val mood by vm.todayMood.collectAsState()
    val agenda by vm.todayAgenda.collectAsState()
    val todos by vm.todos.collectAsState()
    val entries by vm.recentEntries.collectAsState()
    val wheel by vm.lifeWheel.collectAsState()
    val st by satori.state.collectAsState()
    val fs by food.state.collectAsState()
    val c = satori.content
    val tick = rememberTick()

    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = LocalTime.now() } }

    val todayStr = today.toString()
    val zone = ZoneId.systemDefault()
    val nowMs = System.currentTimeMillis()
    val nextEvent = agenda.firstOrNull { !it.allDay && it.end > nowMs }
    val nextLabel = nextEvent?.let { Instant.ofEpochMilli(it.start).atZone(zone).toLocalTime().format(Fmt.time) + " " + it.title }

    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ---------- Ritmus és köszöntés ----------
        item {
            val flow = Insights.inFlow(st, mood)
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(stringResource(if (flow) R.string.rhythm_balanced else R.string.rhythm_care), if (flow) Noc.good else Noc.amber)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Outlined.WbSunny, null, Modifier.size(16.dp), tint = Noc.amber)
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(when (now.hour) { in 5..10 -> R.string.state_morning; in 11..17 -> R.string.state_day; else -> R.string.state_evening }),
                    style = MaterialTheme.typography.labelLarge, color = Noc.amber,
                )
            }
            VSpace(8)
            Row(verticalAlignment = Alignment.Bottom) {
                val greet = stringResource(when (now.hour) { in 5..9 -> R.string.greet_morning; in 10..17 -> R.string.greet_day; else -> R.string.greet_evening })
                val name = settings?.name.orEmpty()
                Text(if (name.isBlank()) greet else "$greet, $name", style = MaterialTheme.typography.headlineLarge, color = Noc.text, modifier = Modifier.weight(1f))
            }
        }

        val cards = HomeCards.visible(settings ?: AppSettings())
        if (cards.none { it != "capture" && it != "insight" && it != "quote" }) item {
            AppCard(onClick = onOpenSettings) {
                Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.home_empty_text), style = MaterialTheme.typography.bodyMedium, color = Noc.text2)
            }
        }
        cards.forEach { card ->
            when (card) {
                "capture" -> {
                // ---------- Beviteli mező ----------
                item {
                    AppCard(onClick = onCapture, padding = PaddingValues(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Noc.cyan.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(18.dp), tint = Noc.cyan)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.capture_prompt), style = MaterialTheme.typography.bodyLarge, color = Noc.text2, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Box(
                                Modifier.size(34.dp).clip(CircleShape).background(Noc.amber.copy(alpha = 0.12f)).border(1.dp, Noc.amber.copy(alpha = 0.35f), CircleShape)
                                    .clickable { tick(); onVoice() },
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Outlined.Mic, stringResource(R.string.voice_input), Modifier.size(18.dp), tint = Noc.amber) }
                        }
                        VSpace(8)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.example) + "  ", style = MaterialTheme.typography.bodySmall, color = Noc.text3)
                            Text(stringResource(R.string.capture_example), style = MaterialTheme.typography.bodySmall, color = Noc.text2, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Noc.well).padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                }
                }
                "insight" -> {
                // ---------- Sensei meglátás ----------
                if (settings?.insightDismissed != todayStr) item {
                    val text = Insights.dailyInsight(fs, food.logic, st, c, mood, todos, nextLabel, now, foodOn = settings?.moduleOn(Modules.FOOD) ?: true, satoriOn = settings?.moduleOn(Modules.SATORI) ?: true)
                    AppCard(glow = Noc.cyan) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Noc.cyan.copy(alpha = 0.12f)).border(1.dp, Noc.cyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Psychology, null, Modifier.size(20.dp), tint = Noc.cyan)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.sensei_insight), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Pill(stringResource(when (now.hour) { in 5..10 -> R.string.sync_morning; in 11..17 -> R.string.sync_day; else -> R.string.sync_evening }))
                        }
                        VSpace(8)
                        Text(text, style = MaterialTheme.typography.bodyLarge, color = Noc.text)
                        VSpace(12)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrimaryButton(stringResource(R.string.ok_got_it), onClick = { vm.updateSettings { it.copy(insightDismissed = todayStr) } })
                            TonalButton(stringResource(R.string.details), onClick = onOpenSensei)
                        }
                    }
                }
                }
                "food" -> {
                // ---------- Étkezés ----------
                item { FoodHomeCard(food, fs, now, onOpenFood) }
                }
                "breath" -> if ("satori" !in cards) item { Row { BreathTile(Modifier.fillMaxWidth(), onBreath) } }
                "satori" -> {
                    val withBreath = "breath" in cards
                // ---------- Satori + légzés ----------
                item {
                    val d = st.cur()
                    val qs = SatoriLogic.themeQuotes(c, st)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppCard(Modifier.weight(1f).height(170.dp), onClick = onOpenSatori, padding = PaddingValues(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(iconFor(c.type(st.type).icon), null, Modifier.size(18.dp), tint = Noc.cyan)
                                Spacer(Modifier.weight(1f))
                                Text("SATORI", style = MaterialTheme.typography.labelSmall, color = Noc.text2)
                            }
                            Spacer(Modifier.weight(1f))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("${d.streak}", style = MaterialTheme.typography.headlineLarge.copy(fontFeatureSettings = "tnum"), color = Noc.text)
                                Text(" " + stringResource(R.string.satori_day_word), style = MaterialTheme.typography.titleSmall, color = Noc.text, modifier = Modifier.padding(bottom = 4.dp))
                            }
                            Text(if (st.onboardingDone == true) c.type(st.type).label else stringResource(R.string.satori_setup_hint), style = MaterialTheme.typography.bodySmall, color = Noc.cyan, maxLines = 2)
                            VSpace(6)
                            Text("„${qs[SatoriLogic.dayIndex(qs.size)]}”", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = Noc.text3, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        if (withBreath) BreathTile(Modifier.weight(1f), onBreath)
                    }
                }
                }
                "mood" -> {
                // ---------- Hangulat ----------
                item {
                    AppCard(padding = PaddingValues(14.dp)) {
                        CardHeader(Icons.Outlined.Mood, stringResource(R.string.mood_title), tint = Noc.violet) {
                            if (mood?.mood != null) Text("${mood?.mood}/5", style = MaterialTheme.typography.labelLarge, color = Noc.violet)
                        }
                        VSpace(10)
                        MoodRow(selected = mood?.mood, onSelect = { vm.setMood(it, null) }, tint = Noc.violet)
                        VSpace(8)
                        EnergyRow(selected = mood?.energy, onSelect = { vm.setMood(null, it) }, tint = Noc.violet)
                    }
                }
                }
                "money" -> {
                // ---------- Pénz ----------
                item {
                    val month = today.withDayOfMonth(1)
                    val spent = entries.filter { it.kind == EntryKind.EXPENSE && runCatching { !LocalDate.parse(it.date).isBefore(month) }.getOrDefault(false) }.sumOf { it.amount ?: 0.0 }
                    val budget = settings?.monthlyBudget ?: 0.0
                    AppCard(onClick = if (budget > 0) null else onOpenSettings) {
                        CardHeader(Icons.Outlined.AccountBalanceWallet, stringResource(R.string.card_money), tint = Noc.sapphire) {
                            if (budget > 0) Text("${Fmt.monthYear(today).substringAfter(". ")} (${(spent * 100 / budget).toInt()}%)", style = MaterialTheme.typography.labelLarge, color = Noc.sapphire)
                        }
                        VSpace(10)
                        if (budget > 0) {
                            val daysLeft = today.lengthOfMonth() - today.dayOfMonth + 1
                            val remain = budget - spent
                            Row(verticalAlignment = Alignment.Bottom) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(R.string.budget_left), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                                    Text(Fmt.money(remain), style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"), color = if (remain >= 0) Noc.text else Noc.coral)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(stringResource(R.string.budget_daily), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                                    Text(Fmt.money(maxOf(0.0, remain / daysLeft)), style = MaterialTheme.typography.titleLarge, color = Noc.good)
                                }
                            }
                            VSpace(10)
                            Meter((spent / budget).toFloat(), if (spent <= budget * today.dayOfMonth / today.lengthOfMonth()) Noc.cyan else Noc.amber)
                            VSpace(8)
                            val onTrack = spent <= budget * today.dayOfMonth / today.lengthOfMonth()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.budget_spent, Fmt.money(spent)), style = MaterialTheme.typography.bodySmall, color = Noc.text2, modifier = Modifier.weight(1f))
                                Icon(Icons.Outlined.CheckCircle, null, Modifier.size(14.dp), tint = if (onTrack) Noc.good else Noc.amber)
                                Text(" " + stringResource(if (onTrack) R.string.budget_on_track else R.string.budget_over_pace), style = MaterialTheme.typography.labelMedium, color = if (onTrack) Noc.good else Noc.amber)
                            }
                        } else {
                            Text(stringResource(R.string.budget_spent, Fmt.money(spent)), style = MaterialTheme.typography.titleMedium, color = Noc.text)
                            Text(stringResource(R.string.budget_set_hint), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                        }
                    }
                }
                }
                "focus" -> {
                // ---------- Mai fókusz és idővonal ----------
                item {
                    val due = todos.filter { !it.done && ((it.dueDate != null && it.dueDate <= todayStr) || (it.dueDate == null && it.important)) }
                    AppCard(onClick = onOpenCalendar) {
                        CardHeader(Icons.Outlined.EventNote, stringResource(R.string.card_focus), tint = Noc.sapphire) {
                            Text(stringResource(R.string.todos_today_n, due.size), style = MaterialTheme.typography.labelMedium, color = Noc.text2)
                        }
                        VSpace(10)
                        if (agenda.isEmpty() && due.isEmpty()) Text(stringResource(R.string.focus_empty), style = MaterialTheme.typography.bodyMedium, color = Noc.text2)
                        agenda.take(5).forEach { a -> FocusRow(a, a == nextEvent, nowMs) }
                        due.take(5).forEach { t -> TodoMini(t, today) { vm.toggleTodo(t) } }
                        if (due.size > 5) Text(stringResource(R.string.and_more, due.size - 5), style = MaterialTheme.typography.bodySmall, color = Noc.cyan, modifier = Modifier.clickable(onClick = onOpenTodos))
                    }
                }
                }
                "wheel" -> {
                // ---------- Életkerék ----------
                item {
                    val mk = today.toString().take(7)
                    val cur = wheel[mk]
                    AppCard(onClick = onOpenPatterns, padding = PaddingValues(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Noc.well).border(1.dp, Noc.stroke, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Tune, null, tint = Noc.cyan)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.wheel_title), style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.width(6.dp))
                                    Box(Modifier.size(7.dp).clip(CircleShape).background(if (cur != null) Noc.good else Noc.text3))
                                }
                                Text(
                                    if (cur != null) stringResource(R.string.wheel_best, cur.maxByOrNull { it.value }?.key.orEmpty(), cur.minByOrNull { it.value }?.key.orEmpty())
                                    else stringResource(R.string.wheel_rate_hint),
                                    style = MaterialTheme.typography.bodySmall, color = Noc.text2, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (cur != null) Column(horizontalAlignment = Alignment.End) {
                                Text(buildAnnotatedString {
                                    withStyle(SpanStyle(color = Noc.amber, fontWeight = FontWeight.Bold)) { append(String.format(Fmt.hu, "%.1f", cur.values.average())) }
                                    withStyle(SpanStyle(color = Noc.text2)) { append(" /10") }
                                }, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
                }
                "quote" -> {
                item {
                    val qs = SatoriLogic.themeQuotes(c, st)
                    Text("„${qs[(SatoriLogic.dayIndex(qs.size) + 3) % qs.size]}”", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = Noc.text2,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                }
            }
        }
    }
}

@Composable
private fun BreathTile(modifier: Modifier, onBreath: () -> Unit) {
    AppCard(modifier.height(170.dp), onClick = onBreath, padding = PaddingValues(14.dp), glow = Noc.amber) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Noc.amber.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Spa, null, Modifier.size(16.dp), tint = Noc.amber)
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(7.dp).clip(CircleShape).background(Noc.amber))
        }
        Spacer(Modifier.weight(1f))
        Text("4-7-8", style = MaterialTheme.typography.headlineMedium, color = Noc.text)
        Text(stringResource(R.string.breath_card_sub), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
        VSpace(8)
        Row(
            Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(10.dp)).background(Noc.amber.copy(alpha = 0.14f)).border(1.dp, Noc.amber.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) { Text(stringResource(R.string.start_arrow), style = MaterialTheme.typography.labelLarge, color = Noc.amber) }
    }
}

@Composable
private fun FocusRow(a: AgendaItem, isNext: Boolean, nowMs: Long) {
    val zone = ZoneId.systemDefault()
    val past = a.end < nowMs && !a.allDay
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 6.dp).size(9.dp).clip(CircleShape).background(if (past) Noc.text3 else a.color?.let { Color(it) } ?: Noc.cyan))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(a.title, style = MaterialTheme.typography.titleSmall, color = if (past) Noc.text2 else Noc.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val time = if (a.allDay) stringResource(R.string.all_day) else
                Instant.ofEpochMilli(a.start).atZone(zone).toLocalTime().format(Fmt.time) + " – " + Instant.ofEpochMilli(a.end).atZone(zone).toLocalTime().format(Fmt.time)
            Text(time + if (a.location.isNotBlank()) " · ${a.location}" else "", style = MaterialTheme.typography.bodySmall, color = Noc.text2, maxLines = 1)
        }
        if (isNext) Pill(stringResource(R.string.next_label))
    }
}

@Composable
private fun TodoMini(t: TodoEntity, today: LocalDate, onToggle: () -> Unit) {
    val tick = rememberTick()
    val overdue = t.dueDate != null && t.dueDate < today.toString()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(t.done, { tick(); onToggle() }, colors = CheckboxDefaults.colors(checkedColor = Noc.cyan, uncheckedColor = Noc.text3, checkmarkColor = Noc.abyss))
        Text(t.title, style = MaterialTheme.typography.bodyMedium, color = Noc.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            textDecoration = if (t.done) TextDecoration.LineThrough else null)
        if (overdue) Pill(stringResource(R.string.group_overdue), textColor = Noc.coral)
    }
}

@Composable
fun MoodRow(selected: Int?, onSelect: (Int) -> Unit, tint: Color = MaterialTheme.colorScheme.primary) {
    val tick = rememberTick()
    val icons = listOf(
        Icons.Outlined.SentimentVeryDissatisfied, Icons.Outlined.SentimentDissatisfied, Icons.Outlined.SentimentNeutral,
        Icons.Outlined.SentimentSatisfied, Icons.Outlined.SentimentVerySatisfied,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        icons.forEachIndexed { i, ic ->
            val v = i + 1
            val on = selected == v
            Box(
                Modifier.size(50.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (on) tint.copy(alpha = 0.18f) else Noc.well)
                    .border(1.dp, if (on) tint.copy(alpha = 0.6f) else Noc.stroke, RoundedCornerShape(16.dp))
                    .clickable { tick(); onSelect(v) },
                contentAlignment = Alignment.Center,
            ) { Icon(ic, null, tint = if (on) tint else Noc.text3, modifier = Modifier.size(26.dp)) }
        }
    }
}

@Composable
fun EnergyRow(selected: Int?, onSelect: (Int) -> Unit, @Suppress("UNUSED_PARAMETER") tint: Color = MaterialTheme.colorScheme.primary) {
    val labels = listOf(stringResource(R.string.energy_low), stringResource(R.string.energy_mid), stringResource(R.string.energy_high))
    hu.lifeos.app.ui.Segmented(options = listOf(1, 2, 3), selected = selected ?: 0, label = { labels[it - 1] }, onSelect = onSelect)
}

@Composable
fun AgendaRow(it: AgendaItem, onClick: (() -> Unit)? = null) {
    val zone = ZoneId.systemDefault()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(4.dp).height(34.dp).clip(RoundedCornerShape(2.dp)).background(it.color?.let { c -> Color(c) } ?: Noc.cyan))
        Spacer(Modifier.width(12.dp))
        Text(
            if (it.allDay) stringResource(R.string.all_day) else Instant.ofEpochMilli(it.start).atZone(zone).toLocalTime().format(Fmt.time),
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"), color = Noc.text2, modifier = Modifier.width(64.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(it.title, style = MaterialTheme.typography.titleSmall)
            if (it.location.isNotBlank()) Text(it.location, style = MaterialTheme.typography.bodySmall, color = Noc.text2)
        }
    }
}

@Composable
fun TodoLine(t: TodoEntity, today: LocalDate, onToggle: () -> Unit, onClick: (() -> Unit)? = null) {
    val tick = rememberTick()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).let { m -> if (onClick != null) m.clickable(onClick = onClick) else m },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = t.done, onCheckedChange = { tick(); onToggle() }, colors = CheckboxDefaults.colors(checkedColor = Noc.cyan, uncheckedColor = Noc.text3, checkmarkColor = Noc.abyss))
        Column(Modifier.weight(1f)) {
            Text(
                t.title, style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (t.done) TextDecoration.LineThrough else null,
                color = if (t.done) Noc.text3 else Noc.text,
                fontWeight = if (t.important && !t.done) FontWeight.SemiBold else null,
            )
            val due = t.dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            if (due != null || t.dueTime != null) {
                val overdue = !t.done && due != null && due.isBefore(today)
                val label = buildString {
                    if (due != null) append(
                        when (due) {
                            today -> stringResource(R.string.today)
                            today.plusDays(1) -> stringResource(R.string.tomorrow)
                            else -> Fmt.dayShort(due)
                        }
                    )
                    if (t.dueTime != null) { if (isNotEmpty()) append(" · "); append(t.dueTime) }
                }
                Text(label, style = MaterialTheme.typography.bodySmall, color = if (overdue) Noc.coral else Noc.text2)
            }
        }
        if (t.important && !t.done) Box(Modifier.size(7.dp).clip(CircleShape).background(Noc.amber))
    }
}

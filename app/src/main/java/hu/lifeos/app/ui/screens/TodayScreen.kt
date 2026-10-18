package hu.lifeos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.SentimentVeryDissatisfied
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.lifeos.app.R
import hu.lifeos.app.data.TodoEntity
import hu.lifeos.app.satori.SatoriLogic
import hu.lifeos.app.satori.SatoriViewModel
import hu.lifeos.app.ui.AgendaItem
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.CardHeader
import hu.lifeos.app.ui.EmptyHint
import hu.lifeos.app.ui.Fmt
import hu.lifeos.app.ui.MainViewModel
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.iconFor
import hu.lifeos.app.ui.theme.hex
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun TodayScreen(
    vm: MainViewModel,
    satori: SatoriViewModel,
    onOpenSatori: () -> Unit,
    onSos: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenTodos: () -> Unit,
    onOpenSettings: () -> Unit,
    onCapture: () -> Unit,
) {
    val settings by vm.settings.collectAsState()
    val today by vm.today.collectAsState()
    val mood by vm.todayMood.collectAsState()
    val agenda by vm.todayAgenda.collectAsState()
    val todos by vm.todos.collectAsState()
    val st by satori.state.collectAsState()
    val content = satori.content

    val todayStr = today.toString()
    val dueTodos = todos.filter { !it.done && it.dueDate != null && it.dueDate <= todayStr } +
        todos.filter { !it.done && it.dueDate == null && it.important }
    val doneToday = todos.count { it.done && it.doneAt != null && Instant.ofEpochMilli(it.doneAt).atZone(ZoneId.systemDefault()).toLocalDate() == today }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    val h = LocalTime.now().hour
                    val greet = stringResource(
                        when (h) { in 5..9 -> R.string.greet_morning; in 10..17 -> R.string.greet_day; else -> R.string.greet_evening }
                    )
                    val name = settings?.name.orEmpty()
                    Text(if (name.isBlank()) greet else "$greet, $name", style = MaterialTheme.typography.headlineMedium)
                    Text(Fmt.dayLong(today), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onOpenSettings) { Icon(Icons.Outlined.Settings, stringResource(R.string.settings)) }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(onClick = onCapture).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Edit, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.capture_prompt), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ---------- Satori kártya ----------
        item {
            val d = st.cur()
            val theme = content.theme(st.theme)
            val accent = hex(theme.colors.tertiary)
            val idx = SatoriLogic.stageIndex(content, d.streak)
            val nIdx = SatoriLogic.nextStageIndex(content, d.streak)
            val frac = SatoriLogic.progressFraction(content, d.streak)
            val closed = d.dayOutcomeDate == todayStr
            val type = content.type(st.type)
            AppCard(onClick = onOpenSatori, color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(iconFor(type.icon), null, tint = accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Satori · ${type.label}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(SatoriLogic.stageTitle(content, st, idx, stringResource(R.string.satori_restarter)), style = MaterialTheme.typography.labelLarge, color = accent)
                }
                VSpace(8)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${d.streak}", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.satori_clean_days), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                }
                LinearProgressIndicator(
                    progress = { frac }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = accent, trackColor = MaterialTheme.colorScheme.surfaceContainerHighest, strokeCap = StrokeCap.Round, gapSize = 0.dp,
                    drawStopIndicator = {},
                )
                if (nIdx != idx) {
                    VSpace(4)
                    Text(stringResource(R.string.satori_days_to_next, content.stages[nIdx].days - d.streak), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                VSpace(12)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (closed) {
                        OutlinedButton(onClick = onOpenSatori, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.satori_closed_today))
                        }
                    } else {
                        Button(onClick = onOpenSatori, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF241A00))) {
                            Text(stringResource(R.string.satori_close_day))
                        }
                    }
                    FilledTonalButton(onClick = onSos, colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) {
                        Text(stringResource(R.string.sos))
                    }
                }
            }
        }

        // ---------- Hangulat ----------
        item {
            AppCard {
                CardHeader(Icons.Outlined.Mood, stringResource(R.string.mood_title))
                VSpace(10)
                MoodRow(selected = mood?.mood, onSelect = { vm.setMood(it, null) })
                VSpace(10)
                Text(stringResource(R.string.energy), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                VSpace(6)
                EnergyRow(selected = mood?.energy, onSelect = { vm.setMood(null, it) })
            }
        }

        // ---------- Mai naptár ----------
        item {
            AppCard(onClick = onOpenCalendar) {
                CardHeader(Icons.Outlined.CalendarToday, stringResource(R.string.today_agenda))
                VSpace(8)
                if (agenda.isEmpty()) EmptyHint(stringResource(R.string.today_agenda_empty))
                agenda.take(8).forEach { AgendaRow(it) }
            }
        }

        // ---------- Teendők ----------
        item {
            AppCard {
                CardHeader(Icons.Outlined.CheckCircle, stringResource(R.string.today_todos)) {
                    if (doneToday > 0) Text(stringResource(R.string.done_today_count, doneToday), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                VSpace(4)
                if (dueTodos.isEmpty()) EmptyHint(stringResource(R.string.today_todos_empty))
                dueTodos.take(10).forEach { t -> TodoLine(t, today, onToggle = { vm.toggleTodo(t) }) }
                TextButton(onClick = onOpenTodos) { Text(stringResource(R.string.all_todos)) }
            }
        }

        // ---------- Napi gondolat ----------
        item {
            val quotes = SatoriLogic.themeQuotes(content, st)
            if (quotes.isNotEmpty()) AppCard {
                CardHeader(Icons.Outlined.FormatQuote, stringResource(R.string.daily_quote))
                VSpace(8)
                Text("„${quotes[SatoriLogic.dayIndex(quotes.size)]}”", style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
            }
        }
    }
}

@Composable
fun MoodRow(selected: Int?, onSelect: (Int) -> Unit, tint: Color = MaterialTheme.colorScheme.primary) {
    val icons = listOf(
        Icons.Outlined.SentimentVeryDissatisfied, Icons.Outlined.SentimentDissatisfied, Icons.Outlined.SentimentNeutral,
        Icons.Outlined.SentimentSatisfied, Icons.Outlined.SentimentVerySatisfied,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        icons.forEachIndexed { i, ic ->
            val v = i + 1
            val on = selected == v
            Box(
                Modifier.size(52.dp).clip(CircleShape)
                    .background(if (on) tint.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onSelect(v) },
                contentAlignment = Alignment.Center,
            ) { Icon(ic, null, tint = if (on) tint else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp)) }
        }
    }
}

@Composable
fun EnergyRow(selected: Int?, onSelect: (Int) -> Unit, tint: Color = MaterialTheme.colorScheme.primary) {
    val labels = listOf(R.string.energy_low, R.string.energy_mid, R.string.energy_high)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { i, l ->
            val v = i + 1
            val on = selected == v
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .background(if (on) tint.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onSelect(v) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(l), color = if (on) tint else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
fun AgendaRow(it: AgendaItem, onClick: (() -> Unit)? = null) {
    val zone = ZoneId.systemDefault()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(it.color?.let { c -> Color(c) } ?: MaterialTheme.colorScheme.primary))
        Spacer(Modifier.width(12.dp))
        Text(
            if (it.allDay) stringResource(R.string.all_day) else Instant.ofEpochMilli(it.start).atZone(zone).toLocalTime().format(Fmt.time),
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(64.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(it.title, style = MaterialTheme.typography.bodyLarge)
            if (it.location.isNotBlank()) Text(it.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TodoLine(t: TodoEntity, today: LocalDate, onToggle: () -> Unit, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).let { m -> if (onClick != null) m.clickable(onClick = onClick) else m },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = t.done, onCheckedChange = { onToggle() })
        Column(Modifier.weight(1f)) {
            Text(
                t.title, style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (t.done) TextDecoration.LineThrough else null,
                color = if (t.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
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
                Text(label, style = MaterialTheme.typography.bodySmall, color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

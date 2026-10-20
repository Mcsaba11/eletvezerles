package hu.lifeos.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.EmptyHint
import hu.lifeos.app.ui.Fmt
import hu.lifeos.app.ui.MainViewModel
import hu.lifeos.app.ui.VSpace
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun CalendarScreen(vm: MainViewModel, onNewEvent: (LocalDate) -> Unit, onEditEvent: (Long) -> Unit, onOpenSettings: () -> Unit, onBack: () -> Unit) {
    val month by vm.month.collectAsState()
    val selected by vm.selectedDay.collectAsState()
    val agenda by vm.monthAgenda.collectAsState()
    val settings by vm.settings.collectAsState()
    val todos by vm.todos.collectAsState()
    val today = LocalDate.now()

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res[Manifest.permission.READ_CALENDAR] == true) vm.updateSettings { it.copy(showPhoneCalendar = true) }
    }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back)) }
                Text(Fmt.monthYear(month), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.shiftMonth(-1) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null) }
                TextButton(onClick = { vm.selectDay(today) }) { Text(stringResource(R.string.today)) }
                IconButton(onClick = { vm.shiftMonth(1) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) }
            }
        }
        item {
            AppCard(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(Modifier.fillMaxWidth()) {
                    DayOfWeek.values().forEach { dow ->
                        Text(
                            Fmt.weekdayShort(dow), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
                VSpace(6)
                val first = month
                val offset = first.dayOfWeek.value - 1
                val daysInMonth = first.lengthOfMonth()
                val cells = offset + daysInMonth
                val rows = (cells + 6) / 7
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val dayNum = r * 7 + col - offset + 1
                            Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                                if (dayNum in 1..daysInMonth) {
                                    val d = first.withDayOfMonth(dayNum)
                                    val items = vm.itemsOn(agenda, d)
                                    val hasTodo = todos.any { !it.done && it.dueDate == d.toString() }
                                    val isSel = d == selected
                                    val isToday = d == today
                                    Column(
                                        Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                                            .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                            .let { if (isToday && !isSel) it.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)) else it }
                                            .clickable { vm.selectDay(d) },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Text(
                                            "$dayNum", style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isToday) FontWeight.Bold else null,
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(6.dp)) {
                                            items.take(3).forEach { ev ->
                                                Box(Modifier.size(5.dp).clip(CircleShape).background(ev.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary))
                                            }
                                            if (hasTodo) Box(Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Fmt.dayLong(selected), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { onNewEvent(selected) }) { Icon(Icons.Outlined.Add, stringResource(R.string.new_event)) }
            }
        }
        item {
            val dayItems = vm.itemsOn(agenda, selected)
            val dayTodos = todos.filter { it.dueDate == selected.toString() }
            AppCard {
                if (dayItems.isEmpty() && dayTodos.isEmpty()) EmptyHint(stringResource(R.string.day_empty))
                dayItems.forEach { item -> AgendaRow(item, onClick = item.own?.let { o -> { onEditEvent(o.id) } }) }
                dayTodos.forEach { t -> TodoLine(t, today, onToggle = { vm.toggleTodo(t) }) }
            }
        }
        if (settings?.showPhoneCalendar != true) item {
            AppCard(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Sync, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.phone_cal_title), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.phone_cal_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                VSpace(8)
                Row {
                    TextButton(onClick = {
                        permLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
                    }) { Text(stringResource(R.string.phone_cal_connect)) }
                    TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.settings)) }
                }
            }
        }
    }
}

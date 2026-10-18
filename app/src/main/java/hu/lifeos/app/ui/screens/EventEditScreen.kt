package hu.lifeos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import hu.lifeos.app.data.EventEntity
import hu.lifeos.app.ui.DatePickDialog
import hu.lifeos.app.ui.Fmt
import hu.lifeos.app.ui.MainViewModel
import hu.lifeos.app.ui.SectionLabel
import hu.lifeos.app.ui.TimePickDialog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditScreen(vm: MainViewModel, id: Long, date: String, onBack: () -> Unit) {
    val zone = ZoneId.systemDefault()
    var existing by remember { mutableStateOf<EventEntity?>(null) }
    var title by remember { mutableStateOf("") }
    var day by remember { mutableStateOf(runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())) }
    var allDay by remember { mutableStateOf(false) }
    var start by remember { mutableStateOf(LocalTime.now().plusHours(1).withMinute(0)) }
    var end by remember { mutableStateOf(start.plusHours(1)) }
    var location by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var pickDate by remember { mutableStateOf(false) }
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }
    val settings by vm.settings.collectAsState()
    val calendars by vm.phoneCalendars.collectAsState()
    val writable = calendars.filter { it.writable }
    var target by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(settings?.defaultWriteCalendarId, settings?.showPhoneCalendar) {
        if (settings?.showPhoneCalendar == true) target = settings?.defaultWriteCalendarId
    }

    LaunchedEffect(id) {
        if (id > 0) vm.loadEvent(id)?.let { e ->
            existing = e
            title = e.title; allDay = e.allDay; location = e.location; note = e.note
            val s = Instant.ofEpochMilli(e.startMillis).atZone(zone)
            day = s.toLocalDate(); start = s.toLocalTime()
            end = Instant.ofEpochMilli(e.endMillis).atZone(zone).toLocalTime()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (existing == null) R.string.new_event else R.string.edit_event)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back)) } },
                actions = {
                    existing?.let { e -> IconButton(onClick = { vm.deleteEvent(e); onBack() }) { Icon(Icons.Outlined.Delete, stringResource(R.string.delete)) } }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.field_title)) }, singleLine = true)
            OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth()) { Text(Fmt.dayLong(day)) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.all_day), modifier = Modifier.weight(1f))
                Switch(allDay, { allDay = it })
            }
            if (!allDay) Row {
                OutlinedButton(onClick = { pickStart = true }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.from_time, start.format(Fmt.time))) }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { pickEnd = true }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.to_time, end.format(Fmt.time))) }
            }
            OutlinedTextField(location, { location = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.field_location)) }, singleLine = true)
            OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.field_note)) }, minLines = 3)

            if (existing == null && settings?.showPhoneCalendar == true && writable.isNotEmpty()) {
                SectionLabel(stringResource(R.string.save_to))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(target == null, { target = null }, label = { Text(stringResource(R.string.own_calendar)) })
                }
                writable.forEach { cal ->
                    FilterChip(target == cal.id, { target = cal.id }, label = { Text(cal.name.ifBlank { cal.account }) })
                }
            }

            Button(
                onClick = {
                    vm.saveEvent(existing, title, day, if (allDay) null else start, if (allDay) null else end, location, note, target) { onBack() }
                },
                enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.save)) }
            if (existing == null && target != null) {
                Text(stringResource(R.string.phone_cal_save_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (pickDate) DatePickDialog(day, { pickDate = false }) { day = it }
    if (pickStart) TimePickDialog(start, { pickStart = false }) { start = it; if (!end.isAfter(it)) end = it.plusHours(1) }
    if (pickEnd) TimePickDialog(end, { pickEnd = false }) { end = it }
}

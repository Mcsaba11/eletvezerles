package hu.lifeos.app.mods.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.mods.Contact
import hu.lifeos.app.mods.ModsViewModel
import hu.lifeos.app.mods.Person
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.DatePickDialog
import hu.lifeos.app.ui.EmptyHint
import hu.lifeos.app.ui.Fmt
import hu.lifeos.app.ui.PrimaryButton
import hu.lifeos.app.ui.SectionLabel
import hu.lifeos.app.ui.TonalButton
import hu.lifeos.app.ui.theme.Noc
import java.time.LocalDate

private val RELATIONS = listOf("family", "partner", "friend", "colleague", "other")
private val FREQS = listOf(7, 14, 30, 90, 0)
private val CONTACT_KINDS = listOf("call", "message", "meet")

@Composable
private fun relationName(id: String) = stringResource(
    when (id) { "family" -> R.string.rel_family; "partner" -> R.string.rel_partner; "friend" -> R.string.rel_friend; "colleague" -> R.string.rel_colleague; else -> R.string.rel_other }
)

@Composable
private fun freqName(n: Int) = when (n) {
    7 -> stringResource(R.string.freq_week); 14 -> stringResource(R.string.freq_2week); 30 -> stringResource(R.string.freq_month)
    90 -> stringResource(R.string.freq_quarter); 0 -> stringResource(R.string.freq_none); else -> stringResource(R.string.freq_n, n)
}

@Composable
private fun contactName(k: String) = stringResource(when (k) { "call" -> R.string.ct_call; "message" -> R.string.ct_message; else -> R.string.ct_meet })

@Composable
fun dueText(p: Person): String {
    val due = p.dueIn() ?: return stringResource(R.string.freq_none)
    return when {
        p.last() == null -> stringResource(R.string.people_never)
        due < 0 -> stringResource(R.string.people_overdue, -due)
        due == 0 -> stringResource(R.string.people_due_today)
        else -> stringResource(R.string.people_due_in, due)
    }
}

@Composable
fun PeopleScreen(vm: ModsViewModel, onBack: () -> Unit) {
    val d by vm.people.state.collectAsState()
    var add by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf<String?>(null) }
    ModScaffold(stringResource(R.string.mod_people), stringResource(R.string.people_kicker), Noc.coral, onBack, onAdd = { add = true }) {
        val due = d.people.filter { (it.dueIn() ?: 1) <= 0 }.sortedBy { it.dueIn() }
        val bdays = d.people.filter { (it.birthdayIn() ?: 99) <= 30 }.sortedBy { it.birthdayIn() }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = ModListPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text(stringResource(R.string.people_sub), style = MaterialTheme.typography.bodyMedium, color = Noc.text2) }
            if (d.people.isEmpty()) item { EmptyHint(stringResource(R.string.people_empty)) }
            if (bdays.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.people_birthdays)) }
                items(bdays, key = { "b" + it.id }) { p ->
                    val n = p.birthdayIn() ?: 0
                    AppCard(onClick = { open = p.id }, glow = Noc.amber, padding = PaddingValues(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Outlined.Cake, Noc.amber, 34)
                            Spacer(Modifier.width(10.dp))
                            Text(p.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(if (n == 0) stringResource(R.string.people_bday_today) else stringResource(R.string.people_due_in, n), style = MaterialTheme.typography.labelLarge, color = Noc.amber)
                        }
                    }
                }
            }
            if (due.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.people_due)) }
                items(due, key = { "d" + it.id }) { p -> PersonRow(p, highlight = true) { open = p.id } }
            }
            if (d.people.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.people_all)) }
                items(d.people.sortedBy { it.name.lowercase() }, key = { it.id }) { p -> PersonRow(p, highlight = false) { open = p.id } }
            }
        }
    }
    if (add) PersonEditSheet(vm, null, onDismiss = { add = false })
    open?.let { id -> d.people.firstOrNull { it.id == id }?.let { p -> PersonSheet(vm, p, onDismiss = { open = null }) } }
}

@Composable
private fun PersonRow(p: Person, highlight: Boolean, onClick: () -> Unit) {
    AppCard(onClick = onClick, padding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Outlined.Person, Noc.coral, 34)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.bodyLarge)
                Text(relationName(p.relation) + " · " + freqName(p.every), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
            }
            Text(dueText(p), style = MaterialTheme.typography.labelMedium, color = if (highlight) Noc.coral else Noc.text2)
        }
    }
}

@Composable
private fun PersonSheet(vm: ModsViewModel, p: Person, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var kind by remember { mutableStateOf("call") }
    var note by remember { mutableStateOf("") }
    var edit by remember { mutableStateOf(false) }
    if (edit) { PersonEditSheet(vm, p, onDismiss = { edit = false; onDismiss() }); return }
    FormSheet(p.name, onDismiss) {
        Text(relationName(p.relation) + " · " + freqName(p.every) + " · " + dueText(p), style = MaterialTheme.typography.bodyMedium, color = Noc.text2)
        if (p.note.isNotBlank()) Text(p.note, style = MaterialTheme.typography.bodyMedium)
        if (p.phone.isNotBlank()) TonalButton(stringResource(R.string.people_call, p.phone), onClick = {
            runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + p.phone)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }, leading = Icons.Outlined.Call, tint = Noc.coral)
        SectionLabel(stringResource(R.string.people_log))
        ChipRow(CONTACT_KINDS, kind, { contactName(it) }, Noc.coral) { kind = it }
        TextIn(note, { note = it }, stringResource(R.string.people_log_note))
        PrimaryButton(stringResource(R.string.people_log_save), onClick = {
            val c = Contact(LocalDate.now().toString(), kind, note.trim())
            vm.people.update { d -> d.copy(people = d.people.map { if (it.id == p.id) it.copy(contacts = it.contacts + c) else it }) }
            onDismiss()
        }, modifier = Modifier.fillMaxWidth(), color = Noc.coral)
        if (p.contacts.isNotEmpty()) {
            SectionLabel(stringResource(R.string.people_history))
            p.contacts.sortedByDescending { it.date }.take(10).forEach { c ->
                Text(
                    runCatching { Fmt.dayShort(LocalDate.parse(c.date)) }.getOrDefault(c.date) + " · " + contactName(c.kind) + if (c.note.isNotBlank()) " · ${c.note}" else "",
                    style = MaterialTheme.typography.bodySmall, color = Noc.text2,
                )
            }
        }
        Row {
            TextButton(onClick = { edit = true }) { Text(stringResource(R.string.edit)) }
            TextButton(onClick = { vm.people.update { d -> d.copy(people = d.people.filter { it.id != p.id }) }; onDismiss() }) {
                Text(stringResource(R.string.delete), color = Noc.coral)
            }
        }
    }
}

@Composable
private fun PersonEditSheet(vm: ModsViewModel, p: Person?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(p?.name ?: "") }
    var rel by remember { mutableStateOf(p?.relation ?: "friend") }
    var every by remember { mutableStateOf(p?.every ?: 14) }
    var phone by remember { mutableStateOf(p?.phone ?: "") }
    var bday by remember { mutableStateOf(p?.birthday ?: "") }
    var note by remember { mutableStateOf(p?.note ?: "") }
    var pick by remember { mutableStateOf(false) }
    FormSheet(stringResource(if (p == null) R.string.people_add else R.string.edit), onDismiss) {
        TextIn(name, { name = it }, stringResource(R.string.people_name))
        Text(stringResource(R.string.people_relation), style = MaterialTheme.typography.labelLarge, color = Noc.text2)
        ChipRow(RELATIONS, rel, { relationName(it) }, Noc.coral) { rel = it }
        Text(stringResource(R.string.people_every), style = MaterialTheme.typography.labelLarge, color = Noc.text2)
        ChipRow(FREQS, every, { freqName(it) }, Noc.coral) { every = it }
        NumIn(phone, { phone = it }, stringResource(R.string.people_phone))
        TonalButton(
            if (bday.isBlank()) stringResource(R.string.people_bday_set) else stringResource(R.string.people_bday, bday.replace("-", ". ") + "."),
            onClick = { pick = true },
        )
        TextIn(note, { note = it }, stringResource(R.string.field_note), lines = 2, placeholder = stringResource(R.string.people_note_hint))
        PrimaryButton(stringResource(R.string.save), onClick = {
            val item = (p ?: Person(name = "")).copy(name = name.trim(), relation = rel, every = every, phone = phone.trim(), birthday = bday, note = note.trim())
            vm.people.update { d -> d.copy(people = if (p == null) d.people + item else d.people.map { if (it.id == p.id) item else it }) }
            onDismiss()
        }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth(), color = Noc.coral)
    }
    if (pick) DatePickDialog(LocalDate.now(), { pick = false }) { bday = "%02d-%02d".format(it.monthValue, it.dayOfMonth) }
}

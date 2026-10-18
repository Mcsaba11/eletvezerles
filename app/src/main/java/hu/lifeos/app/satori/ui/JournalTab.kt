package hu.lifeos.app.satori.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.satori.SatoriViewModel
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.CardHeader
import hu.lifeos.app.ui.EmptyHint
import hu.lifeos.app.ui.Pill
import hu.lifeos.app.ui.SectionLabel
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.iconFor
import kotlinx.coroutines.delay

@Composable
fun JournalTab(vm: SatoriViewModel, modifier: Modifier) {
    val st by vm.state.collectAsState()
    val c = vm.content
    val d = st.cur()
    val th = c.theme(st.theme)
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var note by rememberSaveable { mutableStateOf("") }
    var stamped by remember { mutableStateOf(false) }
    var reason by rememberSaveable { mutableStateOf("") }
    val stateLabels = c.stateDefs.associate { it.id to it.label }

    if (stamped) LaunchedEffect(Unit) { delay(2500); stamped = false }

    LazyColumn(
        modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.satori_daily_presence).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    Text(th.journalTitle, style = MaterialTheme.typography.headlineSmall)
                }
                Pill(stringResource(R.string.satori_nth_day, d.streak))
            }
        }
        item {
            AppCard {
                Text(stringResource(R.string.satori_how_feel), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.satori_journal_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                VSpace(10)
                c.stateDefs.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                        pair.forEach { sd ->
                            val on = selected == sd.id
                            Row(
                                Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .let { if (on) it.border(2.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(14.dp)) else it }
                                    .clickable { selected = sd.id }.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                                    Icon(iconFor(sd.icon), null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                }
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(sd.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                    Text(sd.sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), placeholder = { Text(stringResource(R.string.satori_journal_placeholder)) }, minLines = 2)
                VSpace(10)
                Button(
                    onClick = { selected?.let { vm.addJournal(it, note) }; note = ""; selected = null; stamped = true },
                    enabled = selected != null, modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Verified, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.satori_stamp))
                }
                if (stamped) {
                    VSpace(6)
                    Text(stringResource(R.string.satori_stamped), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                    val msgs = c.messages[d.journal.firstOrNull()?.state ?: "nyugodt"].orEmpty()
                    if (msgs.isNotEmpty()) Text(msgs[d.journal.size % msgs.size], style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            AppCard {
                CardHeader(Icons.Outlined.Favorite, stringResource(R.string.satori_reasons))
                Text(stringResource(R.string.satori_reasons_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                VSpace(8)
                if (d.reasons.isEmpty()) EmptyHint(stringResource(R.string.satori_no_reasons))
                d.reasons.forEachIndexed { i, r ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(r, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { vm.removeReason(i) }) { Icon(Icons.Outlined.Close, stringResource(R.string.delete), Modifier.size(18.dp)) }
                    }
                }
                VSpace(6)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(reason, { reason = it }, Modifier.weight(1f), placeholder = { Text(stringResource(R.string.satori_new_reason)) }, singleLine = true)
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { vm.addReason(reason); reason = "" }, enabled = reason.isNotBlank()) { Text(stringResource(R.string.add)) }
                }
            }
        }
        item { SectionLabel(stringResource(R.string.satori_journal_list)) }
        if (d.journal.isEmpty()) item { EmptyHint(stringResource(R.string.satori_no_entries)) }
        items(d.journal.take(60)) { e ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .let { if (e.source == "dayclose") it.border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f), RoundedCornerShape(14.dp)) else it }
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.History, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(" ${e.date} · ${e.time}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    if (e.source == "dayclose") {
                        Pill(stringResource(R.string.satori_dayclose), color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f), textColor = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(4.dp))
                    }
                    Pill(stateLabels[e.state] ?: e.state, color = MaterialTheme.colorScheme.surfaceContainerHighest, textColor = MaterialTheme.colorScheme.secondary)
                }
                if (e.note.isNotBlank()) Text(e.note, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}


package hu.lifeos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.data.EntryEntity
import hu.lifeos.app.data.EntryKind
import hu.lifeos.app.satori.SatoriViewModel
import hu.lifeos.app.ui.EmptyHint
import hu.lifeos.app.ui.Fmt
import hu.lifeos.app.ui.MainViewModel
import hu.lifeos.app.ui.SectionLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private data class Line(
    val sortKey: String,
    val date: LocalDate,
    val time: String,
    val icon: ImageVector,
    val title: String,
    val sub: String?,
    val filter: String,
    val entry: EntryEntity? = null,
)

@Composable
fun TimelineScreen(vm: MainViewModel, satori: SatoriViewModel) {
    val entries by vm.recentEntries.collectAsState()
    val moods by vm.moodHistory.collectAsState()
    val todos by vm.todos.collectAsState()
    val st by satori.state.collectAsState()
    val content = satori.content
    var filter by rememberSaveable { mutableStateOf("all") }
    var toDelete by remember { mutableStateOf<EntryEntity?>(null) }
    val zone = ZoneId.systemDefault()

    val moodWords = listOf(R.string.mood_1, R.string.mood_2, R.string.mood_3, R.string.mood_4, R.string.mood_5).map { stringResource(it) }
    val energyWords = listOf(R.string.energy_low, R.string.energy_mid, R.string.energy_high).map { stringResource(it) }
    val energyLabel = stringResource(R.string.energy)
    val doneLabel = stringResource(R.string.todo_done)
    val stateLabels = content.stateDefs.associate { it.id to it.label }
    val dayCloseLabel = stringResource(R.string.satori_dayclose)

    val lines = buildList {
        entries.forEach { e ->
            val t = Instant.ofEpochMilli(e.ts).atZone(zone)
            val (icon, f) = when (e.kind) {
                EntryKind.EXPENSE -> Icons.Outlined.AttachMoney to "money"
                EntryKind.INCOME -> Icons.Outlined.AccountBalanceWallet to "money"
                else -> Icons.Outlined.Description to "note"
            }
            val title = when (e.kind) {
                EntryKind.EXPENSE -> "− " + Fmt.money(e.amount ?: 0.0) + if (e.text.isNotBlank()) " · ${e.text}" else ""
                EntryKind.INCOME -> "+ " + Fmt.money(e.amount ?: 0.0) + if (e.text.isNotBlank()) " · ${e.text}" else ""
                else -> e.text
            }
            add(Line("${t.toLocalDate()} ${t.toLocalTime()}", t.toLocalDate(), t.toLocalTime().format(Fmt.time), icon, title, null, f, e))
        }
        moods.forEach { m ->
            val d = runCatching { LocalDate.parse(m.date) }.getOrNull() ?: return@forEach
            val parts = listOfNotNull(m.mood?.let { moodWords.getOrNull(it - 1) }, m.energy?.let { "$energyLabel: ${energyWords.getOrNull(it - 1)}" })
            if (parts.isNotEmpty()) add(Line("${m.date} 99", d, "", Icons.Outlined.Mood, parts.joinToString(" · "), null, "mood"))
        }
        todos.filter { it.done && it.doneAt != null }.forEach { td ->
            val t = Instant.ofEpochMilli(td.doneAt!!).atZone(zone)
            add(Line("${t.toLocalDate()} ${t.toLocalTime()}", t.toLocalDate(), t.toLocalTime().format(Fmt.time), Icons.Outlined.CheckCircle, td.title, doneLabel, "todo"))
        }
        st.data.forEach { (typeId, td) ->
            val typeLabel = content.type(typeId).label
            td.journal.take(200).forEach { j ->
                val d = runCatching { LocalDate.parse(j.date) }.getOrNull() ?: return@forEach
                val head = (if (j.source == "dayclose") "$dayCloseLabel · " else "") + (stateLabels[j.state] ?: j.state)
                add(Line("${j.date} ${j.time}", d, j.time, Icons.Outlined.Spa, head + if (j.note.isNotBlank()) " — ${j.note}" else "", "Satori · $typeLabel", "satori"))
            }
        }
    }.filter { filter == "all" || it.filter == filter }.sortedByDescending { it.sortKey }

    val grouped = lines.groupBy { it.date }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item { Text(stringResource(R.string.tab_timeline), style = MaterialTheme.typography.headlineMedium) }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "all" to R.string.filter_all, "note" to R.string.type_note, "money" to R.string.filter_money,
                    "todo" to R.string.tab_todos, "mood" to R.string.type_mood, "satori" to R.string.satori_name,
                ).forEach { (id, l) -> FilterChip(filter == id, { filter = id }, label = { Text(stringResource(l)) }) }
            }
        }
        if (lines.isEmpty()) item { EmptyHint(stringResource(R.string.timeline_empty)) }
        grouped.forEach { (date, list) ->
            item(key = "h$date") { SectionLabel(Fmt.dayLong(date)) }
            items(list) { l ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .let { m -> if (l.entry != null) m.clickable { toDelete = l.entry } else m }
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                        Icon(l.icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(l.title, style = MaterialTheme.typography.bodyLarge)
                        val meta = listOfNotNull(l.time.ifBlank { null }, l.sub).joinToString(" · ")
                        if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    toDelete?.let { e ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.delete_entry_q)) },
            text = { Text(e.text.ifBlank { Fmt.money(e.amount ?: 0.0) }) },
            confirmButton = { TextButton(onClick = { vm.deleteEntry(e); toDelete = null }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

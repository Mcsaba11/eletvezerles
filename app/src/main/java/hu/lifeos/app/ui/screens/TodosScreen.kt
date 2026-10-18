package hu.lifeos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.data.TodoEntity
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.DatePickDialog
import hu.lifeos.app.ui.EmptyHint
import hu.lifeos.app.ui.Fmt
import hu.lifeos.app.ui.MainViewModel
import hu.lifeos.app.ui.SectionLabel
import hu.lifeos.app.ui.VSpace
import java.time.LocalDate

@Composable
fun TodosScreen(vm: MainViewModel) {
    val todos by vm.todos.collectAsState()
    val today = LocalDate.now()
    var text by rememberSaveable { mutableStateOf("") }
    var due by remember { mutableStateOf<LocalDate?>(null) }
    var important by rememberSaveable { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TodoEntity?>(null) }
    var showDone by rememberSaveable { mutableStateOf(false) }

    fun add() {
        if (text.isBlank()) return
        vm.addTodo(text, due, null, important)
        text = ""; important = false
    }

    val open = todos.filter { !it.done }
    val t = today.toString(); val tm = today.plusDays(1).toString()
    val groups = listOf(
        R.string.group_overdue to open.filter { it.dueDate != null && it.dueDate < t },
        R.string.today to open.filter { it.dueDate == t },
        R.string.tomorrow to open.filter { it.dueDate == tm },
        R.string.group_later to open.filter { it.dueDate != null && it.dueDate > tm },
        R.string.group_nodate to open.filter { it.dueDate == null },
    )
    val done = todos.filter { it.done }.sortedByDescending { it.doneAt ?: 0 }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item { Text(stringResource(R.string.tab_todos), style = MaterialTheme.typography.headlineMedium) }
        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        text, { text = it }, Modifier.weight(1f), placeholder = { Text(stringResource(R.string.new_todo)) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { add() }),
                    )
                    IconButton(onClick = { add() }, enabled = text.isNotBlank()) { Icon(Icons.Outlined.Add, stringResource(R.string.add)) }
                }
                VSpace(8)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(due == null, { due = null }, label = { Text(stringResource(R.string.no_date)) })
                    FilterChip(due == today, { due = today }, label = { Text(stringResource(R.string.today)) })
                    FilterChip(due == today.plusDays(1), { due = today.plusDays(1) }, label = { Text(stringResource(R.string.tomorrow)) })
                    FilterChip(due != null && due!! > today.plusDays(1), { pickDate = true },
                        label = { Text(due?.takeIf { it > today.plusDays(1) }?.let { Fmt.dayShort(it) } ?: "…") })
                    IconButton(onClick = { important = !important }) {
                        Icon(if (important) Icons.Outlined.Star else Icons.Outlined.StarBorder, stringResource(R.string.important),
                            tint = if (important) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        groups.forEach { (label, list) ->
            if (list.isNotEmpty()) {
                item { SectionLabel(stringResource(label)) }
                items(list, key = { it.id }) { td ->
                    AppCard(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        TodoLine(td, today, onToggle = { vm.toggleTodo(td) }, onClick = { editing = td })
                    }
                }
            }
        }
        if (open.isEmpty()) item { EmptyHint(stringResource(R.string.todos_all_done)) }
        if (done.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showDone = !showDone }) { Text(stringResource(R.string.done_count, done.size)) }
                    if (showDone) TextButton(onClick = { vm.clearDoneTodos() }) { Text(stringResource(R.string.clear_done)) }
                }
            }
            if (showDone) items(done.take(50), key = { it.id }) { td ->
                TodoLine(td, today, onToggle = { vm.toggleTodo(td) })
            }
        }
    }

    if (pickDate) DatePickDialog(due ?: today.plusDays(2), { pickDate = false }) { due = it }

    editing?.let { td -> TodoEditDialog(td, onDismiss = { editing = null }, onSave = { vm.updateTodo(it); editing = null }, onDelete = { vm.deleteTodo(td); editing = null }) }
}

@Composable
fun TodoEditDialog(td: TodoEntity, onDismiss: () -> Unit, onSave: (TodoEntity) -> Unit, onDelete: () -> Unit) {
    var title by remember { mutableStateOf(td.title) }
    var note by remember { mutableStateOf(td.note) }
    var due by remember { mutableStateOf(td.dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }) }
    var important by remember { mutableStateOf(td.important) }
    var pick by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_todo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.field_title)) })
                OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.field_note)) }, minLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { pick = true }) { Text(due?.let { Fmt.dayLong(it) } ?: stringResource(R.string.no_date)) }
                    if (due != null) TextButton(onClick = { due = null }) { Text(stringResource(R.string.remove_date)) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { important = !important }) {
                        Icon(if (important) Icons.Outlined.Star else Icons.Outlined.StarBorder, null,
                            tint = if (important) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(stringResource(R.string.important))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(td.copy(title = title.trim(), note = note.trim(), dueDate = due?.toString(), important = important)) }, enabled = title.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDelete) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) } },
    )
    if (pick) DatePickDialog(due ?: LocalDate.now(), { pick = false }) { due = it }
}

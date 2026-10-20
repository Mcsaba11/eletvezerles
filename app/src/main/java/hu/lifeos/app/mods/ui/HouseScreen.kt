package hu.lifeos.app.mods.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.mods.Chore
import hu.lifeos.app.mods.ModsViewModel
import hu.lifeos.app.mods.ShopItem
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.EmptyHint
import hu.lifeos.app.ui.PrimaryButton
import hu.lifeos.app.ui.rememberThump
import hu.lifeos.app.ui.theme.Noc
import java.time.LocalDate

@Composable
fun choreDue(c: Chore): String {
    val n = c.dueIn()
    return when {
        c.last == null -> stringResource(R.string.house_never)
        n < 0 -> stringResource(R.string.people_overdue, -n)
        n == 0 -> stringResource(R.string.people_due_today)
        else -> stringResource(R.string.people_due_in, n)
    }
}

@Composable
fun HouseScreen(vm: ModsViewModel, onBack: () -> Unit) {
    val d by vm.house.state.collectAsState()
    var addChore by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf<Chore?>(null) }
    val thump = rememberThump()
    val tabs = listOf(stringResource(R.string.house_chores), stringResource(R.string.house_shopping))
    ModScaffold(stringResource(R.string.mod_house), stringResource(R.string.house_kicker), Noc.cyan, onBack, tabs,
        onAdd = { p -> if (p == 0) addChore = true }) { page ->
        if (page == 0) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = ModListPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text(stringResource(R.string.house_chores_sub), style = MaterialTheme.typography.bodyMedium, color = Noc.text2) }
                if (d.chores.isEmpty()) item { EmptyHint(stringResource(R.string.house_chores_empty)) }
                items(d.chores.sortedBy { it.dueIn() }, key = { it.id }) { c ->
                    val due = c.dueIn() <= 0
                    AppCard(onClick = { edit = c }, padding = PaddingValues(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(c.last == LocalDate.now().toString(), { on ->
                                thump()
                                vm.house.update { h -> h.copy(chores = h.chores.map { if (it.id == c.id) it.copy(last = if (on) LocalDate.now().toString() else null) else it }) }
                            }, colors = CheckboxDefaults.colors(checkedColor = Noc.cyan, checkmarkColor = Noc.abyss, uncheckedColor = Noc.text3))
                            Column(Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.bodyLarge)
                                Text((if (c.room.isNotBlank()) c.room + " · " else "") + stringResource(R.string.freq_n, c.every), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                            }
                            Text(choreDue(c), style = MaterialTheme.typography.labelMedium, color = if (due) Noc.coral else Noc.text2)
                        }
                    }
                }
            }
        } else ShoppingPage(vm)
    }
    if (addChore || edit != null) ChoreSheet(vm, edit, onDismiss = { addChore = false; edit = null })
}

@Composable
private fun ShoppingPage(vm: ModsViewModel) {
    val d by vm.house.state.collectAsState()
    var text by remember { mutableStateOf("") }
    val thump = rememberThump()
    fun add() {
        val items = text.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() }.map { ShopItem(text = it) }
        if (items.isNotEmpty()) vm.house.update { it.copy(shopping = it.shopping + items) }
        text = ""
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ModListPadding, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    text, { text = it }, Modifier.weight(1f), singleLine = true, placeholder = { Text(stringResource(R.string.house_shop_hint)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { add() }),
                )
                IconButton(onClick = { add() }) { Icon(Icons.Outlined.Add, stringResource(R.string.add), tint = Noc.cyan) }
            }
        }
        if (d.shopping.isEmpty()) item { EmptyHint(stringResource(R.string.house_shop_empty)) }
        items(d.shopping.sortedBy { it.done }, key = { it.id }) { s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(s.done, { on -> thump(); vm.house.update { h -> h.copy(shopping = h.shopping.map { if (it.id == s.id) it.copy(done = on) else it }) } },
                    colors = CheckboxDefaults.colors(checkedColor = Noc.cyan, checkmarkColor = Noc.abyss, uncheckedColor = Noc.text3))
                Text(s.text, style = MaterialTheme.typography.bodyLarge, color = if (s.done) Noc.text3 else Noc.text,
                    textDecoration = if (s.done) TextDecoration.LineThrough else null, modifier = Modifier.weight(1f))
            }
        }
        if (d.shopping.any { it.done }) item {
            TextButton(onClick = { vm.house.update { h -> h.copy(shopping = h.shopping.filter { !it.done }) } }) { Text(stringResource(R.string.house_shop_clear)) }
        }
    }
}

@Composable
private fun ChoreSheet(vm: ModsViewModel, c: Chore?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(c?.name ?: "") }
    var every by remember { mutableStateOf((c?.every ?: 7).toString()) }
    var room by remember { mutableStateOf(c?.room ?: "") }
    FormSheet(stringResource(if (c == null) R.string.house_add else R.string.edit), onDismiss) {
        TextIn(name, { name = it }, stringResource(R.string.habit_name), placeholder = stringResource(R.string.house_name_hint))
        Text(stringResource(R.string.house_every), style = MaterialTheme.typography.labelLarge, color = Noc.text2)
        ChipRow(listOf("1", "3", "7", "14", "30"), every, { stringResource(R.string.freq_n, it.toInt()) }, Noc.cyan) { every = it }
        NumIn(every, { every = it }, stringResource(R.string.house_every_days), suffix = stringResource(R.string.days_short))
        TextIn(room, { room = it }, stringResource(R.string.house_room), placeholder = stringResource(R.string.house_room_hint))
        PrimaryButton(stringResource(R.string.save), onClick = {
            val item = (c ?: Chore(name = "")).copy(name = name.trim(), every = (every.toIntOrNull() ?: 7).coerceIn(1, 365), room = room.trim())
            vm.house.update { h -> h.copy(chores = if (c == null) h.chores + item else h.chores.map { if (it.id == c.id) item else it }) }
            onDismiss()
        }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth(), color = Noc.cyan)
        if (c != null) TextButton(onClick = { vm.house.update { h -> h.copy(chores = h.chores.filter { it.id != c.id }) }; onDismiss() }) {
            Text(stringResource(R.string.delete), color = Noc.coral)
        }
    }
}

package hu.lifeos.app.food.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.food.Food
import hu.lifeos.app.food.FoodLogic
import hu.lifeos.app.food.FoodViewModel
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.VSpace

@Composable
fun FoodListTab(vm: FoodViewModel, modifier: Modifier, toast: (String) -> Unit, onDetails: (Food) -> Unit) {
    val s by vm.state.collectAsState()
    val logic = vm.logic
    val c = vm.content
    val ctx = LocalContext.current
    var q by rememberSaveable { mutableStateOf("") }
    var cat by rememberSaveable { mutableStateOf("all") }
    var rate by rememberSaveable { mutableStateOf("all") }
    var sort by rememberSaveable { mutableStateOf("name") }
    var newOpen by remember { mutableStateOf(false) }
    val nq = FoodLogic.norm(q.trim())
    val sorts = listOf(
        "name" to R.string.food_sort_name, "pdens" to R.string.food_sort_pdens, "p" to R.string.food_sort_p,
        "kcal" to R.string.food_sort_kcal, "c" to R.string.food_sort_c, "gi" to R.string.food_sort_gi,
    )
    val coll = java.text.Collator.getInstance(java.util.Locale("hu", "HU"))
    val list = logic.allFoods(s).filter { f ->
        (rate == "all" || (if (rate == "fav") f.id in s.favs else f.r == rate)) && (cat == "all" || f.cat == cat) && (nq.isEmpty() || FoodLogic.norm(f.name).contains(nq))
    }.let { l ->
        when (sort) {
            "pdens" -> l.sortedByDescending { it.p / maxOf(1.0, it.kcal) }
            "p" -> l.sortedByDescending { it.p }
            "kcal" -> l.sortedBy { it.kcal }
            "c" -> l.sortedBy { it.c }
            "gi" -> l.sortedBy { it.gi }
            else -> l.sortedWith { a, b -> coll.compare(a.name, b.name) }
        }
    }

    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), placeholder = { Text(stringResource(R.string.food_search)) }, singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, null) })
            VSpace(6)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(rate == "all", { rate = "all" }, label = { Text(stringResource(R.string.filter_all)) })
                listOf("g", "m", "b").forEach { r -> FilterChip(rate == r, { rate = r }, label = { Text(c.rtxt[r] ?: r) }) }
                FilterChip(rate == "fav", { rate = "fav" }, label = { Text(stringResource(R.string.food_favs)) })
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(cat == "all", { cat = "all" }, label = { Text(stringResource(R.string.food_all_cats)) })
                c.cats.forEach { (k, v) -> FilterChip(cat == k, { cat = k }, label = { Text(v) }) }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(stringResource(R.string.food_list_count, list.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Picker(stringResource(R.string.food_sort), sorts, sorts.first { it.first == sort }, { ctx.getString(it.second) }, { sort = it.first }, Modifier.width(190.dp))
            }
            TextButton(onClick = { newOpen = true }) { Text(stringResource(R.string.food_new_custom)) }
        }
        items(list.take(300)) { f -> FoodCard(vm, f, f.id in s.favs, toast, onDetails) }
    }
    if (newOpen) NewFoodDialog(c.rtxt, onClose = { newOpen = false }, onSave = { vm.addCustom(it); toast(ctx.getString(R.string.food_custom_saved)); newOpen = false })
}

@Composable
private fun FoodCard(vm: FoodViewModel, f: Food, fav: Boolean, toast: (String) -> Unit, onDetails: (Food) -> Unit) {
    val ctx = LocalContext.current
    val s by vm.state.collectAsState()
    var g by remember(f.id) { mutableStateOf(fmt(if (f.ug > 0) f.ug else 100.0).replace(" ", "")) }
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(f.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            RatePill(f.r, vm.content.rtxt[f.r])
            IconButton(onClick = { vm.toggleFav(f.id) }) {
                Icon(if (fav) Icons.Outlined.Star else Icons.Outlined.StarBorder, stringResource(R.string.food_fav),
                    tint = if (fav) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("${fmt(f.kcal)} kcal · F ${fmt(f.p, 1)} · Sz ${fmt(f.c, 1)} · Zs ${fmt(f.f, 1)} · Rost ${fmt(f.fib, 1)} · GI ${if (f.gi > 0) fmt(f.gi) else "–"}",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (f.note.isNotBlank()) Text(f.note, style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(g, { g = it }, Modifier.width(90.dp), singleLine = true, label = { Text("g") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            TextButton(onClick = {
                val gg = g.replace(',', '.').toDoubleOrNull() ?: 0.0
                if (gg > 0) {
                    vm.addEntry(f, gg, vm.logic.defaultMeal(s.profile), vm.nowHM(), vm.today())
                    toast(ctx.getString(R.string.food_added, f.name, fmt(vm.logic.nutr(f, gg).kcal)))
                }
            }) { Text(stringResource(R.string.food_to_log)) }
            TextButton(onClick = { onDetails(f) }) { Text(stringResource(R.string.food_details)) }
            if (f.src == "custom") IconButton(onClick = { vm.deleteCustom(f.id) }) { Icon(Icons.Outlined.Close, stringResource(R.string.delete)) }
        }
    }
}

@Composable
private fun NewFoodDialog(rtxt: Map<String, String>, onClose: () -> Unit, onSave: (Food) -> Unit) {
    var name by remember { mutableStateOf("") }
    var r by remember { mutableStateOf("g") }
    val nums = remember { mutableStateOf(mapOf("kcal" to "", "p" to "", "c" to "", "f" to "", "fib" to "", "gi" to "40", "ug" to "100")) }
    var u by remember { mutableStateOf("adag") }
    fun d(k: String) = nums.value[k].orEmpty().replace(',', '.').toDoubleOrNull() ?: 0.0
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.food_new_custom)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.field_title)) }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("g", "m", "b").forEach { k -> FilterChip(r == k, { r = k }, label = { Text(rtxt[k] ?: k) }) }
                }
                Text(stringResource(R.string.food_per100), style = MaterialTheme.typography.labelMedium)
                listOf(listOf("kcal" to "kcal", "p" to stringResource(R.string.food_p_short)), listOf("c" to stringResource(R.string.food_c_short), "f" to stringResource(R.string.food_f_short)),
                    listOf("fib" to stringResource(R.string.food_fiber), "gi" to "GI")).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { (k, l) ->
                            OutlinedTextField(nums.value[k].orEmpty(), { v -> nums.value = nums.value + (k to v) }, Modifier.width(110.dp), label = { Text(l) },
                                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(u, { u = it }, Modifier.width(110.dp), label = { Text(stringResource(R.string.food_unit)) }, singleLine = true)
                    OutlinedTextField(nums.value["ug"].orEmpty(), { v -> nums.value = nums.value + ("ug" to v) }, Modifier.width(110.dp), label = { Text(stringResource(R.string.food_unit_g)) },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(Food(id = "", name = name.trim(), r = r, kcal = d("kcal"), p = d("p"), c = d("c"), f = d("f"), fib = d("fib"), gi = d("gi"),
                    u = u.trim().ifBlank { "adag" }, ug = d("ug").takeIf { it > 0 } ?: 100.0))
            }, enabled = name.isNotBlank()) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.cancel)) } },
    )
}


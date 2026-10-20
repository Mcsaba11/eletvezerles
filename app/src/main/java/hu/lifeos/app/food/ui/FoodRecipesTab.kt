package hu.lifeos.app.food.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
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
import hu.lifeos.app.food.LocalPacks
import hu.lifeos.app.food.Recipe
import hu.lifeos.app.food.RecipeItem
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.SectionLabel
import hu.lifeos.app.ui.VSpace

@Composable
fun FoodRecipesTab(vm: FoodViewModel, modifier: Modifier, toast: (String) -> Unit) {
    val s by vm.state.collectAsState()
    val logic = vm.logic
    val c = vm.content
    val ctx = LocalContext.current
    var cur by remember { mutableStateOf(Recipe(id = FoodLogic.rid(), servings = 2)) }
    var ing by remember { mutableStateOf<Food?>(null) }
    var ingG by remember { mutableStateOf("100") }
    var portions by remember { mutableStateOf("1") }
    val sampleIds = c.samples.map { it.id }.toSet()
    val saved = s.recipes.any { it.id == cur.id }

    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.food_recipe_editor), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { cur = Recipe(id = FoodLogic.rid(), servings = 2) }) { Text(stringResource(R.string.food_new_recipe)) }
                }
                OutlinedTextField(cur.name, { cur = cur.copy(name = it) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.food_recipe_name)) }, singleLine = true)
                VSpace(6)
                OutlinedTextField(cur.servings.toString(), { v -> v.toIntOrNull()?.let { cur = cur.copy(servings = it.coerceAtLeast(1)) } },
                    Modifier.width(140.dp), label = { Text(stringResource(R.string.food_servings)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                VSpace(8)
                FoodSearchField(s, logic, c.rtxt, stringResource(R.string.food_ingredient), excludeRecipes = true, onPick = { ing = it })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(ingG, { ingG = it }, Modifier.width(100.dp), label = { Text("g") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        ing?.let { f -> cur = cur.copy(items = cur.items + RecipeItem(f.id, ingG.replace(',', '.').toDoubleOrNull() ?: 100.0)) }
                        ing = null
                    }, enabled = ing != null) { Text(stringResource(R.string.add)) }
                }
                VSpace(8)
                var tot = hu.lifeos.app.food.Nutr(); var w = 0.0
                cur.items.forEachIndexed { i, it ->
                    val f = logic.findFood(s, it.fid, true) ?: return@forEachIndexed
                    val n = logic.nutr(f, it.g); tot += n; w += it.g
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(f.name, style = MaterialTheme.typography.bodyMedium)
                            Text("${fmt(n.kcal)} kcal · F ${fmt(n.p, 1)} · Sz ${fmt(n.c, 1)} · Zs ${fmt(n.f, 1)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedTextField(fmt(it.g).replace(" ", ""), { v -> v.replace(',', '.').toDoubleOrNull()?.let { g ->
                            cur = cur.copy(items = cur.items.toMutableList().also { l -> l[i] = l[i].copy(g = g) }) } },
                            Modifier.width(90.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        IconButton(onClick = { cur = cur.copy(items = cur.items.filterIndexed { j, _ -> j != i }) }) { Icon(Icons.Outlined.Close, null) }
                    }
                }
                if (cur.items.isEmpty()) Text(stringResource(R.string.food_recipe_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else {
                    val sv = maxOf(1, cur.servings).toDouble()
                    val rate = logic.rateFrom(tot)
                    VSpace(8)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.food_per_portion, fmt(tot.kcal / sv), fmt(w / sv)), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        RatePill(rate, c.rtxt[rate])
                    }
                    Text("F ${fmt(tot.p / sv, 1)} g · Sz ${fmt(tot.c / sv, 1)} g · Zs ${fmt(tot.f / sv, 1)} g · Rost ${fmt(tot.fib / sv, 1)} g" + (if (LocalPacks.current.ir) " · GL ${fmt(tot.gl / sv, 1)} (${FoodLogic.glTxt(tot.gl / sv)})" else ""),
                        style = MaterialTheme.typography.bodySmall)
                    VSpace(10)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = {
                            if (cur.name.isBlank()) toast(ctx.getString(R.string.food_recipe_need_name))
                            else { vm.saveRecipe(cur.copy(name = cur.name.trim())); toast(ctx.getString(R.string.food_recipe_saved)) }
                        }) { Text(stringResource(R.string.save)) }
                        if (saved) TextButton(onClick = { vm.deleteRecipe(cur.id); cur = Recipe(id = FoodLogic.rid(), servings = 2); toast(ctx.getString(R.string.food_recipe_deleted)) }) {
                            Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                        }
                    }
                    VSpace(6)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(portions, { portions = it }, Modifier.width(100.dp), label = { Text(stringResource(R.string.food_portions)) }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        OutlinedButton(onClick = {
                            val rf = logic.recipeCalc(s, cur.copy(name = cur.name.ifBlank { ctx.getString(R.string.food_recipe) })).food
                            val pc = portions.replace(',', '.').toDoubleOrNull() ?: 1.0
                            vm.addEntry(rf, rf.ug * pc, logic.defaultMeal(s), vm.nowHM(), vm.today())
                            toast(ctx.getString(R.string.food_added, rf.name, fmt(logic.nutr(rf, rf.ug * pc).kcal)))
                        }) { Text(stringResource(R.string.food_log_recipe)) }
                    }
                }
            }
        }
        item { SectionLabel(stringResource(R.string.food_recipes_list)) }
        items(s.recipes + c.samples) { r ->
            val rc = logic.recipeCalc(s, r)
            val pr = logic.nutr(rc.food, rc.food.ug)
            AppCard(onClick = {
                cur = if (r.id in sampleIds) r.copy(id = FoodLogic.rid()) else r
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.name.ifBlank { stringResource(R.string.food_unnamed_recipe) } + if (r.id in sampleIds) " · " + stringResource(R.string.food_sample) else "",
                            style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.food_recipe_line, r.servings, fmt(pr.kcal), fmt(pr.p)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    RatePill(rc.food.r, c.rtxt[rc.food.r])
                }
            }
        }
    }
}

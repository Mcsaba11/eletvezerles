package hu.lifeos.app.food.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.VSpace

@Composable
fun FoodCalcTab(vm: FoodViewModel, modifier: Modifier, toast: (String) -> Unit) {
    val packsCalc = LocalPacks.current
    val s by vm.state.collectAsState()
    val sent by vm.calcFood.collectAsState()
    val logic = vm.logic
    val c = vm.content
    val ctx = LocalContext.current
    var food by remember { mutableStateOf<Food?>(sent) }
    LaunchedEffect(sent) { if (sent != null) { food = sent; vm.setCalcFood(null) } }
    var qty by remember(food) { mutableStateOf("100") }
    val opts = logic.unitOpts(food)
    var unit by remember(food) { mutableStateOf(opts.first()) }
    var sizeK by remember { mutableStateOf(1.0) }
    var meal by remember { mutableStateOf(logic.defaultMeal(s)) }

    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            AppCard {
                Text(stringResource(R.string.food_calc_title), style = MaterialTheme.typography.titleMedium)
                VSpace(6)
                FoodSearchField(s, logic, c.rtxt, stringResource(R.string.food_food), onPick = { food = it })
                VSpace(6)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    OutlinedTextField(qty, { qty = it }, Modifier.width(90.dp), label = { Text(stringResource(R.string.food_qty)) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    Picker(stringResource(R.string.food_unit), opts, unit, { it.label }, { unit = it }, Modifier.weight(1f))
                }
                if (unit.v != "g") Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.7 to R.string.food_small, 1.0 to R.string.food_medium, 1.4 to R.string.food_big).forEach { (k, l) ->
                        FilterChip(sizeK == k, { sizeK = k }, label = { Text(stringResource(l)) })
                    }
                }
            }
        }
        val f = food
        if (f != null) {
            val q = qty.replace(',', '.').toDoubleOrNull() ?: 0.0
            val g = q * unit.g * (if (unit.v != "g") sizeK else 1.0)
            val n = logic.nutr(f, g)
            val r = logic.targets(s)
            val dayT = logic.totals(s.day(vm.today()).items)
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(f.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        RatePill(f.r, c.rtxt[f.r])
                    }
                    Text("${fmt(g)} g" + if (f.ug > 0 && f.u.isNotBlank()) " · 1 ${f.u} = ${fmt(f.ug)} g" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (f.note.isNotBlank()) { VSpace(4); Text(f.note, style = MaterialTheme.typography.bodyMedium) }
                    VSpace(10)
                    val pd = if (f.kcal > 0) f.p / f.kcal * 100 else 0.0
                    val showGl = LocalPacks.current.let { it.ir || it.diab }
                    TileGrid(listOfNotNull<@Composable (Modifier) -> Unit>(
                        { m -> StatTile(stringResource(R.string.food_kcal), fmt(n.kcal), "kcal", true, m) },
                        { m -> StatTile(stringResource(R.string.food_protein), "${fmt(n.p, 1)} g", stringResource(R.string.food_per100kcal, fmt(pd, 1)), modifier = m) },
                        { m -> StatTile(stringResource(R.string.food_carb), "${fmt(n.c, 1)} g", stringResource(R.string.food_no_fiber), modifier = m) },
                        { m -> StatTile(stringResource(R.string.food_fat), "${fmt(n.f, 1)} g", if (n.kcal > 0) stringResource(R.string.food_pct_of_kcal, fmt(n.f * 9 / n.kcal * 100)) else "", modifier = m) },
                        { m -> StatTile(stringResource(R.string.food_fiber), "${fmt(n.fib, 1)} g", stringResource(R.string.food_of35), modifier = m) },
                        if (showGl) { m -> StatTile(stringResource(R.string.food_gl), fmt(n.gl, 1), "${FoodLogic.glTxt(n.gl)} · GI ${if (f.gi > 0) fmt(f.gi) else "–"} (${FoodLogic.giTxt(f.gi)})", modifier = m) } else null,
                    ))
                }
            }
            item {
                AppCard {
                    Text(stringResource(R.string.food_pct_title), style = MaterialTheme.typography.titleSmall)
                    MacroBar(stringResource(R.string.food_kcal), n.kcal, r.target, "kcal", "max")
                    MacroBar(stringResource(R.string.food_protein), n.p, r.p.toDouble(), "g", "min")
                    MacroBar(stringResource(R.string.food_carb), n.c, r.c.toDouble(), "g", "max")
                    MacroBar(stringResource(R.string.food_fat), n.f, r.f.toDouble(), "g", "max")
                    VSpace(8)
                    val remK = maxOf(0.0, r.target - dayT.kcal); val remC = maxOf(0.0, r.c - dayT.c)
                    val gK = if (f.kcal > 0) remK / f.kcal * 100 else Double.POSITIVE_INFINITY
                    val gC = if (f.c > 0) remC / f.c * 100 else Double.POSITIVE_INFINITY
                    val any = stringResource(R.string.food_any)
                    Text(stringResource(R.string.food_fits_today, if (gK.isFinite()) fmt(gK) + " g" else any, if (gC.isFinite()) fmt(gC) + " g" else any), style = MaterialTheme.typography.bodyMedium)
                    val mn = minOf(gK, gC)
                    if (f.ug > 0 && f.u.isNotBlank() && mn.isFinite()) Text(stringResource(R.string.food_fits_units, fmt(mn / f.ug, 1), f.u), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (packsCalc.ir) item {
                AppCard {
                    Text(stringResource(if (f.r == "g") R.string.food_similar else R.string.food_better), style = MaterialTheme.typography.titleSmall)
                    logic.swapsFor(s, f).forEach { sw ->
                        val sg = if (sw.ug > 0) sw.ug else 100.0
                        val sn = logic.nutr(sw, sg)
                        ClickRow(onClick = { food = sw }) {
                            Column(Modifier.weight(1f)) {
                                Text(sw.name, style = MaterialTheme.typography.bodyLarge)
                                Text((if (sw.u.isNotBlank()) "1 ${sw.u} · " else "") + "${fmt(sn.kcal)} kcal · F ${fmt(sn.p, 1)} g · GL ${fmt(sn.gl, 1)}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            RatePill(sw.r, c.rtxt[sw.r])
                        }
                    }
                }
            }
            item {
                AppCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                        Picker(stringResource(R.string.food_meal), logic.meals(s), meal, { it }, { meal = it }, Modifier.weight(1f))
                        Spacer(Modifier.width(4.dp))
                        Button(onClick = {
                            vm.addEntry(f, g, meal, vm.nowHM(), vm.today(), if (unit.v == "g") null else "${fmt(q)} ${unit.name}")
                            toast(ctx.getString(R.string.food_added, f.name, fmt(n.kcal)))
                        }, enabled = g > 0) { Text(stringResource(R.string.food_add_today)) }
                    }
                }
            }
        } else item {
            Text(stringResource(R.string.food_calc_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


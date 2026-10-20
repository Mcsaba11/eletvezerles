package hu.lifeos.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.data.AppSettings
import hu.lifeos.app.data.Modules
import hu.lifeos.app.food.FoodPacks
import hu.lifeos.app.food.FoodViewModel
import hu.lifeos.app.food.ui.PackToggles
import hu.lifeos.app.ui.PrimaryButton
import hu.lifeos.app.ui.Segmented
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.theme.Noc

/** A modulok megjelenítése: ikon, szín, név, leírás. */
data class ModuleInfo(val id: String, val icon: ImageVector, val color: @Composable () -> Color, val title: Int, val desc: Int)

val MODULE_INFO = listOf(
    ModuleInfo(Modules.FOOD, Icons.Outlined.Restaurant, { Noc.good }, R.string.mod_food, R.string.mod_food_desc),
    ModuleInfo(Modules.SATORI, Icons.Outlined.Spa, { Noc.amber }, R.string.mod_satori, R.string.mod_satori_desc),
    ModuleInfo(Modules.MIND, Icons.Outlined.Mood, { Noc.violet }, R.string.mod_mind, R.string.mod_mind_desc),
    ModuleInfo(Modules.MONEY, Icons.Outlined.AccountBalanceWallet, { Noc.sapphire }, R.string.mod_money, R.string.mod_money_desc),
    ModuleInfo(Modules.TASKS, Icons.Outlined.TaskAlt, { Noc.cyan }, R.string.mod_tasks, R.string.mod_tasks_desc),
    ModuleInfo(Modules.WHEEL, Icons.Outlined.Tune, { Noc.ice }, R.string.mod_wheel, R.string.mod_wheel_desc),
)

/** Egy modul választható csempéje (a varázslóban és a beállításokban). */
@Composable
fun ModuleTile(m: ModuleInfo, on: Boolean, onToggle: () -> Unit) {
    val col = m.color()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (on) col.copy(alpha = 0.10f) else Noc.card)
            .border(1.dp, if (on) col.copy(alpha = 0.6f) else Noc.stroke, RoundedCornerShape(18.dp))
            .clickable(onClick = onToggle).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(col.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(m.icon, null, tint = col)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(m.title), style = MaterialTheme.typography.titleSmall, color = Noc.text)
            Text(stringResource(m.desc), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
        }
        Spacer(Modifier.width(8.dp))
        Icon(if (on) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null, tint = if (on) col else Noc.text3)
    }
}

/**
 * Első indítás: név, modulok, étkezési csomagok és alapadatok, költségkeret.
 * Az app üresen indul, mindenki maga építi fel. Később a Beállításokban bármikor újraindítható.
 */
@Composable
fun SetupWizard(settings: AppSettings, food: FoodViewModel, onFinish: (AppSettings) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(settings.name) }
    var mods by rememberSaveable { mutableStateOf(settings.modules ?: listOf(Modules.TASKS, Modules.MIND)) }
    var packs by rememberSaveable { mutableStateOf(food.state.value.let { s -> s.packs ?: emptyList() }) }
    val p0 = food.state.value.profile
    var sex by rememberSaveable { mutableStateOf(p0.sex) }
    var age by rememberSaveable { mutableStateOf(p0.age.toInt().toString()) }
    var height by rememberSaveable { mutableStateOf(p0.height.toInt().toString()) }
    var weight by rememberSaveable { mutableStateOf(p0.weight.toInt().toString()) }
    var budget by rememberSaveable { mutableStateOf(if (settings.monthlyBudget > 0) settings.monthlyBudget.toLong().toString() else "") }

    // A lépések listája a választásoktól függ.
    val steps = buildList {
        add("hello"); add("modules")
        if (Modules.FOOD in mods) add("food")
        if (Modules.MONEY in mods) add("money")
        add("done")
    }
    val key = steps.getOrElse(step) { "done" }
    BackHandler(enabled = step > 0) { step-- }

    fun finish() {
        if (Modules.FOOD in mods) {
            food.setPacks(packs)
            if (FoodPacks.ED !in packs) food.setProfile { p ->
                p.copy(
                    sex = sex, age = age.toDoubleOrNull() ?: p.age, height = height.toDoubleOrNull() ?: p.height,
                    weight = weight.toDoubleOrNull() ?: p.weight,
                )
            }
        }
        onFinish(settings.copy(name = name.trim(), modules = mods, welcomeDone = true, monthlyBudget = budget.toDoubleOrNull() ?: settings.monthlyBudget))
    }

    Box(Modifier.fillMaxSize().background(Noc.canvas)) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = 24.dp)) {
            // Lépésjelző
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                steps.indices.forEach { i ->
                    Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(if (i <= step) Noc.cyan else Noc.well))
                }
            }
            AnimatedContent(
                key, modifier = Modifier.weight(1f).fillMaxWidth(), label = "wizard",
                transitionSpec = { (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut()) },
            ) { k ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (k) {
                        "hello" -> {
                            VSpace(40)
                            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall, color = Noc.cyan)
                            Text(stringResource(R.string.wiz_hello), style = MaterialTheme.typography.headlineSmall, color = Noc.text)
                            Text(stringResource(R.string.wiz_hello_sub), style = MaterialTheme.typography.bodyLarge, color = Noc.text2)
                            Text(stringResource(R.string.welcome_privacy), style = MaterialTheme.typography.bodyMedium, color = Noc.text3)
                            VSpace(12)
                            OutlinedTextField(
                                name, { name = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.welcome_name)) },
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            )
                        }
                        "modules" -> {
                            Text(stringResource(R.string.wiz_modules), style = MaterialTheme.typography.headlineSmall, color = Noc.text)
                            Text(stringResource(R.string.wiz_modules_sub), style = MaterialTheme.typography.bodyMedium, color = Noc.text2)
                            MODULE_INFO.forEach { m ->
                                ModuleTile(m, m.id in mods) { mods = if (m.id in mods) mods - m.id else mods + m.id }
                            }
                        }
                        "food" -> {
                            Text(stringResource(R.string.wiz_food), style = MaterialTheme.typography.headlineSmall, color = Noc.text)
                            Text(stringResource(R.string.wiz_food_sub), style = MaterialTheme.typography.bodyMedium, color = Noc.text2)
                            PackToggles(packs.toSet()) { id -> packs = if (id in packs) packs - id else packs + id }
                            if (FoodPacks.ED !in packs) {
                                VSpace(4)
                                Text(stringResource(R.string.wiz_food_data), style = MaterialTheme.typography.titleSmall, color = Noc.text)
                                val male = stringResource(R.string.food_male); val female = stringResource(R.string.food_female)
                                Segmented(listOf("f", "m"), sex, { if (it == "m") male else female }, { sex = it }, Modifier.fillMaxWidth())
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(
                                        Triple(R.string.food_age, age) { v: String -> age = v },
                                        Triple(R.string.food_height, height) { v: String -> height = v },
                                        Triple(R.string.food_weight, weight) { v: String -> weight = v },
                                    ).forEach { (l, v, set) ->
                                        OutlinedTextField(v, { set(it.filter { c -> c.isDigit() }) }, Modifier.weight(1f), singleLine = true,
                                            label = { Text(stringResource(l)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                    }
                                }
                                Text(stringResource(R.string.wiz_food_later), style = MaterialTheme.typography.bodySmall, color = Noc.text3)
                            }
                        }
                        "money" -> {
                            Text(stringResource(R.string.wiz_money), style = MaterialTheme.typography.headlineSmall, color = Noc.text)
                            Text(stringResource(R.string.wiz_money_sub), style = MaterialTheme.typography.bodyMedium, color = Noc.text2)
                            OutlinedTextField(
                                budget, { v -> budget = v.filter { it.isDigit() } }, Modifier.fillMaxWidth(), singleLine = true,
                                label = { Text(stringResource(R.string.set_budget)) }, suffix = { Text("Ft") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                        }
                        else -> {
                            VSpace(40)
                            Text(stringResource(R.string.wiz_done), style = MaterialTheme.typography.headlineMedium, color = Noc.text)
                            Text(stringResource(R.string.wiz_done_sub), style = MaterialTheme.typography.bodyLarge, color = Noc.text2)
                            if (Modules.SATORI in mods) Text(stringResource(R.string.wiz_done_satori), style = MaterialTheme.typography.bodyMedium, color = Noc.amber)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) TextButton(onClick = { step-- }) { Text(stringResource(R.string.back), color = Noc.text2) }
                Spacer(Modifier.weight(1f))
                PrimaryButton(
                    stringResource(if (key == "done") R.string.welcome_start else R.string.next),
                    onClick = { if (key == "done") finish() else step++ },
                )
            }
        }
    }
}

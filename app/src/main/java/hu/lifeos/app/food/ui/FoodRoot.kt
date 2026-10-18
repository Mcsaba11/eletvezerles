package hu.lifeos.app.food.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.food.FoodViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodRoot(vm: FoodViewModel, onExit: () -> Unit) {
    val loaded by vm.loaded.collectAsState()
    if (!loaded) return
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = { msg -> scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(msg) } }
    val tabs = listOf(
        R.string.food_tab_today, R.string.food_tab_calc, R.string.food_tab_recipes, R.string.food_tab_foods,
        R.string.food_tab_weight, R.string.food_tab_stats, R.string.food_tab_tips, R.string.food_tab_profile,
    )
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_food)) },
                    navigationIcon = { IconButton(onClick = onExit) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back)) } },
                    actions = { IconButton(onClick = { tab = 7 }) { Icon(Icons.Outlined.Person, stringResource(R.string.food_tab_profile)) } },
                )
                ScrollableTabRow(selectedTabIndex = tab, edgePadding = 12.dp, containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEachIndexed { i, l -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(stringResource(l)) }) }
                }
            }
        },
    ) { pad ->
        val m = Modifier.fillMaxSize().padding(pad)
        when (tab) {
            0 -> FoodTodayTab(vm, m, toast, onProfile = { tab = 7 })
            1 -> FoodCalcTab(vm, m, toast)
            2 -> FoodRecipesTab(vm, m, toast)
            3 -> FoodListTab(vm, m, toast, onDetails = { f -> vm.setCalcFood(f); tab = 1 })
            4 -> FoodWeightTab(vm, m, toast)
            5 -> FoodStatsTab(vm, m)
            6 -> FoodTipsTab(vm, m)
            else -> FoodProfileTab(vm, m, toast)
        }
    }
}

package hu.lifeos.app.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.food.FoodViewModel
import hu.lifeos.app.ui.rememberTick
import hu.lifeos.app.ui.theme.Noc
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodRoot(vm: FoodViewModel, onExit: () -> Unit) {
    val loaded by vm.loaded.collectAsState()
    if (!loaded) return
    val tabs = listOf(
        R.string.food_tab_today, R.string.food_tab_calc, R.string.food_tab_recipes, R.string.food_tab_foods,
        R.string.food_tab_weight, R.string.food_tab_stats, R.string.food_tab_tips, R.string.food_tab_profile,
    )
    val pager = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val tick = rememberTick()
    val snackbar = remember { SnackbarHostState() }
    val toast: (String) -> Unit = { msg -> scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(msg) } }
    val go: (Int) -> Unit = { i -> scope.launch { pager.animateScrollToPage(i) } }
    val rowState = rememberLazyListState()
    LaunchedEffect(pager.currentPage) { rowState.animateScrollToItem((pager.currentPage - 1).coerceAtLeast(0)) }

    Scaffold(
        containerColor = Noc.canvas,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column(Modifier.background(Noc.canvas).statusBarsPadding().padding(top = 4.dp, bottom = 8.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onExit) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back)) }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.card_body_ir).uppercase(), style = MaterialTheme.typography.labelSmall, color = Noc.good)
                        Text(stringResource(R.string.app_food), style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(onClick = { go(7) }) { Icon(Icons.Outlined.Person, stringResource(R.string.food_tab_profile), tint = Noc.text2) }
                }
                LazyRow(
                    state = rowState, contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(tabs) { i, l ->
                        val on = pager.currentPage == i
                        Text(
                            stringResource(l), style = MaterialTheme.typography.labelLarge,
                            color = if (on) Noc.abyss else Noc.text2,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) Noc.good else Noc.card)
                                .border(1.dp, if (on) Noc.good else Noc.stroke, RoundedCornerShape(50))
                                .clickable { tick(); go(i) }.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        },
    ) { pad ->
        HorizontalPager(pager, Modifier.fillMaxSize().padding(pad), beyondViewportPageCount = 1) { page ->
            val m = Modifier.fillMaxSize()
            when (page) {
                0 -> FoodTodayTab(vm, m, toast, onProfile = { go(7) })
                1 -> FoodCalcTab(vm, m, toast)
                2 -> FoodRecipesTab(vm, m, toast)
                3 -> FoodListTab(vm, m, toast, onDetails = { f -> vm.setCalcFood(f); go(1) })
                4 -> FoodWeightTab(vm, m, toast)
                5 -> FoodStatsTab(vm, m)
                6 -> FoodTipsTab(vm, m)
                else -> FoodProfileTab(vm, m, toast)
            }
        }
    }
}

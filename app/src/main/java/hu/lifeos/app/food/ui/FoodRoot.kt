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
import hu.lifeos.app.food.LocalPacks
import hu.lifeos.app.food.packFlags
import hu.lifeos.app.food.packIds
import androidx.compose.runtime.CompositionLocalProvider
import hu.lifeos.app.ui.rememberTick
import hu.lifeos.app.ui.theme.Noc
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodRoot(vm: FoodViewModel, onExit: () -> Unit) {
    val loaded by vm.loaded.collectAsState()
    if (!loaded) return
    val st by vm.state.collectAsState()
    val packs = st.packFlags()
    // A fülek a bekapcsolt csomagoktól függnek: számok nélkül csak Ma, Tippek, Profil.
    val keys = if (packs.numbers) listOf("today", "calc", "recipes", "foods", "weight", "stats", "tips", "profile")
    else listOf("today", "tips", "profile")
    val tabLabel = mapOf(
        "today" to R.string.food_tab_today, "calc" to R.string.food_tab_calc, "recipes" to R.string.food_tab_recipes,
        "foods" to R.string.food_tab_foods, "weight" to R.string.food_tab_weight, "stats" to R.string.food_tab_stats,
        "tips" to R.string.food_tab_tips, "profile" to R.string.food_tab_profile,
    )
    val tabs = keys.map { tabLabel.getValue(it) }
    val pager = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val tick = rememberTick()
    val snackbar = remember { SnackbarHostState() }
    val toast: (String) -> Unit = { msg -> scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(msg) } }
    val go: (Int) -> Unit = { i -> scope.launch { pager.animateScrollToPage(i) } }
    val goKey: (String) -> Unit = { k -> keys.indexOf(k).takeIf { it >= 0 }?.let(go) }
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
                        val sub = hu.lifeos.app.food.FoodPacks.ALL.filter { it in st.packIds() }.map { stringResource(packLabel(it).first) }
                        Text((sub.joinToString(" · ").ifBlank { stringResource(R.string.food_general) }).uppercase(), style = MaterialTheme.typography.labelSmall, color = Noc.good, maxLines = 1)
                        Text(stringResource(R.string.app_food), style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(onClick = { goKey("profile") }) { Icon(Icons.Outlined.Person, stringResource(R.string.food_tab_profile), tint = Noc.text2) }
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
        CompositionLocalProvider(LocalPacks provides packs) {
            HorizontalPager(pager, Modifier.fillMaxSize().padding(pad), beyondViewportPageCount = 1, key = { keys.getOrElse(it) { "x$it" } }) { page ->
                val m = Modifier.fillMaxSize()
                when (keys.getOrElse(page) { "today" }) {
                    "today" -> FoodTodayTab(vm, m, toast, onProfile = { goKey("profile") })
                    "calc" -> FoodCalcTab(vm, m, toast)
                    "recipes" -> FoodRecipesTab(vm, m, toast)
                    "foods" -> FoodListTab(vm, m, toast, onDetails = { f -> vm.setCalcFood(f); goKey("calc") })
                    "weight" -> FoodWeightTab(vm, m, toast)
                    "stats" -> FoodStatsTab(vm, m)
                    "tips" -> FoodTipsTab(vm, m)
                    else -> FoodProfileTab(vm, m, toast)
                }
            }
        }
    }
}

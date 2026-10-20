package hu.lifeos.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import hu.lifeos.app.R
import hu.lifeos.app.capture.CaptureType
import hu.lifeos.app.food.FoodViewModel
import hu.lifeos.app.food.ui.FoodRoot
import hu.lifeos.app.insight.Insights
import hu.lifeos.app.satori.SatoriViewModel
import hu.lifeos.app.satori.ui.BreathOverlay
import hu.lifeos.app.satori.ui.SatoriRoot
import hu.lifeos.app.ui.screens.AppsScreen
import hu.lifeos.app.ui.screens.CalendarScreen
import hu.lifeos.app.ui.screens.EventEditScreen
import hu.lifeos.app.ui.screens.PatternsScreen
import hu.lifeos.app.ui.screens.SenseiScreen
import hu.lifeos.app.ui.screens.SettingsScreen
import hu.lifeos.app.ui.screens.TimelineScreen
import hu.lifeos.app.ui.screens.TodayScreen
import hu.lifeos.app.ui.screens.TodosScreen
import hu.lifeos.app.ui.screens.SetupWizard
import hu.lifeos.app.ui.theme.Noc
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Kívülről érkező parancs (gyorsparancs, widget, értesítés). */
object Actions {
    const val SOS = "sos"
    const val CAPTURE = "capture"
    const val VOICE = "voice"
    const val DAYCLOSE = "dayclose"
    const val FOOD = "food"
    const val BREATH = "breath"
}

@Composable
fun AppRoot(vm: MainViewModel, satori: SatoriViewModel, food: FoodViewModel, mods: hu.lifeos.app.mods.ModsViewModel, action: StateFlow<String?>, consumeAction: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val s = settings ?: return
    if (!s.welcomeDone) {
        SetupWizard(s, food, onFinish = { next -> vm.updateSettings { next } })
        return
    }

    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "today"
    val isTab = dockTabs.any { it.route == route }
    var captureOpen by rememberSaveable { mutableStateOf(false) }
    var captureInitial by rememberSaveable { mutableStateOf("") }
    var breathOpen by rememberSaveable { mutableStateOf(false) }
    var breathTech by rememberSaveable { mutableStateOf("relax") }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val mood by vm.todayMood.collectAsState()
    val st by satori.state.collectAsState()
    val pending by action.collectAsState()

    fun openSatori(sos: Boolean = false) = nav.navigate(if (sos) "satori?sos=true" else "satori?sos=false") { launchSingleTop = true }
    fun go(r: String) = nav.navigate(r) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    val voice = rememberVoiceInput { t -> captureInitial = t; captureOpen = true }

    LaunchedEffect(pending) {
        when (pending) {
            Actions.SOS -> openSatori(true)
            Actions.CAPTURE -> { captureInitial = ""; captureOpen = true }
            Actions.VOICE -> voice()
            Actions.DAYCLOSE -> openSatori(false)
            Actions.FOOD -> nav.navigate("food") { launchSingleTop = true }
            Actions.BREATH -> { breathTech = "relax"; breathOpen = true }
            else -> pending?.takeIf { it.startsWith("open:") }?.let { r -> nav.navigate(r.removePrefix("open:")) { launchSingleTop = true } }
        }
        if (pending != null) consumeAction()
    }

    val tabPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 170.dp)
    val initials = s.name.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }.ifBlank { "M" }
    val flow = Insights.inFlow(st, mood)

    Box(Modifier.fillMaxSize().background(Noc.canvas)) {
        Column(Modifier.fillMaxSize()) {
            AnimatedVisibility(isTab, enter = fadeIn(), exit = fadeOut()) {
                TopHeader(
                    status = stringResource(if (flow) R.string.status_in_flow else R.string.status_care),
                    statusColor = if (flow) Noc.good else Noc.amber,
                    initials = initials, onSos = if (s.moduleOn(hu.lifeos.app.data.Modules.SATORI)) ({ openSatori(true) }) else null,
                    onProfile = { nav.navigate("settings") },
                )
            }
            NavHost(
                nav, startDestination = "today", modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    if (dockTabs.any { it.route == targetState.destination.route }) fadeIn()
                    else slideInHorizontally { it / 3 } + fadeIn()
                },
                exitTransition = { fadeOut() },
                popEnterTransition = { fadeIn() },
                popExitTransition = {
                    if (dockTabs.any { it.route == initialState.destination.route }) fadeOut()
                    else slideOutHorizontally { it / 3 } + fadeOut()
                },
            ) {
                composable("today") {
                    TodayScreen(
                        vm, satori, food, tabPadding,
                        onOpenSatori = { openSatori() }, onOpenFood = { nav.navigate("food") },
                        onOpenCalendar = { nav.navigate("calendar") }, onOpenTodos = { nav.navigate("todos") },
                        onOpenSensei = { go("sensei") }, onOpenPatterns = { go("patterns") },
                        onOpenSettings = { nav.navigate("settings") }, onBreath = { breathTech = "relax"; breathOpen = true },
                        onCapture = { captureInitial = ""; captureOpen = true }, onVoice = voice,
                        mods = mods, onOpenModule = { r -> nav.navigate(r) { launchSingleTop = true } },
                    )
                }
                composable("timeline") {
                    TimelineScreen(vm, satori, food, tabPadding, onOpenSatori = { openSatori() }, onOpenFood = { nav.navigate("food") },
                        mods = mods, onOpenModule = { r -> nav.navigate(r) { launchSingleTop = true } },
                        onEditEvent = { id -> nav.navigate("event?id=$id&date=") })
                }
                composable("apps") {
                    AppsScreen(
                        tabPadding, s, onOpenModule = { r -> nav.navigate(r) { launchSingleTop = true } }, onToggleModule = { id -> vm.updateSettings { st -> val cur = st.moduleList(); st.copy(modules = if (id in cur) cur - id else cur + id) } },
                        onOpenSatori = { openSatori() }, onOpenFood = { nav.navigate("food") },
                        onOpenCalendar = { nav.navigate("calendar") }, onOpenTodos = { nav.navigate("todos") },
                        onOpenSettings = { nav.navigate("settings") },
                    )
                }
                composable("patterns") { PatternsScreen(vm, satori, food, tabPadding, onOpenSettings = { nav.navigate("settings") }, mods = mods) }
                composable("sensei") { SenseiScreen(vm, satori, food, tabPadding, onBreath = { breathTech = "relax"; breathOpen = true }, onSos = { openSatori(true) }) }
                composable("calendar") {
                    CalendarScreen(vm,
                        onNewEvent = { date -> nav.navigate("event?id=-1&date=$date") },
                        onEditEvent = { id -> nav.navigate("event?id=$id&date=") },
                        onOpenSettings = { nav.navigate("settings") },
                        onBack = { nav.popBackStack() },
                    )
                }
                composable("money") {
                    val sst by satori.state.collectAsState()
                    val saved = if (s.moduleOn(hu.lifeos.app.data.Modules.SATORI) && sst.onboardingDone == true)
                        sst.followedIds().sumOf { id -> sst.data[id]?.let { it.dailyCost * it.streak } ?: 0.0 } else null
                    hu.lifeos.app.mods.ui.MoneyScreen(mods, s.monthlyBudget, onSetBudget = { v -> vm.updateSettings { it.copy(monthlyBudget = v) } }, satoriSaved = saved, onBack = { nav.popBackStack() })
                }
                composable("mind") { hu.lifeos.app.ui.screens.MindScreen(vm, satori, onBreath = { t -> breathTech = t; breathOpen = true }, onBack = { nav.popBackStack() }) }
                composable("sleep") { hu.lifeos.app.mods.ui.SleepScreen(mods, onBack = { nav.popBackStack() }) }
                composable("move") { hu.lifeos.app.mods.ui.MoveScreen(mods, onBack = { nav.popBackStack() }) }
                composable("people") { hu.lifeos.app.mods.ui.PeopleScreen(mods, onBack = { nav.popBackStack() }) }
                composable("goals") { hu.lifeos.app.mods.ui.GoalsScreen(mods, onBack = { nav.popBackStack() }) }
                composable("learn") { hu.lifeos.app.mods.ui.LearnScreen(mods, onBack = { nav.popBackStack() }) }
                composable("house") { hu.lifeos.app.mods.ui.HouseScreen(mods, onBack = { nav.popBackStack() }) }
                composable("trackers") { hu.lifeos.app.mods.ui.TrackersScreen(mods, onBack = { nav.popBackStack() }) }
                composable("todos") { TodosScreen(vm, onBack = { nav.popBackStack() }) }
                composable("food") { FoodRoot(food, onExit = { nav.popBackStack() }) }
                composable("settings") { SettingsScreen(vm, satori, onBack = { nav.popBackStack() }) }
                composable(
                    "event?id={id}&date={date}",
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                        navArgument("date") { type = NavType.StringType; defaultValue = "" },
                    ),
                ) { entry ->
                    EventEditScreen(vm, id = entry.arguments?.getLong("id") ?: -1L, date = entry.arguments?.getString("date").orEmpty(), onBack = { nav.popBackStack() })
                }
                composable(
                    "satori?sos={sos}",
                    arguments = listOf(navArgument("sos") { type = NavType.BoolType; defaultValue = false }),
                ) { entry ->
                    SatoriRoot(satori, startSos = entry.arguments?.getBoolean("sos") ?: false, onExit = { nav.popBackStack() })
                }
            }
        }

        // ---------- Lebegő beviteli kapszula és dokk ----------
        AnimatedVisibility(
            isTab, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut(),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (route == "today" || route == "timeline") CapturePill(onClick = { captureInitial = ""; captureOpen = true }, onMic = voice, modifier = Modifier.padding(bottom = 4.dp))
                FloatingDock(route, onSelect = { go(it) })
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 150.dp))
    }

    if (captureOpen) {
        CaptureSheet(
            initial = captureInitial,
            onDismiss = { captureOpen = false },
            onSave = { parsed ->
                vm.saveCapture(parsed) { type ->
                    val msg = when (type) {
                        CaptureType.EVENT -> R.string.saved_event
                        CaptureType.TODO -> R.string.saved_todo
                        CaptureType.EXPENSE -> R.string.saved_expense
                        CaptureType.INCOME -> R.string.saved_income
                        CaptureType.MOOD -> R.string.saved_mood
                        CaptureType.NOTE -> R.string.saved_note
                    }
                    scope.launch { snackbar.showSnackbar(ctx.getString(msg)) }
                }
                captureOpen = false
            },
        )
    }
    if (breathOpen) BreathOverlay(satori, onClose = { breathOpen = false }, initial = breathTech)
}

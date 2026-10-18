package hu.lifeos.app.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import hu.lifeos.app.satori.SatoriViewModel
import hu.lifeos.app.satori.ui.SatoriRoot
import hu.lifeos.app.ui.screens.AppsScreen
import hu.lifeos.app.ui.screens.CalendarScreen
import hu.lifeos.app.ui.screens.EventEditScreen
import hu.lifeos.app.ui.screens.SettingsScreen
import hu.lifeos.app.ui.screens.TimelineScreen
import hu.lifeos.app.ui.screens.TodayScreen
import hu.lifeos.app.ui.screens.TodosScreen
import hu.lifeos.app.ui.screens.WelcomeScreen
import kotlinx.coroutines.launch

private data class Tab(val route: String, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab("today", R.string.tab_today, Icons.Outlined.WbSunny),
    Tab("calendar", R.string.tab_calendar, Icons.Outlined.CalendarToday),
    Tab("todos", R.string.tab_todos, Icons.Outlined.CheckCircle),
    Tab("timeline", R.string.tab_timeline, Icons.Outlined.History),
    Tab("apps", R.string.tab_apps, Icons.Outlined.Apps),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: MainViewModel, satori: SatoriViewModel, food: FoodViewModel) {
    val settings by vm.settings.collectAsState()
    val s = settings ?: return
    if (!s.welcomeDone) {
        WelcomeScreen(onDone = { name -> vm.updateSettings { it.copy(name = name, welcomeDone = true) } })
        return
    }

    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "today"
    val isTab = tabs.any { it.route == route }
    var captureOpen by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    fun openSatori(sos: Boolean = false) = nav.navigate(if (sos) "satori?sos=true" else "satori?sos=false")

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (isTab) NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = route == t.route,
                        onClick = {
                            nav.navigate(t.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(t.icon, null) },
                        label = { Text(stringResource(t.label), maxLines = 1) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (isTab && route != "apps") FloatingActionButton(
                onClick = { captureOpen = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) { Icon(Icons.Outlined.Add, stringResource(R.string.capture_title)) }
        },
    ) { pad ->
        NavHost(
            nav, startDestination = "today",
            modifier = if (isTab) Modifier.padding(bottom = pad.calculateBottomPadding()) else Modifier,
            enterTransition = { fadeIn() }, exitTransition = { fadeOut() },
        ) {
            composable("today") {
                TodayScreen(vm, satori, food,
                    onOpenSatori = { openSatori() }, onSos = { openSatori(true) },
                    onOpenFood = { nav.navigate("food") },
                    onOpenCalendar = { nav.navigate("calendar") { launchSingleTop = true } },
                    onOpenTodos = { nav.navigate("todos") { launchSingleTop = true } },
                    onOpenSettings = { nav.navigate("settings") },
                    onCapture = { captureOpen = true },
                )
            }
            composable("calendar") {
                CalendarScreen(vm,
                    onNewEvent = { date -> nav.navigate("event?id=-1&date=$date") },
                    onEditEvent = { id -> nav.navigate("event?id=$id&date=") },
                    onOpenSettings = { nav.navigate("settings") },
                )
            }
            composable("todos") { TodosScreen(vm) }
            composable("timeline") { TimelineScreen(vm, satori) }
            composable("apps") {
                AppsScreen(onOpenSatori = { openSatori() }, onOpenFood = { nav.navigate("food") }, onOpenSettings = { nav.navigate("settings") })
            }
            composable("food") { FoodRoot(food, onExit = { nav.popBackStack() }) }
            composable("settings") { SettingsScreen(vm, satori, onBack = { nav.popBackStack() }) }
            composable(
                "event?id={id}&date={date}",
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("date") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                EventEditScreen(
                    vm, id = entry.arguments?.getLong("id") ?: -1L, date = entry.arguments?.getString("date").orEmpty(),
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                "satori?sos={sos}",
                arguments = listOf(navArgument("sos") { type = NavType.BoolType; defaultValue = false }),
            ) { entry ->
                SatoriRoot(satori, startSos = entry.arguments?.getBoolean("sos") ?: false, onExit = { nav.popBackStack() })
            }
        }
    }

    if (captureOpen) {
        CaptureSheet(
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
}

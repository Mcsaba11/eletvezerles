package hu.lifeos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.data.AppSettings
import hu.lifeos.app.data.Modules
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.Pill
import hu.lifeos.app.ui.SectionLabel
import hu.lifeos.app.ui.theme.Noc

private data class MiniApp(val name: Int, val desc: Int, val icon: ImageVector, val color: @Composable () -> Color, val onClick: (() -> Unit)? = null)

@Composable
fun AppsScreen(
    contentPadding: PaddingValues, settings: AppSettings, onToggleModule: (String) -> Unit, onOpenModule: (String) -> Unit,
    onOpenSatori: () -> Unit, onOpenFood: () -> Unit, onOpenCalendar: () -> Unit, onOpenTodos: () -> Unit, onOpenSettings: () -> Unit,
) {
    val active = listOfNotNull(
        if (settings.moduleOn(Modules.SATORI)) MiniApp(R.string.satori_name, R.string.app_satori_desc, Icons.Outlined.Spa, { Noc.amber }, onOpenSatori) else null,
        if (settings.moduleOn(Modules.FOOD)) MiniApp(R.string.app_food, R.string.app_food_desc, Icons.Outlined.Restaurant, { Noc.good }, onOpenFood) else null,
        if (settings.moduleOn(Modules.TASKS)) MiniApp(R.string.tab_calendar, R.string.app_calendar_desc, Icons.Outlined.CalendarToday, { Noc.sapphire }, onOpenCalendar) else null,
        if (settings.moduleOn(Modules.TASKS)) MiniApp(R.string.tab_todos, R.string.app_todos_desc, Icons.Outlined.CheckCircle, { Noc.cyan }, onOpenTodos) else null,
    ) + MODULE_INFO.filter { it.id in APP_ROUTES && settings.moduleOn(it.id) }.map { m ->
        MiniApp(m.title, m.desc, m.icon, m.color, { onOpenModule(m.id) })
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(), contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            Column {
                Text(stringResource(R.string.tab_apps), style = MaterialTheme.typography.headlineLarge)
                Text(stringResource(R.string.apps_sub), style = MaterialTheme.typography.bodyMedium, color = Noc.text2)
            }
        }
        if (active.isNotEmpty()) item(span = { GridItemSpan(2) }) { SectionLabel(stringResource(R.string.apps_active)) }
        items(active) { a -> Tile(a, ready = true) }
        item(span = { GridItemSpan(2) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.apps_modules))
                Text(stringResource(R.string.apps_modules_sub), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                MODULE_INFO.forEach { m -> ModuleTile(m, settings.moduleOn(m.id)) { onToggleModule(m.id) } }
            }
        }
        item(span = { GridItemSpan(2) }) {
            AppCard(onClick = onOpenSettings, padding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Settings, null, tint = Noc.text2)
                    Text("  " + stringResource(R.string.settings), style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
private fun Tile(a: MiniApp, ready: Boolean) {
    val col = a.color()
    AppCard(Modifier.height(150.dp).let { if (!ready) it.alpha(0.6f) else it }, onClick = a.onClick, padding = PaddingValues(14.dp), glow = if (ready) col else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(col.copy(alpha = 0.12f)).border(1.dp, col.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(a.icon, null, tint = col) }
            Spacer(Modifier.weight(1f))
            if (!ready) Pill(stringResource(R.string.coming_soon), textColor = Noc.text3)
        }
        Spacer(Modifier.weight(1f))
        Text(stringResource(a.name), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(a.desc), style = MaterialTheme.typography.bodySmall, color = Noc.text2, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** A saját képernyővel rendelkező új modulok (a modul azonosítója egyben az útvonal is). */
val APP_ROUTES = setOf("mind", "money", "sleep", "move", "people", "goals", "learn", "house", "trackers")

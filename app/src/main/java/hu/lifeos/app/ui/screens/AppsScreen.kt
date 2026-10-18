package hu.lifeos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.ui.Pill

private data class MiniApp(val name: Int, val desc: Int, val icon: ImageVector, val ready: Boolean, val onClick: (() -> Unit)? = null)

@Composable
fun AppsScreen(onOpenSatori: () -> Unit, onOpenFood: () -> Unit, onOpenSettings: () -> Unit) {
    val apps = listOf(
        MiniApp(R.string.satori_name, R.string.app_satori_desc, Icons.Outlined.Spa, true, onOpenSatori),
        MiniApp(R.string.app_food, R.string.app_food_desc, Icons.Outlined.Restaurant, true, onOpenFood),
        MiniApp(R.string.app_money, R.string.app_money_desc, Icons.Outlined.AccountBalanceWallet, false),
        MiniApp(R.string.app_sleep, R.string.app_sleep_desc, Icons.Outlined.NightsStay, false),
        
        MiniApp(R.string.app_move, R.string.app_move_desc, Icons.Outlined.FitnessCenter, false),
        MiniApp(R.string.app_people, R.string.app_people_desc, Icons.Outlined.People, false),
        MiniApp(R.string.app_goals, R.string.app_goals_desc, Icons.Outlined.Flag, false),
        MiniApp(R.string.app_learn, R.string.app_learn_desc, Icons.Outlined.School, false),
        MiniApp(R.string.app_home, R.string.app_home_desc, Icons.Outlined.Home, false),
        MiniApp(R.string.app_builder, R.string.app_builder_desc, Icons.Outlined.Widgets, false),
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.tab_apps), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.apps_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onOpenSettings) { Icon(Icons.Outlined.Settings, stringResource(R.string.settings)) }
            }
        }
        items(apps) { a ->
            Column(
                Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(22.dp))
                    .background(if (a.ready) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainerLow)
                    .let { m -> if (a.onClick != null) m.clickable(onClick = a.onClick) else m }
                    .padding(16.dp),
            ) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) { Icon(a.icon, null, tint = MaterialTheme.colorScheme.primary) }
                Text(stringResource(a.name), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
                Text(
                    stringResource(a.desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, modifier = Modifier.weight(1f),
                )
                if (!a.ready) Pill(stringResource(R.string.coming_soon), Modifier.alpha(0.8f))
            }
        }
    }
}

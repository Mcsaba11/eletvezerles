package hu.lifeos.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.ui.theme.Noc
import java.time.LocalDate

data class DockTab(val route: String, val label: Int, val icon: ImageVector)

val dockTabs = listOf(
    DockTab("today", R.string.tab_today, Icons.Outlined.CalendarToday),
    DockTab("timeline", R.string.tab_timeline, Icons.Outlined.ShowChart),
    DockTab("apps", R.string.tab_apps, Icons.Outlined.Apps),
    DockTab("patterns", R.string.tab_patterns, Icons.Outlined.BubbleChart),
    DockTab("sensei", R.string.tab_sensei, Icons.Outlined.Psychology),
)

/** Az app felső sávja: logó, név, állapot, SOS és a profil. */
@Composable
fun TopHeader(
    status: String, statusColor: Color, initials: String,
    onSos: (() -> Unit)?, onProfile: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null,
) {
    val thump = rememberThump()
    Row(
        modifier.fillMaxWidth().background(Noc.canvas).statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Noc.card).border(1.dp, Noc.stroke, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Spa, null, Modifier.size(20.dp), tint = Noc.cyan) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = Noc.text)
                Spacer(Modifier.width(8.dp))
                StatusChip(status, statusColor)
            }
            Text(subtitle ?: (stringResource(R.string.today) + ", " + Fmt.dayFull(LocalDate.now())), style = MaterialTheme.typography.bodySmall, color = Noc.text2, maxLines = 1)
        }
        if (onSos != null) Row(
            Modifier.clip(RoundedCornerShape(50)).background(Noc.sos.copy(alpha = 0.14f)).border(1.dp, Noc.sos.copy(alpha = 0.45f), RoundedCornerShape(50))
                .clickable { thump(); onSos() }.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Spa, null, Modifier.size(15.dp), tint = Noc.sos)
            Spacer(Modifier.width(5.dp))
            Text(stringResource(R.string.sos), style = MaterialTheme.typography.labelLarge, color = Noc.sos)
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(Noc.card).border(1.5.dp, Noc.cyan.copy(alpha = 0.7f), CircleShape).clickable(onClick = onProfile),
            contentAlignment = Alignment.Center,
        ) { Text(initials, style = MaterialTheme.typography.labelLarge, color = Noc.cyan) }
    }
}

/** Lebegő beviteli kapszula a dokk fölött („Egy beviteli mező…” + mikrofon). */
@Composable
fun CapturePill(onClick: () -> Unit, onMic: () -> Unit, modifier: Modifier = Modifier) {
    val tick = rememberTick()
    Row(
        modifier.shadow(18.dp, RoundedCornerShape(50), ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(50)).background(Noc.cardElevated.copy(alpha = 0.96f)).border(1.dp, Noc.cyan.copy(alpha = 0.45f), RoundedCornerShape(50))
            .clickable { tick(); onClick() }.padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.AddCircleOutline, null, Modifier.size(20.dp), tint = Noc.cyan)
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.capture_pill), style = MaterialTheme.typography.titleSmall, color = Noc.text)
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(Noc.well).clickable { tick(); onMic() },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Mic, stringResource(R.string.voice_input), Modifier.size(18.dp), tint = Noc.text2) }
    }
}

/** Lebegő, üvegszerű alsó navigációs dokk. */
@Composable
fun FloatingDock(current: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val tick = rememberTick()
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)
            .shadow(24.dp, RoundedCornerShape(26.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(26.dp)).background(Noc.card.copy(alpha = 0.97f)).border(1.dp, Noc.stroke, RoundedCornerShape(26.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        dockTabs.forEach { t ->
            val on = t.route == current
            val col by animateColorAsState(if (on) Noc.cyan else Noc.text3, label = "dock")
            val bar by animateDpAsState(if (on) 18.dp else 0.dp, label = "bar")
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).clickable { if (!on) { tick(); onSelect(t.route) } }.padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.width(bar).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Brush.horizontalGradient(listOf(Noc.cyan, Noc.sapphire))))
                Spacer(Modifier.height(4.dp))
                Icon(t.icon, null, Modifier.size(22.dp), tint = col)
                Text(stringResource(t.label), style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold), color = col, maxLines = 1)
            }
        }
    }
}

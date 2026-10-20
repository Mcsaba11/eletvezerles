package hu.lifeos.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.HotTub
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.PhonelinkOff
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Shower
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.SmokingRooms
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsMartialArts
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A Satori-tartalomban (JSON) lévő ikonnevek (Material Symbols) leképezése a Compose ikonjaira.
 * Ismeretlen név esetén egy semleges ikon jelenik meg, így az új tartalom sosem okoz hibát.
 */
fun iconFor(name: String): ImageVector = when (name) {
    "air" -> Icons.Outlined.Waves
    "auto_awesome" -> Icons.Outlined.AutoAwesome
    "back_hand" -> Icons.Outlined.PanTool
    "balance" -> Icons.Outlined.Tune
    "beach_access" -> Icons.Outlined.BeachAccess
    "bedtime" -> Icons.Outlined.NightsStay
    "bolt" -> Icons.Outlined.FlashOn
    "call" -> Icons.Outlined.Call
    "center_focus_strong" -> Icons.Outlined.CenterFocusStrong
    "check_circle", "task_alt" -> Icons.Outlined.CheckCircle
    "checklist" -> Icons.Outlined.AssignmentTurnedIn
    "cookie" -> Icons.Outlined.Cake
    "directions_run" -> Icons.Outlined.DirectionsRun
    "directions_walk" -> Icons.Outlined.DirectionsWalk
    "diversity_1" -> Icons.Outlined.Group
    "edit_note" -> Icons.Outlined.Edit
    "egg", "nutrition" -> Icons.Outlined.Restaurant
    "favorite" -> Icons.Outlined.Favorite
    "fitness_center" -> Icons.Outlined.FitnessCenter
    "hearing" -> Icons.Outlined.Hearing
    "hot_tub" -> Icons.Outlined.HotTub
    "liquor" -> Icons.Outlined.LocalBar
    "meeting_room" -> Icons.Outlined.MeetingRoom
    "park" -> Icons.Outlined.Park
    "phonelink_off" -> Icons.Outlined.PhonelinkOff
    "psychology" -> Icons.Outlined.Psychology
    "restart_alt" -> Icons.Outlined.Replay
    "self_improvement" -> Icons.Outlined.SelfImprovement
    "shield" -> Icons.Outlined.Shield
    "shower" -> Icons.Outlined.Shower
    "smartphone" -> Icons.Outlined.Smartphone
    "smoking_rooms" -> Icons.Outlined.SmokingRooms
    "spa" -> Icons.Outlined.Spa
    "sports_martial_arts" -> Icons.Outlined.SportsMartialArts
    "surfing" -> Icons.Outlined.Waves
    "terrain" -> Icons.Outlined.Terrain
    "thunderstorm" -> Icons.Outlined.FlashOn
    "timer" -> Icons.Outlined.Timer
    "touch_app" -> Icons.Outlined.TouchApp
    "trending_down" -> Icons.Outlined.TrendingDown
    "visibility" -> Icons.Outlined.Visibility
    "visibility_off" -> Icons.Outlined.VisibilityOff
    "warning" -> Icons.Outlined.Warning
    "water_drop" -> Icons.Outlined.Opacity
    "wb_sunny" -> Icons.Outlined.WbSunny
    "coffee" -> Icons.Outlined.LocalCafe
    "fastfood" -> Icons.Outlined.Fastfood
    "casino" -> Icons.Outlined.Casino
    "shopping_cart" -> Icons.Outlined.ShoppingCart
    "sports_esports" -> Icons.Outlined.SportsEsports
    "eco" -> Icons.Outlined.Eco
    "star" -> Icons.Outlined.Star
    "restaurant" -> Icons.Outlined.Restaurant
    else -> Icons.Outlined.Spa
}

/** A saját szokásokhoz választható ikonok. */
val CUSTOM_HABIT_ICONS = listOf("star", "favorite", "bolt", "coffee", "fastfood", "smartphone", "casino", "shopping_cart", "sports_esports", "eco", "liquor", "fitness_center")

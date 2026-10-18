package hu.lifeos.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import hu.lifeos.app.satori.ThemeColors

private val DarkCore = darkColorScheme(
    primary = Color(0xFF88D7AD),
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF0E4F33),
    onPrimaryContainer = Color(0xFFA4F3C8),
    secondary = Color(0xFFB3CCBE),
    onSecondary = Color(0xFF1F352A),
    secondaryContainer = Color(0xFF354B40),
    onSecondaryContainer = Color(0xFFCFE9D9),
    tertiary = Color(0xFFE9C349),
    onTertiary = Color(0xFF3C2F00),
    tertiaryContainer = Color(0xFF574500),
    onTertiaryContainer = Color(0xFFFFE088),
    background = Color(0xFF101314),
    onBackground = Color(0xFFE1E3E0),
    surface = Color(0xFF101314),
    onSurface = Color(0xFFE1E3E0),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFBFC9C1),
    surfaceContainerLowest = Color(0xFF0B0E0F),
    surfaceContainerLow = Color(0xFF181C1C),
    surfaceContainer = Color(0xFF1C2020),
    surfaceContainerHigh = Color(0xFF262B2A),
    surfaceContainerHighest = Color(0xFF313635),
    outline = Color(0xFF89938C),
    outlineVariant = Color(0xFF404943),
    error = Color(0xFFFFB4AB),
)

private val LightCore = lightColorScheme(
    primary = Color(0xFF1B6B48),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA4F3C8),
    onPrimaryContainer = Color(0xFF002113),
    secondary = Color(0xFF4D6357),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFE9D9),
    onSecondaryContainer = Color(0xFF0A1F15),
    tertiary = Color(0xFF735C00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE088),
    onTertiaryContainer = Color(0xFF231B00),
    background = Color(0xFFF7F8F4),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFF7F8F4),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFDCE5DC),
    onSurfaceVariant = Color(0xFF404943),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF1F3EF),
    surfaceContainer = Color(0xFFEBEEE9),
    surfaceContainerHigh = Color(0xFFE5E8E3),
    surfaceContainerHighest = Color(0xFFDFE3DE),
    outline = Color(0xFF707971),
    outlineVariant = Color(0xFFC0C9C0),
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val base = Typography()
val AppTypography = base.copy(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
)

@Composable
fun AppTheme(darkMode: String = "system", content: @Composable () -> Unit) {
    val dark = when (darkMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val scheme = if (dark) DarkCore else LightCore
    SystemBars(dark)
    MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
}

@Composable
private fun SystemBars(dark: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
}

fun hex(s: String): Color = Color(android.graphics.Color.parseColor(s))

/** A Satori témák (zen, harcos, jóga…) mindig sötét, saját színvilágú sémát kapnak. */
fun satoriScheme(c: ThemeColors): ColorScheme = darkColorScheme(
    primary = hex(c.primary),
    onPrimary = Color(0xFF2A0F0A),
    primaryContainer = hex(c.primaryContainer),
    onPrimaryContainer = hex(c.onPrimaryContainer),
    secondary = hex(c.secondary),
    onSecondary = Color(0xFF0B2014),
    secondaryContainer = hex(c.secondaryContainer),
    onSecondaryContainer = hex(c.onSecondaryContainer),
    tertiary = hex(c.tertiary),
    onTertiary = Color(0xFF241A00),
    tertiaryContainer = hex(c.tertiaryContainer),
    onTertiaryContainer = hex(c.onTertiaryContainer),
    outline = hex(c.outline),
    outlineVariant = hex(c.outlineVariant),
    background = Color(0xFF111316),
    onBackground = Color(0xFFEDE7E3),
    surface = Color(0xFF111316),
    onSurface = Color(0xFFEDE7E3),
    surfaceVariant = Color(0xFF2A2D31),
    onSurfaceVariant = Color(0xFFC9C2BE),
    surfaceContainerLowest = Color(0xFF0C0E10),
    surfaceContainerLow = Color(0xFF181B1E),
    surfaceContainer = Color(0xFF1D2023),
    surfaceContainerHigh = Color(0xFF272A2E),
    surfaceContainerHighest = Color(0xFF323539),
)

object StatusColors {
    val ok = Color(0xFF7FD6A4)
    val warn = Color(0xFFE9C349)
    val bad = Color(0xFFFF8A7A)
}

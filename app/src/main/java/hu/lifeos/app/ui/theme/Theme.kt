package hu.lifeos.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import hu.lifeos.app.R
import hu.lifeos.app.satori.ThemeColors

/*
 * „Nocturne” arculat: éjkék alap, elektromos cián jel, borostyán meleg kiemelés.
 * A sötét mód az alap; világos változat is van ugyanezekkel a szerepekkel.
 */

val Jakarta = FontFamily(
    Font(R.font.jakarta_regular, FontWeight.Normal),
    Font(R.font.jakarta_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold),
    Font(R.font.jakarta_extrabold, FontWeight.ExtraBold),
)

/** Az arculat különleges színei, amelyek nem férnek bele a Material szerepekbe. */
@Immutable
data class Palette(
    val dark: Boolean,
    val abyss: Color,
    val canvas: Color,
    val card: Color,
    val cardElevated: Color,
    val stroke: Color,
    val sheen: Color,
    val well: Color,
    val cyan: Color,
    val sapphire: Color,
    val ice: Color,
    val amber: Color,
    val coral: Color,
    val sos: Color,
    val good: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val violet: Color,
)

val DarkPalette = Palette(
    dark = true,
    abyss = Color(0xFF060913), canvas = Color(0xFF0A0F1D), card = Color(0xFF161F36), cardElevated = Color(0xFF1E293B),
    stroke = Color(0xA62A3859), sheen = Color(0x14FFFFFF), well = Color(0xFF0F172A),
    cyan = Color(0xFF00F2FE), sapphire = Color(0xFF38BDF8), ice = Color(0xFF93C5FD), amber = Color(0xFFF59E0B),
    coral = Color(0xFFFB7185), sos = Color(0xFFF43F5E), good = Color(0xFF34D399),
    text = Color(0xFFF8FAFC), text2 = Color(0xFF94A3B8), text3 = Color(0xFF64748B), violet = Color(0xFFA78BFA),
)

val LightPalette = Palette(
    dark = false,
    abyss = Color(0xFFE6ECF5), canvas = Color(0xFFF3F6FB), card = Color(0xFFFFFFFF), cardElevated = Color(0xFFFFFFFF),
    stroke = Color(0xFFDCE3EE), sheen = Color(0x00FFFFFF), well = Color(0xFFE9EEF6),
    cyan = Color(0xFF0891B2), sapphire = Color(0xFF0284C7), ice = Color(0xFF2563EB), amber = Color(0xFFD97706),
    coral = Color(0xFFE11D48), sos = Color(0xFFE11D48), good = Color(0xFF059669),
    text = Color(0xFF0F172A), text2 = Color(0xFF475569), text3 = Color(0xFF94A3B8), violet = Color(0xFF7C3AED),
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

/** Rövid elérés: `Noc.cyan` stb. a kompozícióban. */
val Noc: Palette @Composable get() = LocalPalette.current

private fun schemeFor(p: Palette): ColorScheme = if (p.dark) darkColorScheme(
    primary = p.cyan, onPrimary = Color(0xFF060913),
    primaryContainer = Color(0xFF0B3A4A), onPrimaryContainer = Color(0xFFB8FBFF),
    secondary = p.sapphire, onSecondary = Color(0xFF00354A),
    secondaryContainer = Color(0xFF13324A), onSecondaryContainer = Color(0xFFC4E7FF),
    tertiary = p.amber, onTertiary = Color(0xFF2A1700),
    tertiaryContainer = Color(0xFF3A2A12), onTertiaryContainer = Color(0xFFFFDDB8),
    background = p.canvas, onBackground = p.text,
    surface = p.canvas, onSurface = p.text,
    surfaceVariant = Color(0xFF243049), onSurfaceVariant = p.text2,
    surfaceContainerLowest = p.abyss, surfaceContainerLow = p.card, surfaceContainer = Color(0xFF1A2440),
    surfaceContainerHigh = p.cardElevated, surfaceContainerHighest = Color(0xFF2A3859),
    outline = Color(0xFF3A4A66), outlineVariant = Color(0xFF243049),
    error = p.coral, onError = Color(0xFF3A0010), errorContainer = Color(0xFF4A1020), onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = p.text, inverseOnSurface = p.canvas,
) else lightColorScheme(
    primary = p.cyan, onPrimary = Color.White,
    primaryContainer = Color(0xFFCFF7FB), onPrimaryContainer = Color(0xFF053B47),
    secondary = p.sapphire, onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEEFC), onSecondaryContainer = Color(0xFF07344F),
    tertiary = p.amber, onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEBC7), onTertiaryContainer = Color(0xFF4A2A00),
    background = p.canvas, onBackground = p.text,
    surface = p.canvas, onSurface = p.text,
    surfaceVariant = Color(0xFFE2E8F0), onSurfaceVariant = p.text2,
    surfaceContainerLowest = p.abyss, surfaceContainerLow = p.card, surfaceContainer = Color(0xFFF7F9FC),
    surfaceContainerHigh = Color(0xFFEFF3F8), surfaceContainerHighest = Color(0xFFE2E8F0),
    outline = Color(0xFFCBD5E1), outlineVariant = Color(0xFFE2E8F0),
    error = p.coral, onError = Color.White, errorContainer = Color(0xFFFFE4E6), onErrorContainer = Color(0xFF4C0519),
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private fun ts(size: Int, weight: FontWeight, line: Int, spacing: Double = 0.0) =
    TextStyle(fontFamily = Jakarta, fontSize = size.sp, fontWeight = weight, lineHeight = line.sp, letterSpacing = spacing.sp)

val AppTypography = Typography(
    displayLarge = ts(44, FontWeight.Bold, 52, -1.2),
    displayMedium = ts(36, FontWeight.Bold, 44, -1.0),
    displaySmall = ts(30, FontWeight.Bold, 38, -0.75),
    headlineLarge = ts(26, FontWeight.Bold, 34, -0.5),
    headlineMedium = ts(22, FontWeight.Bold, 28, -0.33),
    headlineSmall = ts(19, FontWeight.SemiBold, 26, -0.19),
    titleLarge = ts(19, FontWeight.Bold, 26, -0.19),
    titleMedium = ts(17, FontWeight.SemiBold, 24, -0.08),
    titleSmall = ts(15, FontWeight.SemiBold, 20),
    bodyLarge = ts(16, FontWeight.Normal, 24),
    bodyMedium = ts(14, FontWeight.Normal, 20),
    bodySmall = ts(13, FontWeight.Normal, 18),
    labelLarge = ts(14, FontWeight.SemiBold, 18, 0.14),
    labelMedium = ts(12, FontWeight.SemiBold, 16, 0.24),
    labelSmall = ts(10, FontWeight.Bold, 14, 0.6),
)

@Composable
fun AppTheme(darkMode: String = "dark", content: @Composable () -> Unit) {
    val dark = when (darkMode) {
        "light" -> false
        "system" -> isSystemInDarkTheme()
        else -> true
    }
    val palette = if (dark) DarkPalette else LightPalette
    SystemBars(dark)
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = schemeFor(palette), typography = AppTypography, shapes = AppShapes, content = content)
    }
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

/**
 * A Satori a közös arculatot használja; a választott téma (zen, harcos…) csak a hangulatot,
 * a szövegeket és a fotókat adja. Így minden modul egységes marad.
 */
@Composable
fun satoriScheme(@Suppress("UNUSED_PARAMETER") c: ThemeColors): ColorScheme = MaterialTheme.colorScheme

object StatusColors {
    val ok = Color(0xFF34D399)
    val warn = Color(0xFFF59E0B)
    val bad = Color(0xFFFB7185)
}

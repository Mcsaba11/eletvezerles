package hu.lifeos.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import hu.lifeos.app.ui.theme.Noc
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// ---------------- Rezgés ----------------

/** Rövid, finom tapintható visszajelzés (koppintás, kipipálás). */
@Composable
fun rememberTick(): () -> Unit {
    val h = LocalHapticFeedback.current
    return remember(h) { { h.performHapticFeedback(HapticFeedbackType.TextHandleMove) } }
}

/** Erősebb visszajelzés (mentés, napzárás, SOS). */
@Composable
fun rememberThump(): () -> Unit {
    val h = LocalHapticFeedback.current
    return remember(h) { { h.performHapticFeedback(HapticFeedbackType.LongPress) } }
}

// ---------------- Kártyák ----------------

/**
 * Az arculat alap kártyája: éjkék felület, 1 px finom szegély, felső fényél,
 * koppintásra enyhén benyomódik és rezdül.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = Color.Unspecified,
    glow: Color? = null,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = Noc
    val bg = if (color == Color.Unspecified) p.card else color
    val shape = RoundedCornerShape(24.dp)
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 0.985f else 1f, spring(stiffness = 900f), label = "press")
    val tick = rememberTick()
    Column(
        modifier
            .fillMaxWidth()
            .scale(sc)
            .clip(shape)
            .background(bg)
            .drawBehind {
                if (glow != null) drawRect(Brush.radialGradient(listOf(glow.copy(alpha = 0.16f), Color.Transparent), center = Offset(size.width * 0.9f, 0f), radius = size.width * 0.8f))
                drawLine(p.sheen, Offset(24f, 0.5f), Offset(size.width - 24f, 0.5f), 1.5f)
            }
            .border(1.dp, if (glow != null) glow.copy(alpha = 0.45f) else p.stroke, shape)
            .let { if (onClick != null) it.clickable(interactionSource = src, indication = null) { tick(); onClick() } else it }
            .padding(padding),
        content = content,
    )
}

/** Kártyafejléc: kis ikon-négyzet, nagybetűs címke, jobb oldalon kiegészítő. */
@Composable
fun CardHeader(
    icon: ImageVector, title: String, modifier: Modifier = Modifier, tint: Color = Color.Unspecified,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val c = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(c.copy(alpha = 0.12f)).border(1.dp, c.copy(alpha = 0.25f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, Modifier.size(17.dp), tint = c) }
        Spacer(Modifier.width(10.dp))
        Text(title.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelMedium, color = Noc.text2, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailing()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = Noc.text3,
        modifier = modifier.padding(start = 4.dp, top = 10.dp, bottom = 6.dp),
    )
}

@Composable
fun MenuRow(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit, trailing: @Composable () -> Unit = {
    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Noc.text3)
}) {
    val tick = rememberTick()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { tick(); onClick() }.padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Noc.well).border(1.dp, Noc.stroke, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Noc.text2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        trailing()
    }
}

/** Kis kapszula-címke (állapot, kategória). */
@Composable
fun Pill(
    text: String, modifier: Modifier = Modifier,
    color: Color = Color.Unspecified, textColor: Color = Color.Unspecified,
) {
    val tc = if (textColor == Color.Unspecified) MaterialTheme.colorScheme.primary else textColor
    val bg = if (color == Color.Unspecified) tc.copy(alpha = 0.12f) else color
    Text(
        text, style = MaterialTheme.typography.labelMedium, color = tc, maxLines = 1,
        modifier = modifier.clip(RoundedCornerShape(50)).background(bg).border(1.dp, tc.copy(alpha = 0.28f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Állapotjelző kapszula pöttyel (pl. „Mederben”). */
@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.12f)).border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
    }
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = Noc.text2, modifier = modifier.padding(vertical = 8.dp))
}

@Composable
fun VSpace(h: Int) = Spacer(Modifier.height(h.dp))

// ---------------- Gombok ----------------

/** Elsődleges, ciánnal töltött kapszulagomb. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = Color.Unspecified, leading: ImageVector? = null) {
    val c = if (color == Color.Unspecified) Noc.cyan else color
    val thump = rememberThump()
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 0.97f else 1f, label = "btn")
    Row(
        modifier.scale(sc).height(46.dp).clip(RoundedCornerShape(14.dp))
            .background(if (enabled) Brush.horizontalGradient(listOf(c, c.copy(alpha = 0.82f))) else Brush.horizontalGradient(listOf(Noc.well, Noc.well)))
            .clickable(enabled = enabled, interactionSource = src, indication = null) { thump(); onClick() }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        val fg = if (enabled) Color(0xFF060913) else Noc.text3
        if (leading != null) { Icon(leading, null, Modifier.size(18.dp), tint = fg); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1)
    }
}

/** Másodlagos, sötét üvegszerű gomb. */
@Composable
fun TonalButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, leading: ImageVector? = null, tint: Color = Color.Unspecified) {
    val tc = if (tint == Color.Unspecified) Noc.text else tint
    val tick = rememberTick()
    Row(
        modifier.height(46.dp).clip(RoundedCornerShape(14.dp)).background(Noc.cardElevated).border(1.dp, Noc.stroke, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) { tick(); onClick() }.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (leading != null) { Icon(leading, null, Modifier.size(18.dp), tint = tc); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (enabled) tc else Noc.text3, maxLines = 1)
    }
}

// ---------------- Szegmentált váltó ----------------

/** Besüllyesztett sín, rajta a kiválasztott elem csúszik (natív szegmentált vezérlő). */
@Composable
fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val idx = options.indexOf(selected)
    val tick = rememberTick()
    BoxWithConstraints(modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(50)).background(Noc.well).border(1.dp, Noc.stroke, RoundedCornerShape(50)).padding(3.dp)) {
        val w = maxWidth / options.size
        val x by androidx.compose.animation.core.animateDpAsState(w * idx.coerceAtLeast(0), spring(dampingRatio = 0.8f, stiffness = 500f), label = "seg")
        if (idx >= 0) Box(Modifier.offset(x = x).width(w).fillMaxHeight().clip(RoundedCornerShape(50)).background(Noc.cardElevated).border(1.dp, Noc.cyan.copy(alpha = 0.35f), RoundedCornerShape(50)))
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { i, o ->
                val col by animateColorAsState(if (i == idx) Noc.cyan else Noc.text2, tween(200), label = "segc")
                Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(50)).clickable { tick(); onSelect(o) }, contentAlignment = Alignment.Center) {
                    Text(label(o), style = MaterialTheme.typography.labelMedium, color = col, maxLines = 1)
                }
            }
        }
    }
}

// ---------------- Gyűrű és sávok ----------------

/** Mérőgyűrű sötét vájatban, fénylő ívvel. */
@Composable
fun MetricRing(
    fraction: Float, color: Color, modifier: Modifier = Modifier, stroke: Dp = 7.dp,
    center: @Composable () -> Unit = {},
) {
    val anim by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(900), label = "ring")
    val well = Noc.well
    val sw = with(LocalDensity.current) { stroke.toPx() }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSizeCompat()) {
            val r = size.minDimension / 2 - sw
            val tl = Offset(center.x - r, center.y - r)
            val sz = Size(r * 2, r * 2)
            drawArc(well, 0f, 360f, false, tl, sz, style = Stroke(sw))
            if (anim > 0f) {
                drawArc(color.copy(alpha = 0.25f), -90f, 360f * anim, false, tl, sz, style = Stroke(sw * 2.2f, cap = StrokeCap.Round))
                drawArc(color, -90f, 360f * anim, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
            }
        }
        center()
    }
}

/** Vékony, lekerekített sáv sötét vájatban. */
@Composable
fun Meter(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val anim by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700), label = "meter")
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(Noc.well)) {
        Box(
            Modifier.fillMaxWidth(anim).fillMaxHeight().clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.75f), color)))
        )
    }
}

/** Nagy, táblázatos számjegyű érték (KPI), mellette kisebb egység. */
@Composable
fun BigValue(value: String, unit: String = "", color: Color = Color.Unspecified, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(value, style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"), color = if (color == Color.Unspecified) Noc.text else color)
        if (unit.isNotBlank()) {
            Spacer(Modifier.width(6.dp))
            Text(unit, style = MaterialTheme.typography.titleSmall, color = Noc.text2, modifier = Modifier.padding(bottom = 5.dp))
        }
    }
}

/** Kis KPI-csempe: címke, érték, alcím. */
@Composable
fun KpiTile(label: String, value: String, sub: String = "", modifier: Modifier = Modifier, accent: Color = Color.Unspecified, icon: ImageVector? = null, onClick: (() -> Unit)? = null) {
    val a = if (accent == Color.Unspecified) Noc.cyan else accent
    AppCard(modifier, onClick = onClick, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(icon, null, Modifier.size(16.dp), tint = a); Spacer(Modifier.width(6.dp)) }
            Text(label.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = Noc.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        VSpace(6)
        Text(value, style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"), color = Noc.text, maxLines = 1)
        if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = a, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** Lehúzó-fogantyú a lapok tetején. */
@Composable
fun SheetHandle() {
    Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Noc.text3))
    }
}

private fun Modifier.matchParentSizeCompat(): Modifier = this.then(Modifier.fillMaxWidth().fillMaxHeight())

object Fmt {
    val hu: Locale = Locale.getDefault()
    val time: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun dayLong(d: LocalDate): String =
        DateTimeFormatter.ofPattern("MMMM d., EEEE", hu).format(d).replaceFirstChar { it.titlecase(hu) }

    fun dayShort(d: LocalDate): String = DateTimeFormatter.ofPattern("MMM d.", hu).format(d)

    fun dayFull(d: LocalDate): String = DateTimeFormatter.ofPattern("yyyy. MMM d.", hu).format(d)

    fun monthYear(d: LocalDate): String =
        DateTimeFormatter.ofPattern("yyyy. MMMM", hu).format(d).replaceFirstChar { it.titlecase(hu) }

    fun weekdayShort(dow: java.time.DayOfWeek): String = dow.getDisplayName(TextStyle.SHORT, hu).take(3).replaceFirstChar { it.titlecase(hu) }

    fun money(v: Double): String {
        val s = String.format(hu, "%,.0f", v).replace(',', ' ').replace(' ', ' ')
        return "$s Ft"
    }
}


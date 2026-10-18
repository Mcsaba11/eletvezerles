package hu.lifeos.app.satori.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.satori.SatoriLogic
import hu.lifeos.app.satori.SatoriViewModel
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.SectionLabel
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.iconFor
import hu.lifeos.app.ui.theme.StatusColors
import java.time.LocalDate

@Composable
fun BalanceTab(vm: SatoriViewModel, modifier: Modifier) {
    val st by vm.state.collectAsState()
    val c = vm.content
    var step by rememberSaveable { mutableIntStateOf(0) }
    var filter by rememberSaveable { mutableStateOf("all") }
    val open = remember { mutableStateOf(setOf<String>()) }
    val today = SatoriLogic.today()
    val done = st.merleg[today].orEmpty()
    val week = (0..6).count { i -> st.merleg[LocalDate.now().minusDays(i.toLong()).toString()].orEmpty().isNotEmpty() }

    LazyColumn(
        modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(stringResource(R.string.satori_balance_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.satori_balance_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            val s = c.mzSteps[step]
            AppCard(color = MaterialTheme.colorScheme.surfaceContainer) {
                Seesaw(angle = s.a, leftOn = s.w.getOrElse(0) { 0 } == 1, rightOn = s.w.getOrElse(1) { 0 } == 1,
                    leftLabel = stringResource(R.string.satori_pleasure), rightLabel = stringResource(R.string.satori_pain))
                VSpace(8)
                Text("${step + 1}/${c.mzSteps.size} · ${s.t}", style = MaterialTheme.typography.titleSmall)
                VSpace(4)
                Text(s.x, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                VSpace(8)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (step > 0) TextButton(onClick = { step-- }) { Text(stringResource(R.string.back)) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { step = (step + 1) % c.mzSteps.size }) {
                        Text(stringResource(if (step == c.mzSteps.lastIndex) R.string.again else R.string.next))
                    }
                }
            }
        }
        item {
            SectionLabel(stringResource(R.string.satori_toolbox))
            Text(stringResource(R.string.satori_toolbox_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            VSpace(6)
            Text(stringResource(R.string.satori_toolbox_stats, done.size, week), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            VSpace(6)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all" to R.string.filter_all, "quick" to R.string.satori_filter_quick, "daily" to R.string.satori_filter_daily).forEach { (id, l) ->
                    FilterChip(filter == id, { filter = id }, label = { Text(stringResource(l)) })
                }
            }
        }
        items(c.mzTools.filter { filter == "all" || (if (filter == "quick") it.quick else it.daily) }, key = { it.id }) { t ->
            val isOpen = t.id in open.value
            val isDone = t.id in done
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .animateContentSize(),
            ) {
                Row(
                    Modifier.fillMaxWidth().clickable { open.value = if (isOpen) open.value - t.id else open.value + t.id }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                        Icon(iconFor(t.ic), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.n, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(t.time + if (isDone) " · " + stringResource(R.string.satori_done_today) else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val evColor = when (t.ev.getOrNull(0)) { "e1" -> StatusColors.ok; "e2" -> MaterialTheme.colorScheme.secondary; "e3" -> StatusColors.warn; else -> MaterialTheme.colorScheme.outline }
                    Text(t.ev.getOrElse(1) { "" }, style = MaterialTheme.typography.labelSmall, color = evColor,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(evColor.copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 3.dp))
                }
                if (isOpen) Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                    Text(stringResource(R.string.satori_how), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
                    Text(t.how, style = MaterialTheme.typography.bodyMedium)
                    VSpace(8)
                    Text(stringResource(R.string.satori_what_we_know), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
                    Text(t.sci, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (t.warn.isNotBlank()) {
                        VSpace(8)
                        Text(stringResource(R.string.satori_careful), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(t.warn, style = MaterialTheme.typography.bodyMedium)
                    }
                    VSpace(10)
                    if (isDone) OutlinedButton(onClick = { vm.toggleMerleg(t.id) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.satori_did_it_done)) }
                    else FilledTonalButton(onClick = { vm.toggleMerleg(t.id) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.satori_did_it)) }
                }
            }
        }
        item { SectionLabel(stringResource(R.string.satori_myths)) }
        items(c.mzMyths) { m ->
            AppCard {
                Text(m.getOrElse(0) { "" }, style = MaterialTheme.typography.titleSmall)
                VSpace(4)
                Text(m.getOrElse(1) { "" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            AppCard(color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                Text(stringResource(R.string.satori_safety_title), style = MaterialTheme.typography.titleSmall)
                VSpace(4)
                Text(stringResource(R.string.satori_safety_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                VSpace(10)
                Text(stringResource(R.string.satori_sources_title), style = MaterialTheme.typography.titleSmall)
                VSpace(4)
                Text(stringResource(R.string.satori_sources_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Seesaw(angle: Float, leftOn: Boolean, rightOn: Boolean, leftLabel: String, rightLabel: String) {
    val a by animateFloatAsState(angle, spring(dampingRatio = 0.45f, stiffness = 60f), label = "beam")
    val lw by animateFloatAsState(if (leftOn) 1f else 0f, label = "lw")
    val rw by animateFloatAsState(if (rightOn) 1f else 0f, label = "rw")
    val beam = MaterialTheme.colorScheme.onSurfaceVariant
    val pleasure = MaterialTheme.colorScheme.tertiary
    val pain = MaterialTheme.colorScheme.primary
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(leftLabel, style = MaterialTheme.typography.labelSmall, color = pleasure, modifier = Modifier.weight(1f))
            Text(rightLabel, style = MaterialTheme.typography.labelSmall, color = pain)
        }
        Canvas(Modifier.fillMaxWidth().height(130.dp)) {
            val cx = size.width / 2
            val pivotY = size.height * 0.62f
            // talapzat
            val tri = Path().apply {
                moveTo(cx, pivotY); lineTo(cx - 26f, size.height - 6f); lineTo(cx + 26f, size.height - 6f); close()
            }
            drawPath(tri, beam.copy(alpha = 0.5f))
            rotate(a, pivot = Offset(cx, pivotY)) {
                val half = size.width * 0.42f
                drawLine(beam, Offset(cx - half, pivotY), Offset(cx + half, pivotY), strokeWidth = 8f, cap = StrokeCap.Round)
                val r = 26f
                drawCircle(pleasure.copy(alpha = 0.25f + 0.55f * lw), r, Offset(cx - half + r, pivotY - r - 4f))
                drawCircle(pain.copy(alpha = 0.25f + 0.55f * rw), r, Offset(cx + half - r, pivotY - r - 4f))
            }
            drawCircle(Color.White.copy(alpha = 0.8f), 6f, Offset(cx, pivotY))
        }
    }
}

package hu.lifeos.app.satori.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.lifeos.app.R
import hu.lifeos.app.satori.SatoriLogic
import hu.lifeos.app.satori.SatoriViewModel
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.CardHeader
import hu.lifeos.app.ui.SectionLabel
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.iconFor
import hu.lifeos.app.ui.theme.StatusColors

@Composable
fun ResultsTab(vm: SatoriViewModel, modifier: Modifier, onCertificate: (Int) -> Unit) {
    val st by vm.state.collectAsState()
    val c = vm.content
    val d = st.cur()
    val idx = SatoriLogic.stageIndex(c, d.streak)
    val nIdx = SatoriLogic.nextStageIndex(c, d.streak)
    val frac = SatoriLogic.progressFraction(c, d.streak)
    val restarter = stringResource(R.string.satori_restarter)
    val days14 = SatoriLogic.last14(d)
    val stateLabels = c.stateDefs.associate { it.id to it.label }

    LazyColumn(
        modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ---------- Rangfejléc ----------
        item {
            AppCard(color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.satori_level_n, idx + 1), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f))
                    Text(
                        if (nIdx != idx) stringResource(R.string.satori_days_to_next, c.stages[nIdx].days - d.streak) else stringResource(R.string.satori_max_short),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(SatoriLogic.stageTitle(c, st, idx, restarter), style = MaterialTheme.typography.headlineMedium)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${d.streak}", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.tertiary)
                    Text(" / ${c.stages[nIdx].days} " + stringResource(R.string.satori_day_word), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                }
                Text("„${SatoriLogic.stageMotto(c, st, idx)}”", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                VSpace(10)
                LinearProgressIndicator(
                    progress = { frac }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.tertiary, trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {},
                )
                VSpace(4)
                Text(stringResource(R.string.satori_pct_done, Math.round(frac * 1000) / 10f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ---------- 14 napos hőtérkép ----------
        item {
            AppCard {
                CardHeader(Icons.Outlined.ShowChart, stringResource(R.string.satori_heatmap))
                VSpace(10)
                for (row in 0..1) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (col in 0..6) {
                            val i = row * 7 + col
                            val (date, s) = days14[i]
                            val color = when (s) {
                                "nyugodt" -> StatusColors.ok
                                "megbotlas" -> StatusColors.bad
                                null -> MaterialTheme.colorScheme.surfaceContainerHighest
                                else -> StatusColors.warn
                            }
                            val isToday = i == 13
                            Box(
                                Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                                    .background(if (s == null) color else color.copy(alpha = 0.3f))
                                    .let { if (isToday) it.border(1.5.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(8.dp)) else it },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${date.dayOfMonth}", style = MaterialTheme.typography.labelSmall, color = if (s == null) MaterialTheme.colorScheme.onSurfaceVariant else color)
                            }
                        }
                    }
                    if (row == 0) VSpace(6)
                }
                VSpace(10)
                val ok = days14.count { it.second == "nyugodt" }
                val fail = days14.count { it.second == "megbotlas" }
                val none = days14.count { it.second == null }
                val wobble = 14 - ok - fail - none
                Text(stringResource(R.string.satori_trend_summary, ok, wobble, fail, none), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SatoriLogic.bestWeek(d)?.let { bw ->
                    VSpace(6)
                    Text(stringResource(R.string.satori_best_week, bw.year, bw.week, bw.count), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }

        // ---------- Mintázat ----------
        SatoriLogic.topRiskyDaypart(d)?.let { risk ->
            item {
                AppCard(color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)) {
                    CardHeader(Icons.Outlined.ShowChart, stringResource(R.string.satori_pattern))
                    VSpace(6)
                    Text(stringResource(R.string.satori_pattern_text, risk.share, c.daypartLabel[risk.part] ?: risk.part), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // ---------- Út-térkép ----------
        item {
            SectionLabel(stringResource(R.string.satori_map))
            val listState = rememberLazyListState()
            LaunchedEffect(idx) { listState.animateScrollToItem((idx - 1).coerceAtLeast(0)) }
            LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                itemsIndexed(c.stages) { i, s ->
                    val earned = d.streak >= s.days
                    val current = i == idx
                    Row(verticalAlignment = Alignment.Top) {
                        if (i > 0) Box(Modifier.padding(top = 14.dp).width(28.dp).height(3.dp).clip(RoundedCornerShape(2.dp))
                            .background(if (earned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHighest))
                        Column(Modifier.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.size(30.dp).clip(CircleShape).background(
                                    when {
                                        current -> MaterialTheme.colorScheme.tertiary
                                        earned -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceContainerLowest
                                    }
                                ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    when { current -> Icons.Outlined.DirectionsWalk; earned -> Icons.Outlined.Check; else -> Icons.Outlined.Lock },
                                    null, Modifier.size(16.dp),
                                    tint = if (current) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                if (earned) SatoriLogic.stageTitle(c, st, i, restarter) else stringResource(R.string.satori_n_days, s.days),
                                style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2,
                                color = if (earned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        // ---------- Mérföldkövek ----------
        item {
            val earnedCount = c.stages.drop(1).count { d.streak >= it.days }
            SectionLabel(stringResource(R.string.satori_milestones, earnedCount, c.stages.size - 1))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                c.stages.drop(1).chunked(2).forEachIndexed { rowI, pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEachIndexed { j, s ->
                            val i = rowI * 2 + j + 1
                            val earned = d.streak >= s.days
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                                    .background(if (earned) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLowest)
                                    .let { if (earned) it.clickable { onCertificate(i) } else it.alpha(0.65f) }
                                    .padding(12.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (earned) Icons.Outlined.CheckCircle else Icons.Outlined.Lock, null, Modifier.size(20.dp),
                                        tint = if (earned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.weight(1f))
                                    Text(stringResource(R.string.satori_n_days, s.days), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                }
                                VSpace(6)
                                if (earned) {
                                    Text(SatoriLogic.stageTitle(c, st, i, restarter), style = MaterialTheme.typography.titleSmall)
                                    Text(SatoriLogic.stageSub(c, st, i, ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                        Icon(Icons.Outlined.EmojiEvents, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
                                        Text(" " + stringResource(R.string.satori_view_cert), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                                    }
                                } else {
                                    Text(stringResource(R.string.satori_locked_level), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(stringResource(R.string.satori_surprise), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(stringResource(R.string.satori_days_left, s.days - d.streak), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // ---------- Ösvény-bontás ----------
        item {
            SectionLabel(stringResource(R.string.satori_breakdown))
            AppCard {
                c.types.forEach { t ->
                    val td = st.data[t.id] ?: return@forEach
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                        Icon(iconFor(t.icon), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Row {
                                Text(t.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text(stringResource(R.string.satori_n_days, td.streak), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                            }
                            LinearProgressIndicator(
                                progress = { SatoriLogic.progressFraction(c, td.streak) }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.secondary, trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {},
                            )
                        }
                    }
                }
            }
        }
        item {
            val legend = listOf(
                StatusColors.ok to (stateLabels["nyugodt"] ?: ""),
                StatusColors.warn to (stateLabels["kisertes"] ?: ""),
                StatusColors.bad to (stateLabels["megbotlas"] ?: ""),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                legend.forEach { (col, l) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(col))
                        Text(" $l", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}


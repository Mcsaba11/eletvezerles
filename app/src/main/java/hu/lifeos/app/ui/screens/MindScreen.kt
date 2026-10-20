package hu.lifeos.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.mods.ui.IconBadge
import hu.lifeos.app.mods.ui.ModListPadding
import hu.lifeos.app.mods.ui.ModScaffold
import hu.lifeos.app.satori.SatoriViewModel
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.CardHeader
import hu.lifeos.app.ui.Fmt
import hu.lifeos.app.ui.KpiTile
import hu.lifeos.app.ui.MainViewModel
import hu.lifeos.app.ui.SectionLabel
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.iconFor
import hu.lifeos.app.ui.theme.Noc
import java.time.DayOfWeek
import java.time.LocalDate

/** Hangulat és légzés: mai hangulat, 5 hetes naptár, összefüggések, légzőgyakorlatok, földelés. */
@Composable
fun MindScreen(vm: MainViewModel, satori: SatoriViewModel, onBreath: (String) -> Unit, onBack: () -> Unit) {
    val mood by vm.todayMood.collectAsState()
    val hist by vm.moodHistory.collectAsState()
    val c = satori.content
    var grounding by remember { mutableStateOf(false) }
    val byDate = hist.associateBy { it.date }
    ModScaffold(stringResource(R.string.mod_mind), stringResource(R.string.mind_kicker), Noc.violet, onBack) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = ModListPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                AppCard(glow = Noc.violet) {
                    CardHeader(Icons.Outlined.Mood, stringResource(R.string.mood_title), tint = Noc.violet)
                    VSpace(10)
                    MoodRow(selected = mood?.mood, onSelect = { vm.setMood(it, null) }, tint = Noc.violet)
                    VSpace(8)
                    EnergyRow(selected = mood?.energy, onSelect = { vm.setMood(null, it) }, tint = Noc.violet)
                }
            }
            // 5 hetes naptár, hétfővel kezdve
            item {
                val today = LocalDate.now()
                val start = today.minusDays(today.dayOfWeek.value - 1L).minusWeeks(4)
                AppCard {
                    Text(stringResource(R.string.mind_calendar), style = MaterialTheme.typography.titleMedium)
                    VSpace(10)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DayOfWeek.values().forEach { d ->
                            Text(Fmt.weekdayShort(d), style = MaterialTheme.typography.labelSmall, color = Noc.text3, modifier = Modifier.weight(1f))
                        }
                    }
                    VSpace(4)
                    (0 until 5).forEach { w ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding2()) {
                            (0 until 7).forEach { i ->
                                val day = start.plusDays(w * 7L + i)
                                val m = byDate[day.toString()]?.mood
                                val future = day.isAfter(today)
                                Box(
                                    Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                                        .background(if (future) Noc.canvas else if (m == null) Noc.well else Noc.violet.copy(alpha = 0.15f + 0.17f * m)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("${day.dayOfMonth}", style = MaterialTheme.typography.labelSmall, color = if (m != null && m >= 4) Noc.text else Noc.text3)
                                }
                            }
                        }
                    }
                }
            }
            item {
                val today = LocalDate.now()
                fun avg(from: Int, to: Int) = hist.filter { h ->
                    val d = runCatching { LocalDate.parse(h.date) }.getOrNull() ?: return@filter false
                    !d.isBefore(today.minusDays(to.toLong())) && !d.isAfter(today.minusDays(from.toLong()))
                }.mapNotNull { it.mood }.let { if (it.isEmpty()) null else it.average() }
                val now7 = avg(0, 6); val prev7 = avg(7, 13)
                val byDow = hist.mapNotNull { h -> h.mood?.let { m -> runCatching { LocalDate.parse(h.date).dayOfWeek }.getOrNull()?.let { it to m } } }
                    .groupBy({ it.first }, { it.second })
                val best = if (byDow.values.sumOf { it.size } >= 10) byDow.maxByOrNull { it.value.average() }?.key else null
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiTile(
                        stringResource(R.string.mind_week_avg), now7?.let { String.format(Fmt.hu, "%.1f", it) } ?: "–",
                        if (now7 != null && prev7 != null) stringResource(R.string.mind_vs_prev, (if (now7 >= prev7) "+" else "") + String.format(Fmt.hu, "%.1f", now7 - prev7)) else "",
                        Modifier.weight(1f), accent = Noc.violet,
                    )
                    KpiTile(stringResource(R.string.mind_best_day), best?.let { Fmt.weekdayShort(it) } ?: "–", stringResource(R.string.mind_best_day_sub), Modifier.weight(1f), accent = Noc.good)
                }
            }
            item { SectionLabel(stringResource(R.string.mind_breath)) }
            items(c.techniqueDefs, key = { it.id }) { t ->
                AppCard(onClick = { onBreath(t.id) }, padding = PaddingValues(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Outlined.Waves, Noc.amber, 36)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.title, style = MaterialTheme.typography.titleSmall)
                            Text(t.sub, style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                        }
                        Text(stringResource(R.string.start_arrow), style = MaterialTheme.typography.labelLarge, color = Noc.amber)
                    }
                }
            }
            item {
                AppCard(onClick = { grounding = !grounding }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Outlined.Spa, Noc.good, 36)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.mind_grounding), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.mind_grounding_sub), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                        }
                    }
                    AnimatedVisibility(grounding) {
                        Column(Modifier.padding2()) {
                            c.grounding.forEach { g ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding2()) {
                                    Text("${g.n}", style = MaterialTheme.typography.titleLarge, color = Noc.good, modifier = Modifier.width(28.dp))
                                    Icon(iconFor(g.icon), null, tint = Noc.text2)
                                    Spacer(Modifier.width(8.dp))
                                    Text(g.text, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.padding2() = this.then(Modifier.padding(top = 6.dp))

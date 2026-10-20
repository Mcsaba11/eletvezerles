package hu.lifeos.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.food.FoodViewModel
import hu.lifeos.app.insight.Insights
import hu.lifeos.app.satori.SatoriLogic
import hu.lifeos.app.satori.SatoriViewModel
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.CardHeader
import hu.lifeos.app.ui.MainViewModel
import hu.lifeos.app.ui.Pill
import hu.lifeos.app.ui.PrimaryButton
import hu.lifeos.app.ui.TonalButton
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.iconFor
import hu.lifeos.app.ui.rememberTick
import hu.lifeos.app.ui.theme.Noc

@Composable
fun SenseiScreen(
    vm: MainViewModel, satori: SatoriViewModel, food: FoodViewModel, contentPadding: PaddingValues,
    onBreath: () -> Unit, onSos: () -> Unit,
) {
    val st by satori.state.collectAsState()
    val fs by food.state.collectAsState()
    val mood by vm.todayMood.collectAsState()
    val todos by vm.todos.collectAsState()
    val c = satori.content
    val th = c.theme(st.theme)
    var topic by rememberSaveable { mutableStateOf<String?>(null) }
    val tick = rememberTick()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(stringResource(R.string.sensei_kicker).uppercase(), style = MaterialTheme.typography.labelSmall, color = Noc.cyan)
            Text(stringResource(R.string.tab_sensei), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.sensei_sub), style = MaterialTheme.typography.bodyMedium, color = Noc.text2)
        }
        item {
            AppCard(glow = Noc.cyan) {
                CardHeader(Icons.Outlined.Psychology, stringResource(R.string.sensei_insight), tint = Noc.cyan)
                VSpace(8)
                Text(Insights.dailyInsight(fs, food.logic, st, c, mood, todos, null), style = MaterialTheme.typography.bodyLarge)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppCard(Modifier.weight(1f), onClick = onBreath, glow = Noc.amber, padding = PaddingValues(14.dp)) {
                    Icon(Icons.Outlined.Spa, null, tint = Noc.amber)
                    VSpace(10)
                    Text(stringResource(R.string.breath_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.breath_card_sub), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                }
                AppCard(Modifier.weight(1f), onClick = onSos, glow = Noc.sos, padding = PaddingValues(14.dp)) {
                    Icon(Icons.Outlined.SelfImprovement, null, tint = Noc.sos)
                    VSpace(10)
                    Text(stringResource(R.string.sos_short_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.sos_short_sub), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
                }
            }
        }
        item {
            AppCard {
                CardHeader(Icons.Outlined.CheckCircle, stringResource(R.string.sensei_task), tint = Noc.good)
                VSpace(8)
                Text(c.mentorTasks[SatoriLogic.dayIndex(c.mentorTasks.size)], style = MaterialTheme.typography.bodyLarge)
            }
        }
        item {
            Text(stringResource(R.string.satori_help_with).uppercase(), style = MaterialTheme.typography.labelSmall, color = Noc.text3, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                c.mentorTopics.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { tp ->
                            val on = topic == tp.id
                            Column(
                                Modifier.weight(1f).height(92.dp).clip(RoundedCornerShape(18.dp)).background(if (on) Noc.cardElevated else Noc.card)
                                    .border(1.dp, if (on) Noc.cyan.copy(alpha = 0.7f) else Noc.stroke, RoundedCornerShape(18.dp))
                                    .clickable { tick(); topic = if (on) null else tp.id }.padding(12.dp),
                            ) {
                                Icon(iconFor(tp.icon), null, Modifier.size(22.dp), tint = if (on) Noc.cyan else Noc.sapphire)
                                Spacer(Modifier.weight(1f))
                                Text(tp.label, style = MaterialTheme.typography.labelLarge, color = Noc.text)
                            }
                        }
                    }
                }
            }
        }
        item {
            AnimatedVisibility(topic != null) {
                val t = topic ?: return@AnimatedVisibility
                AppCard(glow = Noc.cyan) {
                    Text(stringResource(R.string.satori_mentor_says, th.mentorName.ifBlank { stringResource(R.string.tab_sensei) }).uppercase(), style = MaterialTheme.typography.labelSmall, color = Noc.cyan)
                    VSpace(6)
                    Text((c.mentorOpen[st.theme] ?: c.mentorOpen["zen"])?.get(t).orEmpty(), style = MaterialTheme.typography.bodyLarge)
                    VSpace(10)
                    c.mentorSteps[t].orEmpty().forEachIndexed { i, s ->
                        Row(Modifier.padding(bottom = 8.dp)) {
                            Box(Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(Noc.well), contentAlignment = Alignment.Center) {
                                Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = Noc.cyan)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(s, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        }
                    }
                    Text(stringResource(R.string.satori_mentor_close), style = MaterialTheme.typography.bodyMedium, color = Noc.text2)
                    if (t == "hullam" || t == "feszultseg" || t == "nehez") {
                        VSpace(10)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrimaryButton(stringResource(R.string.breath_title), onClick = onBreath)
                            TonalButton(stringResource(R.string.sos), onClick = onSos, tint = Noc.sos)
                        }
                    }
                }
            }
        }
        item {
            val qs = SatoriLogic.themeQuotes(c, st)
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(th.quoteCaption.ifBlank { stringResource(R.string.daily_quote) }, textColor = Noc.amber)
                }
                VSpace(8)
                Text("„${qs[SatoriLogic.dayIndex(qs.size)]}”", style = MaterialTheme.typography.titleMedium, fontStyle = FontStyle.Italic)
            }
        }
        item {
            Text(stringResource(R.string.satori_mentor_disclaimer), style = MaterialTheme.typography.bodySmall, color = Noc.text3, modifier = Modifier.fillMaxWidth())
        }
    }
}

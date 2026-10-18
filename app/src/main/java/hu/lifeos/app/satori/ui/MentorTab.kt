package hu.lifeos.app.satori.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.satori.SatoriLogic
import hu.lifeos.app.satori.SatoriViewModel
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.SectionLabel
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.iconFor

@Composable
fun MentorTab(vm: SatoriViewModel, modifier: Modifier, onSos: () -> Unit) {
    val st by vm.state.collectAsState()
    val c = vm.content
    val th = c.theme(st.theme)
    val d = st.cur()
    var topic by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(th.mentorHeading, style = MaterialTheme.typography.headlineSmall)
            Text(th.mentorSubtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            AppCard(color = MaterialTheme.colorScheme.surfaceContainer) {
                val greet = th.mentorGreeting + if (d.streak > 0) " " + stringResource(R.string.satori_mentor_streak, d.streak) else ""
                Text(greet, style = MaterialTheme.typography.bodyLarge)
                VSpace(12)
                Text(stringResource(R.string.satori_today_thought).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                val qs = SatoriLogic.themeQuotes(c, st)
                Text("„${qs[SatoriLogic.dayIndex(qs.size)]}”", style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
                VSpace(12)
                Text(stringResource(R.string.satori_today_task, c.mentorTasks[SatoriLogic.dayIndex(c.mentorTasks.size)]), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
        item { SectionLabel(stringResource(R.string.satori_help_with)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                c.mentorTopics.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { tp ->
                            val on = topic == tp.id
                            Column(
                                Modifier.weight(1f).height(96.dp).clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                    .let { if (on) it.border(1.5.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(16.dp)) else it }
                                    .clickable { topic = tp.id }.padding(12.dp),
                            ) {
                                Icon(iconFor(tp.icon), null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.secondary)
                                VSpace(6)
                                Text(tp.label, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
        topic?.let { t ->
            item {
                AppCard(color = MaterialTheme.colorScheme.surfaceContainer) {
                    Text(stringResource(R.string.satori_mentor_says, th.mentorName.ifBlank { stringResource(R.string.satori_tab_mentor) }).uppercase(),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    VSpace(6)
                    Text((c.mentorOpen[st.theme] ?: c.mentorOpen["zen"])?.get(t).orEmpty(), style = MaterialTheme.typography.bodyLarge)
                    VSpace(10)
                    c.mentorSteps[t].orEmpty().forEachIndexed { i, s ->
                        Text("${i + 1}. $s", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    VSpace(4)
                    Text(stringResource(R.string.satori_mentor_close), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (t == "hullam" || t == "feszultseg" || t == "nehez") {
                        VSpace(10)
                        Button(
                            onClick = onSos, modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        ) { Text(stringResource(R.string.satori_start_sos)) }
                    }
                }
            }
        }
        item {
            Text(stringResource(R.string.satori_mentor_disclaimer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

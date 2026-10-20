package hu.lifeos.app.mods.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.mods.ModsViewModel
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.CardHeader
import hu.lifeos.app.ui.Meter
import hu.lifeos.app.ui.MetricRing
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.theme.Noc
import java.time.LocalDate

/* A modulok kártyái a kezdőlapon. Üres modulnál rövid útmutatást mutatnak. */

@Composable
private fun Hint(text: String) = Text(text, style = MaterialTheme.typography.bodyMedium, color = Noc.text2)

@Composable
fun SleepHomeCard(vm: ModsViewModel, onOpen: () -> Unit) {
    val d by vm.sleep.state.collectAsState()
    val last = d.nights.maxByOrNull { it.date }
    val target = (d.targetHours * 60).toInt()
    AppCard(onClick = onOpen) {
        CardHeader(Icons.Outlined.Bedtime, stringResource(R.string.mod_sleep), tint = Noc.violet)
        VSpace(8)
        if (last == null || last.date < LocalDate.now().minusDays(1).toString()) {
            Hint(stringResource(R.string.sleep_home_log))
        } else Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(hm(last.minutes()), style = MaterialTheme.typography.headlineSmall)
                val avg = SleepStats.avgMin(SleepStats.recent(d, 7))
                Text(stringResource(R.string.sleep_home_avg, hm(avg)), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
            }
            MetricRing(last.minutes().toFloat() / target, Noc.violet, Modifier.size(54.dp), stroke = 5.dp) {
                Text("${last.quality ?: "–"}", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun MoveHomeCard(vm: ModsViewModel, onOpen: () -> Unit) {
    val d by vm.move.state.collectAsState()
    val wk = MoveStats.weekMinutes(d)
    val today = d.items.filter { it.date == LocalDate.now().toString() }.sumOf { it.minutes }
    AppCard(onClick = onOpen) {
        CardHeader(Icons.Outlined.DirectionsRun, stringResource(R.string.mod_move), tint = Noc.good) {
            Text(stringResource(R.string.move_min_of, wk, d.weeklyGoal), style = MaterialTheme.typography.labelMedium, color = Noc.text2)
        }
        VSpace(8)
        Meter(wk.toFloat() / d.weeklyGoal.coerceAtLeast(1), Noc.good)
        VSpace(6)
        Hint(if (today > 0) stringResource(R.string.move_home_today, today) else stringResource(R.string.move_home_none))
    }
}

@Composable
fun PeopleHomeCard(vm: ModsViewModel, onOpen: () -> Unit) {
    val d by vm.people.state.collectAsState()
    val due = d.people.filter { (it.dueIn() ?: 1) <= 0 }.sortedBy { it.dueIn() }
    val bday = d.people.filter { (it.birthdayIn() ?: 99) <= 7 }.minByOrNull { it.birthdayIn() ?: 99 }
    AppCard(onClick = onOpen) {
        CardHeader(Icons.Outlined.People, stringResource(R.string.mod_people), tint = Noc.coral)
        VSpace(8)
        when {
            d.people.isEmpty() -> Hint(stringResource(R.string.people_home_empty))
            due.isEmpty() && bday == null -> Hint(stringResource(R.string.people_home_ok))
            else -> {
                bday?.let { p ->
                    val n = p.birthdayIn() ?: 0
                    Text(if (n == 0) stringResource(R.string.people_home_bday_today, p.name) else stringResource(R.string.people_home_bday, p.name, n),
                        style = MaterialTheme.typography.bodyMedium, color = Noc.amber)
                }
                if (due.isNotEmpty()) Text(stringResource(R.string.people_home_due, due.take(3).joinToString(", ") { it.name }),
                    style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun GoalsHomeCard(vm: ModsViewModel, onOpen: () -> Unit) {
    val d by vm.goals.state.collectAsState()
    val active = d.goals.filter { !it.done }
    AppCard(onClick = onOpen) {
        CardHeader(Icons.Outlined.Flag, stringResource(R.string.mod_goals), tint = Noc.amber) {
            if (active.isNotEmpty()) Text(stringResource(R.string.goals_active_n, active.size), style = MaterialTheme.typography.labelMedium, color = Noc.text2)
        }
        VSpace(8)
        if (active.isEmpty()) Hint(stringResource(R.string.goals_home_empty))
        active.sortedBy { it.deadline ?: "9999" }.take(2).forEach { g ->
            Column(Modifier.padding4()) {
                Row {
                    Text(g.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${(g.progress() * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = Noc.amber)
                }
                Meter(g.progress(), Noc.amber, height = 4.dp)
            }
        }
    }
}

private fun Modifier.padding4() = this.then(Modifier.padding(vertical = 4.dp))

@Composable
fun LearnHomeCard(vm: ModsViewModel, onOpen: () -> Unit) {
    val d by vm.learn.state.collectAsState()
    val today = LearnStats.minutesOn(d, LocalDate.now().toString())
    val cur = d.items.firstOrNull { !it.done }
    AppCard(onClick = onOpen) {
        CardHeader(Icons.Outlined.School, stringResource(R.string.mod_learn), tint = Noc.ice) {
            Text(stringResource(R.string.move_min_of, today, d.dailyGoal), style = MaterialTheme.typography.labelMedium, color = Noc.text2)
        }
        VSpace(8)
        Meter(today.toFloat() / d.dailyGoal.coerceAtLeast(1), Noc.ice)
        VSpace(6)
        Hint(cur?.let { it.title + if (it.total > 0) " · ${it.progress}/${it.total}" else "" } ?: stringResource(R.string.learn_home_empty))
    }
}

@Composable
fun HouseHomeCard(vm: ModsViewModel, onOpen: () -> Unit) {
    val d by vm.house.state.collectAsState()
    val due = d.chores.filter { it.dueIn() <= 0 }
    val shop = d.shopping.count { !it.done }
    AppCard(onClick = onOpen) {
        CardHeader(Icons.Outlined.CleaningServices, stringResource(R.string.mod_house), tint = Noc.cyan)
        VSpace(8)
        if (d.chores.isEmpty() && d.shopping.isEmpty()) Hint(stringResource(R.string.house_home_empty))
        else {
            Hint(if (due.isEmpty()) stringResource(R.string.house_home_ok) else stringResource(R.string.house_home_due, due.take(3).joinToString(", ") { it.name }))
            if (shop > 0) Text(stringResource(R.string.house_home_shop, shop), style = MaterialTheme.typography.bodySmall, color = Noc.cyan)
        }
    }
}

@Composable
fun TrackersHomeCards(vm: ModsViewModel, onOpen: () -> Unit) {
    val d by vm.trackers.state.collectAsState()
    val list = d.trackers.filter { it.onHome }
    if (list.isEmpty()) {
        AppCard(onClick = onOpen) {
            Text(stringResource(R.string.mod_trackers), style = MaterialTheme.typography.titleMedium)
            Hint(stringResource(R.string.trackers_home_empty))
        }
    } else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        list.forEach { t -> TrackerCard(vm, d, t, onEdit = onOpen) }
    }
}


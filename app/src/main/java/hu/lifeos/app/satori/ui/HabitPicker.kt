package hu.lifeos.app.satori.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.satori.SatoriViewModel
import hu.lifeos.app.ui.CUSTOM_HABIT_ICONS
import hu.lifeos.app.ui.MenuRow
import hu.lifeos.app.ui.PrimaryButton
import hu.lifeos.app.ui.SectionLabel
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.iconFor
import hu.lifeos.app.ui.theme.Noc

/** Saját szokás űrlapja: név, ikon, napi költség. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomHabitForm(onCreate: (label: String, icon: String, dailyCost: Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf(CUSTOM_HABIT_ICONS.first()) }
    var cost by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            name, { name = it }, Modifier.fillMaxWidth(), singleLine = true,
            label = { Text(stringResource(R.string.habit_name)) }, placeholder = { Text(stringResource(R.string.habit_name_hint)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Text(stringResource(R.string.habit_icon), style = MaterialTheme.typography.labelLarge, color = Noc.text2)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CUSTOM_HABIT_ICONS.forEach { ic ->
                val on = ic == icon
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (on) Noc.amber.copy(alpha = 0.18f) else Noc.well)
                        .border(1.dp, if (on) Noc.amber else Noc.stroke, RoundedCornerShape(12.dp))
                        .clickable { icon = ic },
                    contentAlignment = Alignment.Center,
                ) { Icon(iconFor(ic), null, tint = if (on) Noc.amber else Noc.text2) }
            }
        }
        OutlinedTextField(
            cost, { v -> cost = v.filter { it.isDigit() } }, Modifier.fillMaxWidth(), singleLine = true,
            label = { Text(stringResource(R.string.habit_cost)) }, suffix = { Text("Ft") },
            supportingText = { Text(stringResource(R.string.habit_cost_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        PrimaryButton(
            stringResource(R.string.habit_create), onClick = { onCreate(name, icon, cost.toDoubleOrNull() ?: 0.0) },
            enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Párbeszédablak a saját szokás létrehozásához (a bevezetőben). */
@Composable
fun CustomHabitDialog(onDismiss: () -> Unit, onCreate: (String, String, Double) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.habit_custom)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { CustomHabitForm(onCreate) } },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Új szokás követése: a még nem követett beépített szokások és a saját szokás űrlapja. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitSheet(vm: SatoriViewModel, onDismiss: () -> Unit) {
    val st by vm.state.collectAsState()
    val followed = st.followedIds()
    var customOpen by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Noc.cardElevated) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Text(stringResource(R.string.habit_add), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
            Text(stringResource(R.string.habit_add_sub), style = MaterialTheme.typography.bodySmall, color = Noc.text2, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
            val rest = vm.content.allTypes().filter { it.id !in followed }
            if (rest.isNotEmpty()) SectionLabel(stringResource(R.string.habit_presets))
            rest.forEach { t ->
                MenuRow(iconFor(t.icon), t.label, null, onClick = { vm.follow(t.id); onDismiss() })
            }
            VSpace(8)
            SectionLabel(stringResource(R.string.habit_custom))
            if (!customOpen) MenuRow(Icons.Outlined.Add, stringResource(R.string.habit_custom_new), stringResource(R.string.habit_custom_sub), onClick = { customOpen = true })
            else CustomHabitForm { l, i, c -> vm.addCustomHabit(l, i, c); onDismiss() }
        }
    }
}

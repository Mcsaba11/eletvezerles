package hu.lifeos.app.satori.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import hu.lifeos.app.R
import hu.lifeos.app.satori.SatoriLogic
import hu.lifeos.app.satori.SatoriViewModel
import hu.lifeos.app.ui.Fmt
import hu.lifeos.app.ui.Pill
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.iconFor
import hu.lifeos.app.ui.theme.StatusColors
import java.time.LocalDate

// ---------------- Nap lezárása ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayCloseSheet(vm: SatoriViewModel, onDismiss: () -> Unit) {
    val st by vm.state.collectAsState()
    val c = vm.content
    val d = st.cur()
    val mode = c.mode(st.mode)
    val closedToday = d.dayOutcomeDate == SatoriLogic.today()
    var editing by rememberSaveable { mutableStateOf(!closedToday) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var note by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding().imePadding()) {
            Text(stringResource(R.string.satori_close_day_title), style = MaterialTheme.typography.titleLarge)
            VSpace(6)
            if (!editing) {
                val label = SatoriLogic.outcomeLabel(c, st, d.dayOutcome ?: "siker")
                Text(stringResource(R.string.satori_today_result, label), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
                Text(stringResource(R.string.satori_closed_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                VSpace(12)
                TextButton(onClick = { editing = true; selected = if (d.dayOutcome == "grace") "sikertelen" else d.dayOutcome }) {
                    Text(stringResource(R.string.satori_change_today))
                }
                return@Column
            }
            Text(mode.dayDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (st.mode == "balanced") {
                VSpace(8)
                val remaining = (d.weeklyAllowance - SatoriLogic.graceUsedThisWeek(d)).coerceAtLeast(0)
                Pill(stringResource(R.string.satori_allowance_pill, remaining, d.weeklyAllowance))
            }
            VSpace(14)
            val options = buildList {
                add(Triple("siker", "check_circle", StatusColors.ok))
                if (st.mode != "strict") add(Triple("megingas", "air", StatusColors.warn))
                add(Triple("sikertelen", "restart_alt", StatusColors.bad))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (id, icon, col) ->
                    val on = selected == id
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .let { if (on) it.border(2.dp, col, RoundedCornerShape(16.dp)) else it }
                            .clickable { selected = id }.padding(vertical = 14.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(iconFor(icon), null, tint = col, modifier = Modifier.size(28.dp))
                        VSpace(6)
                        Text(mode.labels[id]?.takeIf { it.isNotBlank() } ?: (c.outcomeMeta[id]?.label ?: id),
                            style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                    }
                }
            }
            VSpace(12)
            OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), placeholder = { Text(stringResource(R.string.satori_close_note)) }, minLines = 2)
            VSpace(12)
            Button(
                onClick = { selected?.let { vm.closeDay(it, note) }; onDismiss() }, enabled = selected != null, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.onTertiary),
            ) { Text(stringResource(R.string.satori_close_day)) }
        }
    }
}

// ---------------- Oklevél (szintlépés) ----------------

@Composable
fun CertificateDialog(vm: SatoriViewModel, stageIndex: Int, replay: Boolean, onClose: () -> Unit) {
    val st by vm.state.collectAsState()
    val c = vm.content
    val th = c.theme(st.theme)
    val s = c.stages.getOrNull(stageIndex) ?: return
    val accent = MaterialTheme.colorScheme.tertiary
    val title = SatoriLogic.stageTitle(c, st, stageIndex, stringResource(R.string.satori_restarter))
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.padding(20.dp).clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerLowest)))
                .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
                .verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.EmojiEvents, null, tint = accent, modifier = Modifier.size(48.dp))
            VSpace(8)
            Text(stringResource(if (replay) R.string.satori_cert else R.string.satori_cert_new).uppercase(), style = MaterialTheme.typography.labelSmall, color = accent)
            Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            VSpace(10)
            Text(stringResource(R.string.satori_cert_line, s.days, stageIndex + 1, title), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            VSpace(10)
            Text(s.welcome, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, fontStyle = FontStyle.Italic)
            val land = (c.land[st.theme] ?: c.land["zen"]).orEmpty().getOrNull(stageIndex)
            if (!land.isNullOrBlank()) {
                VSpace(14)
                Text(stringResource(R.string.satori_cert_land).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(land, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
            VSpace(14)
            Text(SatoriLogic.stageReward(c, st, stageIndex), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
            VSpace(14)
            Text(
                (if (!replay) stringResource(R.string.satori_cert_date, Fmt.dayLong(LocalDate.now())) + " · " else "") +
                    stringResource(R.string.satori_cert_signed, th.mentorName.ifBlank { stringResource(R.string.satori_tab_mentor) }),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
            )
            VSpace(16)
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = MaterialTheme.colorScheme.onTertiary)) {
                Text(stringResource(R.string.satori_continue))
            }
        }
    }
}

// ---------------- Nyitó megerősítés ----------------

@Composable
fun AffirmationDialog(onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0C0E10))) {
            Image(painterResource(R.drawable.photo_splash), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.6f)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xF00C0E10)))))
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(28.dp), verticalArrangement = Arrangement.Bottom) {
                Icon(Icons.Outlined.SelfImprovement, null, tint = MaterialTheme.colorScheme.tertiary)
                VSpace(8)
                Text(stringResource(R.string.aff_kicker).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                VSpace(8)
                Text(stringResource(R.string.aff_line1), style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                Text(stringResource(R.string.aff_line2), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                VSpace(8)
                Text(stringResource(R.string.aff_line3), style = MaterialTheme.typography.titleMedium, color = Color.White)
                VSpace(12)
                Text(stringResource(R.string.aff_body), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                VSpace(24)
                Button(onClick = onClose, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.onTertiary)) {
                    Text(stringResource(R.string.aff_continue))
                }
            }
        }
    }
}

// ---------------- Bevezető (első indítás) ----------------

@Composable
fun Onboarding(vm: SatoriViewModel, onCancel: () -> Unit) {
    val c = vm.content
    var step by rememberSaveable { mutableIntStateOf(0) }
    var picked by rememberSaveable { mutableStateOf(listOf<String>()) }
    var customOpen by rememberSaveable { mutableStateOf(false) }
    var theme by rememberSaveable { mutableStateOf("zen") }
    var mode by rememberSaveable { mutableStateOf("balanced") }
    val photo = if (step < 2) R.drawable.photo_splash else themePhoto(theme)

    Box(Modifier.fillMaxSize().background(Color(0xFF0C0E10))) {
        Image(painterResource(photo), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.45f)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xF00C0E10)))))
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel), color = Color.White) }
            }
            Spacer(Modifier.size(80.dp))
            when (step) {
                0 -> {
                    Text("Satori", style = MaterialTheme.typography.displaySmall, color = Color.White)
                    Text(stringResource(R.string.ob_intro), style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.85f))
                    VSpace(16)
                    Button(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ob_start)) }
                }
                1 -> {
                    Text(stringResource(R.string.ob_type), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Text(stringResource(R.string.ob_type_multi), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                    c.allTypes().forEach { t ->
                        ChoiceRow(iconFor(t.icon), t.label, null, t.id in picked) { picked = if (t.id in picked) picked - t.id else picked + t.id }
                    }
                    ChoiceRow(Icons.Outlined.Add, stringResource(R.string.habit_custom_new), stringResource(R.string.habit_custom_sub), false) { customOpen = true }
                    Button(onClick = { step = 2 }, enabled = picked.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.next)) }
                }
                2 -> {
                    Text(stringResource(R.string.ob_theme), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    c.themes.forEach { t -> ChoiceRow(iconFor(t.icon), t.label, t.tagline, t.id == theme) { theme = t.id; vm.setTheme(t.id) } }
                    Button(onClick = { step = 3 }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.next)) }
                }
                3 -> {
                    Text(stringResource(R.string.ob_mode), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    listOf("strict", "balanced", "gentle").forEach { id ->
                        val m = c.mode(id)
                        ChoiceRow(iconFor(m.icon), m.name, m.desc, id == mode) { mode = id }
                    }
                    Button(onClick = { step = 4 }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.next)) }
                }
                else -> {
                    val th = c.theme(theme)
                    Text("„${th.tagline}”", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Text(th.pledgeText, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.85f), fontStyle = FontStyle.Italic)
                    VSpace(16)
                    Button(onClick = { vm.finishOnboarding(picked, theme, mode) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ob_go)) }
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.ob_import_hint), color = Color.White.copy(alpha = 0.8f)) }
                }
            }
        }
    }
    if (customOpen) CustomHabitDialog(onDismiss = { customOpen = false }) { l, i, cost ->
        val id = vm.addCustomHabit(l, i, cost)
        picked = picked + id
        customOpen = false
    }
}

@Composable
private fun ChoiceRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String?, selected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.tertiary
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xB314171A))
            .let { if (selected) it.border(2.dp, accent, RoundedCornerShape(16.dp)) else it }
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = accent)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
        }
    }
}


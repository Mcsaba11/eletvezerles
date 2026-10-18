package hu.lifeos.app.satori.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.satori.SatoriEvent
import hu.lifeos.app.satori.SatoriViewModel
import hu.lifeos.app.ui.MenuRow
import hu.lifeos.app.ui.SectionLabel
import hu.lifeos.app.ui.VSpace
import hu.lifeos.app.ui.iconFor
import hu.lifeos.app.ui.theme.AppShapes
import hu.lifeos.app.ui.theme.AppTypography
import hu.lifeos.app.ui.theme.satoriScheme

fun themePhoto(id: String): Int = when (id) {
    "harcos" -> R.drawable.photo_harcos
    "joga" -> R.drawable.photo_joga
    "sportolo" -> R.drawable.photo_sportolo
    "felfedezo" -> R.drawable.photo_felfedezo
    else -> R.drawable.photo_zen
}

fun themeSosPhoto(id: String): Int = when (id) {
    "harcos" -> R.drawable.photo_sos_harcos
    "joga" -> R.drawable.photo_sos_joga
    "sportolo" -> R.drawable.photo_sos_sportolo
    "felfedezo" -> R.drawable.photo_sos_felfedezo
    else -> R.drawable.photo_sos_zen
}

private data class STab(val label: Int, val icon: ImageVector)

/** Egyszer, a Satori első megnyitásakor (folyamatonként) megjelenő megerősítés. */
private var affirmationShown = false

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatoriRoot(vm: SatoriViewModel, startSos: Boolean, onExit: () -> Unit) {
    val st by vm.state.collectAsState()
    val loaded by vm.loaded.collectAsState()
    val c = vm.content
    val theme = c.theme(st.theme)

    MaterialTheme(colorScheme = satoriScheme(theme.colors), typography = AppTypography, shapes = AppShapes) {
        if (!loaded) return@MaterialTheme
        if (st.onboardingDone != true) {
            Onboarding(vm, onCancel = onExit)
            return@MaterialTheme
        }

        var tab by rememberSaveable { mutableIntStateOf(0) }
        var sosOpen by rememberSaveable { mutableStateOf(startSos) }
        var breathOpen by rememberSaveable { mutableStateOf(false) }
        var bodyOpen by rememberSaveable { mutableStateOf(false) }
        var dayCloseOpen by rememberSaveable { mutableStateOf(false) }
        var typeSheet by rememberSaveable { mutableStateOf(false) }
        var settingsOpen by rememberSaveable { mutableStateOf(false) }
        var certIndex by rememberSaveable { mutableStateOf<Int?>(null) }
        var certReplay by rememberSaveable { mutableStateOf(false) }
        var affirmation by rememberSaveable { mutableStateOf(!affirmationShown && !startSos) }
        val event by vm.event.collectAsState()
        var comfort by remember { mutableStateOf<SatoriEvent.Comfort?>(null) }
        var resetPulse by remember { mutableStateOf(false) }

        LaunchedEffect(event) {
            when (val e = event) {
                is SatoriEvent.LevelUp -> { certIndex = e.stageIndex; certReplay = false }
                is SatoriEvent.Comfort -> comfort = e
                SatoriEvent.ResetPulse -> resetPulse = true
                null -> {}
            }
            if (event != null) vm.consumeEvent()
        }

        val tabs = listOf(
            STab(R.string.satori_tab_path, Icons.Outlined.Spa),
            STab(R.string.satori_tab_results, Icons.Outlined.EmojiEvents),
            STab(R.string.satori_tab_journal, Icons.Outlined.Book),
            STab(R.string.satori_tab_balance, Icons.Outlined.Tune),
            STab(R.string.satori_tab_mentor, Icons.Outlined.Psychology),
        )
        val type = c.type(st.type)

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    navigationIcon = { IconButton(onClick = onExit) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back)) } },
                    title = {
                        TextButton(onClick = { typeSheet = true }) {
                            Icon(iconFor(type.icon), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(6.dp))
                            Text(type.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Icon(Icons.Outlined.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    actions = {
                        Button(
                            onClick = { sosOpen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                        ) { Text(stringResource(R.string.sos)) }
                        IconButton(onClick = { settingsOpen = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.settings)) }
                    },
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    tabs.forEachIndexed { i, t ->
                        val label = if (i == 4) theme.mentorName.ifBlank { stringResource(t.label) } else stringResource(t.label)
                        NavigationBarItem(
                            selected = tab == i, onClick = { tab = i },
                            icon = { Icon(t.icon, null) }, label = { Text(label, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)),
                        )
                    }
                }
            },
        ) { pad ->
            val mod = Modifier.padding(pad)
            when (tab) {
                0 -> PathTab(vm, mod, onDayClose = { dayCloseOpen = true }, onBody = { bodyOpen = true }, resetPulse = resetPulse, onPulseDone = { resetPulse = false })
                1 -> ResultsTab(vm, mod, onCertificate = { idx -> certIndex = idx; certReplay = true })
                2 -> JournalTab(vm, mod)
                3 -> BalanceTab(vm, mod)
                else -> MentorTab(vm, mod, onSos = { sosOpen = true })
            }
        }

        if (dayCloseOpen) DayCloseSheet(vm, onDismiss = { dayCloseOpen = false })
        if (typeSheet) TypeSheet(vm, onDismiss = { typeSheet = false })
        if (settingsOpen) SatoriSettingsSheet(vm, onDismiss = { settingsOpen = false })
        if (sosOpen) SosOverlay(vm, onClose = { sosOpen = false }, onBreath = { breathOpen = true })
        if (breathOpen) BreathOverlay(vm, onClose = { breathOpen = false })
        if (bodyOpen) BodyTimelineOverlay(vm, onClose = { bodyOpen = false })
        certIndex?.let { CertificateDialog(vm, it, certReplay, onClose = { certIndex = null }) }
        comfort?.let { cm ->
            AlertDialog(
                onDismissRequest = { comfort = null },
                icon = { Icon(Icons.Outlined.Spa, null, tint = MaterialTheme.colorScheme.tertiary) },
                title = { Text(cm.title) }, text = { Text(cm.text) },
                confirmButton = { TextButton(onClick = { comfort = null }) { Text(stringResource(R.string.satori_restart_go)) } },
            )
        }
        if (affirmation) AffirmationDialog(onClose = { affirmation = false; affirmationShown = true })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeSheet(vm: SatoriViewModel, onDismiss: () -> Unit) {
    val st by vm.state.collectAsState()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Text(stringResource(R.string.satori_category), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
            vm.content.types.forEach { t ->
                val streak = st.data[t.id]?.streak ?: 0
                MenuRow(iconFor(t.icon), t.label, stringResource(R.string.satori_n_days, streak), onClick = { vm.setType(t.id); onDismiss() }, trailing = {
                    if (t.id == st.type) Icon(Icons.Outlined.Spa, null, tint = MaterialTheme.colorScheme.tertiary)
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SatoriSettingsSheet(vm: SatoriViewModel, onDismiss: () -> Unit) {
    val st by vm.state.collectAsState()
    val c = vm.content
    val d = st.cur()
    val ctx = LocalContext.current
    var allyEdit by remember { mutableStateOf(false) }
    var restartAsk by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))

            SectionLabel(stringResource(R.string.satori_theme))
            Text(stringResource(R.string.satori_theme_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp))
            c.themes.forEach { th ->
                MenuRow(iconFor(th.icon), th.label, th.tagline, onClick = { vm.setTheme(th.id) }, trailing = {
                    if (th.id == st.theme) Icon(Icons.Outlined.Spa, null, tint = MaterialTheme.colorScheme.tertiary)
                })
            }

            SectionLabel(stringResource(R.string.satori_rules))
            Text(stringResource(R.string.satori_rules_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp))
            listOf("strict", "balanced", "gentle").forEach { id ->
                val m = c.mode(id)
                MenuRow(iconFor(m.icon), m.name, m.desc, onClick = { vm.setMode(id) }, trailing = {
                    if (st.mode == id) Icon(Icons.Outlined.Spa, null, tint = MaterialTheme.colorScheme.tertiary)
                })
            }
            if (st.mode == "balanced") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text(stringResource(R.string.satori_allowance, d.weeklyAllowance), modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.setWeeklyAllowance(d.weeklyAllowance - 1) }, enabled = d.weeklyAllowance > 0) { Text("−") }
                    TextButton(onClick = { vm.setWeeklyAllowance(d.weeklyAllowance + 1) }, enabled = d.weeklyAllowance < 7) { Text("+") }
                }
            }

            SectionLabel(stringResource(R.string.satori_support))
            MenuRow(Icons.Outlined.Psychology, stringResource(R.string.satori_ally), st.ally.name.ifBlank { stringResource(R.string.satori_ally_none) }, onClick = { allyEdit = true })
            MenuRow(Icons.Outlined.Book, stringResource(R.string.satori_share), stringResource(R.string.satori_share_sub), onClick = {
                val text = ctx.getString(R.string.satori_share_text, c.theme(st.theme).pledgeText, d.streak, c.type(st.type).label)
                ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))
            })
            VSpace(12)
            TextButton(onClick = { restartAsk = true }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)) {
                Text(stringResource(R.string.satori_restart))
            }
            Text(stringResource(R.string.satori_more_in_settings), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
    if (allyEdit) {
        var name by remember { mutableStateOf(st.ally.name) }
        var phone by remember { mutableStateOf(st.ally.phone) }
        AlertDialog(
            onDismissRequest = { allyEdit = false },
            title = { Text(stringResource(R.string.satori_ally)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.satori_ally_help), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.satori_ally_name)) }, singleLine = true)
                    OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.satori_ally_phone)) }, singleLine = true)
                }
            },
            confirmButton = { TextButton(onClick = { vm.setAlly(name, phone); allyEdit = false }) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton(onClick = { allyEdit = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (restartAsk) {
        AlertDialog(
            onDismissRequest = { restartAsk = false },
            title = { Text(stringResource(R.string.satori_restart)) },
            text = { Text(stringResource(R.string.satori_restart_text, c.type(st.type).label)) },
            confirmButton = { TextButton(onClick = { restartAsk = false; vm.restart(); onDismiss() }) { Text(stringResource(R.string.satori_restart_yes)) } },
            dismissButton = { TextButton(onClick = { restartAsk = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}


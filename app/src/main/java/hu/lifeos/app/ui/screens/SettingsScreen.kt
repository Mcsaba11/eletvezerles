package hu.lifeos.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hu.lifeos.app.BuildConfigInfo
import hu.lifeos.app.R
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import hu.lifeos.app.data.Modules
import hu.lifeos.app.data.HomeCards
import hu.lifeos.app.container
import hu.lifeos.app.data.Backup
import hu.lifeos.app.data.ImportKind
import hu.lifeos.app.satori.SatoriViewModel
import hu.lifeos.app.ui.AppCard
import hu.lifeos.app.ui.MainViewModel
import hu.lifeos.app.ui.MenuRow
import hu.lifeos.app.ui.SectionLabel
import hu.lifeos.app.ui.TimePickDialog
import hu.lifeos.app.ui.VSpace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, satori: SatoriViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsState()
    val settings = s ?: return
    val calendars by vm.phoneCalendars.collectAsState()
    val ctx = LocalContext.current
    val backup = remember { Backup(ctx.container) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var pickTime by remember { mutableStateOf(false) }
    var pasteOpen by remember { mutableStateOf(false) }
    var confirmImport by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.refreshPhone() }

    fun toast(res: Int) { scope.launch { snackbar.showSnackbar(ctx.getString(res)) } }

    val calPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res[Manifest.permission.READ_CALENDAR] == true) vm.updateSettings { it.copy(showPhoneCalendar = true) }
        else toast(R.string.perm_denied)
    }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) vm.updateSettings { it.copy(reminderEnabled = true) } else toast(R.string.perm_denied)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching {
                val text = backup.export()
                withContext(Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } }
            }.isSuccess
            toast(if (ok) R.string.backup_saved else R.string.backup_failed)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val text = runCatching {
                withContext(Dispatchers.IO) { ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }
            }.getOrNull()
            if (text.isNullOrBlank()) toast(R.string.backup_bad) else confirmImport = text
        }
    }

    fun doImport(text: String) = scope.launch {
        val r = runCatching { backup.import(text) }
        toast(
            when (r.getOrNull()) {
                ImportKind.FULL -> R.string.backup_restored
                ImportKind.SATORI_LEGACY -> R.string.satori_imported
                ImportKind.FOOD_LEGACY -> R.string.food_imported
                null -> R.string.backup_bad
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back)) } },
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionLabel(stringResource(R.string.set_general))
            AppCard {
                var name by remember { mutableStateOf(settings.name) }
                OutlinedTextField(
                    name, { name = it; vm.updateSettings { st -> st.copy(name = it.trim()) } },
                    Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.your_name)) }, singleLine = true,
                )
                VSpace(12)
                Text(stringResource(R.string.appearance), style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to R.string.mode_system, "dark" to R.string.mode_dark, "light" to R.string.mode_light).forEach { (id, l) ->
                        FilterChip(settings.darkMode == id, { vm.updateSettings { it.copy(darkMode = id) } }, label = { Text(stringResource(l)) })
                    }
                }
                VSpace(12)
                var budget by remember { mutableStateOf(if (settings.monthlyBudget > 0) settings.monthlyBudget.toLong().toString() else "") }
                OutlinedTextField(
                    budget,
                    { v ->
                        budget = v.filter { it.isDigit() }
                        val num = budget.toDoubleOrNull() ?: 0.0
                        vm.updateSettings { st -> st.copy(monthlyBudget = num) }
                    },
                    Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.set_budget)) }, singleLine = true,
                    suffix = { Text("Ft") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                )
            }

            SectionLabel(stringResource(R.string.apps_modules))
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MODULE_INFO.forEach { m ->
                        ModuleTile(m, settings.moduleOn(m.id)) {
                            vm.updateSettings { st -> val cur = st.modules ?: Modules.ALL; st.copy(modules = if (m.id in cur) cur - m.id else cur + m.id) }
                        }
                    }
                }
            }

            SectionLabel(stringResource(R.string.home_cards))
            AppCard {
                Text(stringResource(R.string.home_cards_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                VSpace(6)
                val order = HomeCards.ordered(settings).filter { id -> HomeCards.module(id)?.let(settings::moduleOn) ?: true }
                order.forEachIndexed { i, id ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(homeCardLabel(id)), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        IconButton(onClick = { vm.updateSettings { st -> st.copy(homeOrder = moveCard(HomeCards.ordered(st), id, -1)) } }, enabled = i > 0) {
                            Icon(Icons.Outlined.KeyboardArrowUp, stringResource(R.string.move_up))
                        }
                        IconButton(onClick = { vm.updateSettings { st -> st.copy(homeOrder = moveCard(HomeCards.ordered(st), id, 1)) } }, enabled = i < order.size - 1) {
                            Icon(Icons.Outlined.KeyboardArrowDown, stringResource(R.string.move_down))
                        }
                        Switch(id !in settings.homeHidden, { on -> vm.updateSettings { st -> st.copy(homeHidden = if (on) st.homeHidden - id else st.homeHidden + id) } })
                    }
                }
                VSpace(6)
                TextButton(onClick = { vm.updateSettings { it.copy(welcomeDone = false) } }) { Text(stringResource(R.string.wiz_restart)) }
            }

            SectionLabel(stringResource(R.string.tab_calendar))
            AppCard {
                SwitchRow(stringResource(R.string.phone_cal_show), stringResource(R.string.phone_cal_show_sub), settings.showPhoneCalendar) { on ->
                    if (on && !ctx.container.phoneCalendar.canRead()) {
                        calPerm.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
                    } else vm.updateSettings { it.copy(showPhoneCalendar = on) }
                }
                if (settings.showPhoneCalendar && calendars.isNotEmpty()) {
                    VSpace(8)
                    Text(stringResource(R.string.phone_cal_which), style = MaterialTheme.typography.titleSmall)
                    calendars.forEach { cal ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(cal.id !in settings.hiddenCalendarIds, { on ->
                                vm.updateSettings { st -> st.copy(hiddenCalendarIds = if (on) st.hiddenCalendarIds - cal.id else st.hiddenCalendarIds + cal.id) }
                            })
                            Column(Modifier.weight(1f)) {
                                Text(cal.name.ifBlank { cal.account })
                                if (cal.account.isNotBlank() && cal.account != cal.name) Text(cal.account, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    val writable = calendars.filter { it.writable }
                    if (writable.isNotEmpty()) {
                        VSpace(8)
                        Text(stringResource(R.string.phone_cal_default_write), style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(settings.defaultWriteCalendarId == null, { vm.updateSettings { it.copy(defaultWriteCalendarId = null) } })
                            Text(stringResource(R.string.own_calendar))
                        }
                        writable.forEach { cal ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(settings.defaultWriteCalendarId == cal.id, { vm.updateSettings { it.copy(defaultWriteCalendarId = cal.id) } })
                                Text(cal.name.ifBlank { cal.account })
                            }
                        }
                    }
                }
            }

            SectionLabel(stringResource(R.string.set_reminder))
            AppCard {
                SwitchRow(stringResource(R.string.reminder_on), stringResource(R.string.reminder_sub), settings.reminderEnabled) { on ->
                    if (on && Build.VERSION.SDK_INT >= 33 && !hu.lifeos.app.notify.Reminders.canNotify(ctx)) {
                        notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else vm.updateSettings { it.copy(reminderEnabled = on) }
                }
                if (settings.reminderEnabled) {
                    TextButton(onClick = { pickTime = true }) { Text(stringResource(R.string.reminder_time, settings.reminderTime)) }
                }
            }

            SectionLabel(stringResource(R.string.set_security))
            AppCard {
                SwitchRow(stringResource(R.string.lock_on), stringResource(R.string.lock_sub), settings.lockEnabled) { on ->
                    vm.updateSettings { it.copy(lockEnabled = on) }
                }
            }

            SectionLabel(stringResource(R.string.set_data))
            AppCard {
                Text(stringResource(R.string.data_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                VSpace(4)
                MenuRow(Icons.Outlined.FileDownload, stringResource(R.string.backup_export), stringResource(R.string.backup_export_sub),
                    onClick = { exportLauncher.launch("eletvezerles-mentes-${LocalDate.now()}.json") })
                MenuRow(Icons.Outlined.FileUpload, stringResource(R.string.backup_import), stringResource(R.string.backup_import_sub2),
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) })
                MenuRow(Icons.Outlined.ContentPaste, stringResource(R.string.satori_paste), stringResource(R.string.satori_paste_sub),
                    onClick = { pasteOpen = true })
            }

            VSpace(8)
            Text(
                stringResource(R.string.version, BuildConfigInfo.versionName(ctx)), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp),
            )
        }
    }

    if (pickTime) TimePickDialog(runCatching { LocalTime.parse(settings.reminderTime) }.getOrDefault(LocalTime.of(20, 0)), { pickTime = false }) { t ->
        vm.updateSettings { it.copy(reminderTime = t.toString()) }
    }

    if (pasteOpen) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pasteOpen = false },
            title = { Text(stringResource(R.string.satori_paste)) },
            text = {
                Column {
                    Text(stringResource(R.string.satori_paste_help), style = MaterialTheme.typography.bodySmall)
                    VSpace(8)
                    OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 240.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = { pasteOpen = false; confirmImport = text }, enabled = text.isNotBlank()) { Text(stringResource(R.string.next)) }
            },
            dismissButton = { TextButton(onClick = { pasteOpen = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    confirmImport?.let { text ->
        AlertDialog(
            onDismissRequest = { confirmImport = null },
            title = { Text(stringResource(R.string.import_confirm_title)) },
            text = { Text(stringResource(R.string.import_confirm_text)) },
            confirmButton = { TextButton(onClick = { confirmImport = null; doImport(text) }) { Text(stringResource(R.string.import_confirm_yes)) } },
            dismissButton = { TextButton(onClick = { confirmImport = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
fun SwitchRow(title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onChange)
    }
}

/** A kezdőlap-kártya neve a beállításokhoz. */
fun homeCardLabel(id: String): Int = when (id) {
    "capture" -> R.string.hc_capture
    "insight" -> R.string.sensei_insight
    "food" -> R.string.app_food
    "satori" -> R.string.satori_name
    "breath" -> R.string.hc_breath
    "mood" -> R.string.mood_title
    "money" -> R.string.card_money
    "focus" -> R.string.card_focus
    "wheel" -> R.string.wheel_title
    "sleep" -> R.string.mod_sleep
    "move" -> R.string.mod_move
    "people" -> R.string.mod_people
    "goals" -> R.string.mod_goals
    "learn" -> R.string.mod_learn
    "house" -> R.string.mod_house
    "trackers" -> R.string.mod_trackers
    else -> R.string.hc_quote
}

/** Egy kártya mozgatása a sorrendben (dir = -1 fel, +1 le). */
fun moveCard(order: List<String>, id: String, dir: Int): List<String> {
    val l = order.toMutableList()
    val i = l.indexOf(id); val j = i + dir
    if (i < 0 || j !in l.indices) return order
    l[i] = l[j]; l[j] = id
    return l
}

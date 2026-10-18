package hu.lifeos.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.capture.CaptureType
import hu.lifeos.app.capture.Parsed
import hu.lifeos.app.capture.QuickParser
import java.time.LocalDate

fun captureLabel(t: CaptureType): Int = when (t) {
    CaptureType.EVENT -> R.string.type_event
    CaptureType.TODO -> R.string.type_todo
    CaptureType.EXPENSE -> R.string.type_expense
    CaptureType.INCOME -> R.string.type_income
    CaptureType.MOOD -> R.string.type_mood
    CaptureType.NOTE -> R.string.type_note
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSheet(onDismiss: () -> Unit, onSave: (Parsed) -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf("") }
    var override by remember { mutableStateOf<CaptureType?>(null) }
    val parsed = remember(text) { QuickParser.parse(text) }
    val type = override ?: parsed.type
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    fun canSave(): Boolean = when (type) {
        CaptureType.MOOD -> parsed.mood != null
        CaptureType.EXPENSE, CaptureType.INCOME -> parsed.amount != null
        else -> parsed.title.isNotBlank()
    }
    fun save() { if (canSave()) onSave(parsed.copy(type = type)) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding().imePadding()) {
            Text(stringResource(R.string.capture_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.capture_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            VSpace(12)
            OutlinedTextField(
                value = text, onValueChange = { text = it; override = null },
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                placeholder = { Text(stringResource(R.string.capture_placeholder)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
                minLines = 2,
            )
            VSpace(12)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CaptureType.entries.forEach { t ->
                    FilterChip(selected = t == type, onClick = { override = t }, label = { Text(stringResource(captureLabel(t))) })
                }
            }
            if (text.isNotBlank()) {
                VSpace(8)
                val parts = buildList {
                    if (parsed.title.isNotBlank()) add("„${parsed.title}”")
                    parsed.date?.let { add(if (it == LocalDate.now()) stringResource(R.string.today) else Fmt.dayShort(it)) }
                    parsed.time?.let { add(it.format(Fmt.time)) }
                    parsed.amount?.let { add(Fmt.money(it)) }
                    parsed.mood?.let { add("$it/5") }
                }
                Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            VSpace(16)
            Button(onClick = { save() }, enabled = canSave(), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
        }
    }
}

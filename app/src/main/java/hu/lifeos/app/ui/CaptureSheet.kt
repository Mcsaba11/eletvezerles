package hu.lifeos.app.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.capture.CaptureType
import hu.lifeos.app.capture.Parsed
import hu.lifeos.app.capture.QuickParser
import hu.lifeos.app.ui.theme.Noc
import java.time.LocalDate

fun captureLabel(t: CaptureType): Int = when (t) {
    CaptureType.EVENT -> R.string.type_event
    CaptureType.TODO -> R.string.type_todo
    CaptureType.EXPENSE -> R.string.type_expense
    CaptureType.INCOME -> R.string.type_income
    CaptureType.MOOD -> R.string.type_mood
    CaptureType.NOTE -> R.string.type_note
}

/** Beszédfelismerés indítása (magyar). Az eredmény szövegként jön vissza. */
@Composable
fun rememberVoiceInput(onText: (String) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(onText)
        }
    }
    val prompt = stringResource(R.string.voice_prompt)
    return {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hu-HU")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        runCatching { launcher.launch(i) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSheet(onDismiss: () -> Unit, onSave: (Parsed) -> Unit, initial: String = "") {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf(initial) }
    var override by remember { mutableStateOf<CaptureType?>(null) }
    val parsed = remember(text) { QuickParser.parse(text) }
    val type = override ?: parsed.type
    val focus = remember { FocusRequester() }
    val tick = rememberTick()
    val voice = rememberVoiceInput { t -> text = if (text.isBlank()) t else "$text $t"; override = null }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    fun canSave(): Boolean = when (type) {
        CaptureType.MOOD -> parsed.mood != null
        CaptureType.EXPENSE, CaptureType.INCOME -> parsed.amount != null
        else -> parsed.title.isNotBlank()
    }
    fun save() { if (canSave()) onSave(parsed.copy(type = type)) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = Noc.cardElevated, dragHandle = { SheetHandle() }) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding().imePadding()) {
            Text(stringResource(R.string.capture_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.capture_hint), style = MaterialTheme.typography.bodySmall, color = Noc.text2)
            VSpace(12)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Noc.well).border(1.dp, Noc.cyan.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                    .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("›", style = MaterialTheme.typography.titleLarge, color = Noc.cyan)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f).padding(vertical = 8.dp)) {
                    if (text.isEmpty()) Text(stringResource(R.string.capture_placeholder), style = MaterialTheme.typography.bodyLarge, color = Noc.text3)
                    BasicTextField(
                        value = text, onValueChange = { text = it; override = null },
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Noc.text),
                        cursorBrush = SolidColor(Noc.cyan),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { save() }),
                    )
                }
                Box(
                    Modifier.size(38.dp).clip(CircleShape).background(Noc.amber.copy(alpha = 0.14f)).clickable { tick(); voice() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Mic, stringResource(R.string.voice_input), Modifier.size(20.dp), tint = Noc.amber) }
            }
            VSpace(12)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CaptureType.entries.forEach { t ->
                    val on = t == type
                    Text(
                        stringResource(captureLabel(t)), style = MaterialTheme.typography.labelLarge, color = if (on) Noc.abyss else Noc.text2,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) Noc.cyan else Noc.card)
                            .border(1.dp, if (on) Noc.cyan else Noc.stroke, RoundedCornerShape(50))
                            .clickable { tick(); override = t }.padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
            }
            if (text.isNotBlank()) {
                VSpace(12)
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Noc.card).border(1.dp, Noc.stroke, RoundedCornerShape(16.dp)).padding(12.dp)) {
                    Field(stringResource(R.string.f_type), stringResource(captureLabel(type)))
                    if (parsed.title.isNotBlank()) Field(stringResource(R.string.field_title), parsed.title)
                    parsed.date?.let { Field(stringResource(R.string.f_date), if (it == LocalDate.now()) stringResource(R.string.today) else Fmt.dayLong(it)) }
                    parsed.time?.let { Field(stringResource(R.string.f_time), it.format(Fmt.time)) }
                    parsed.amount?.let { Field(stringResource(R.string.f_amount), Fmt.money(it)) }
                    parsed.mood?.let { Field(stringResource(R.string.type_mood), "$it/5") }
                }
            }
            VSpace(16)
            PrimaryButton(stringResource(R.string.save_enter), onClick = { save() }, enabled = canSave(), modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Noc.text3, modifier = Modifier.width(90.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Noc.text)
    }
}

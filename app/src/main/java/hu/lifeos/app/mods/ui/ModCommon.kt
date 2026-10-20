package hu.lifeos.app.mods.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.ui.SheetHandle
import hu.lifeos.app.ui.rememberThump
import hu.lifeos.app.ui.rememberTick
import hu.lifeos.app.ui.theme.Noc
import kotlinx.coroutines.launch

/** Az alkalmazások egységes váza: fejléc, lapozható fülek, lebegő hozzáadás gomb. */
@Composable
fun ModScaffold(
    title: String, kicker: String, accent: Color, onBack: () -> Unit,
    tabs: List<String> = emptyList(),
    onAdd: ((page: Int) -> Unit)? = null,
    snackbar: SnackbarHostState? = null,
    headerExtra: @Composable () -> Unit = {},
    page: @Composable (Int) -> Unit,
) {
    val pages = tabs.size.coerceAtLeast(1)
    val pager = rememberPagerState(pageCount = { pages })
    val scope = rememberCoroutineScope()
    val tick = rememberTick()
    val rowState = rememberLazyListState()
    LaunchedEffect(pager.currentPage) { if (tabs.size > 1) rowState.animateScrollToItem((pager.currentPage - 1).coerceAtLeast(0)) }

    Box(Modifier.fillMaxSize().background(Noc.canvas)) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.statusBarsPadding().padding(top = 4.dp, bottom = 8.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back)) }
                    Column(Modifier.weight(1f)) {
                        Text(kicker.uppercase(), style = MaterialTheme.typography.labelSmall, color = accent, maxLines = 1)
                        Text(title, style = MaterialTheme.typography.titleLarge)
                    }
                    headerExtra()
                }
                if (tabs.size > 1) LazyRow(state = rowState, contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(tabs) { i, l ->
                        val on = pager.currentPage == i
                        Text(
                            l, style = MaterialTheme.typography.labelLarge, color = if (on) Noc.abyss else Noc.text2,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) accent else Noc.card)
                                .border(1.dp, if (on) accent else Noc.stroke, RoundedCornerShape(50))
                                .clickable { tick(); scope.launch { pager.animateScrollToPage(i) } }.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { p -> page(p) }
        }
        if (onAdd != null) {
            val thump = rememberThump()
            Box(
                Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp).size(58.dp).clip(CircleShape)
                    .background(accent).clickable { thump(); onAdd(pager.currentPage) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Add, stringResource(R.string.add), tint = Noc.abyss) }
        }
        if (snackbar != null) SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 90.dp))
    }
}

/** Lista-tartalom alapbeállításai a modulokban (alul hely a lebegő gombnak). */
val ModListPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp)

/** Alulról felcsúszó űrlap. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormSheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Noc.cardElevated, dragHandle = { SheetHandle() },
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

/** Választó kapszulák vízszintesen görgetve. */
@Composable
fun <T> ChipRow(options: List<T>, selected: T?, label: @Composable (T) -> String, accent: Color, onSelect: (T) -> Unit) {
    val tick = rememberTick()
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { o ->
            val on = o == selected
            Text(
                label(o), style = MaterialTheme.typography.labelLarge, color = if (on) Noc.abyss else Noc.text2,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) accent else Noc.card)
                    .border(1.dp, if (on) accent else Noc.stroke, RoundedCornerShape(50))
                    .clickable { tick(); onSelect(o) }.padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
fun TextIn(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier.fillMaxWidth(), lines: Int = 1, placeholder: String? = null) {
    OutlinedTextField(
        value, onChange, modifier, label = { Text(label) }, singleLine = lines == 1, minLines = lines,
        placeholder = placeholder?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    )
}

@Composable
fun NumIn(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier.fillMaxWidth(), suffix: String? = null, decimal: Boolean = false) {
    OutlinedTextField(
        value, { v -> onChange(v.filter { it.isDigit() || (decimal && (it == ',' || it == '.')) }) }, modifier,
        label = { Text(label) }, singleLine = true, suffix = suffix?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
    )
}

fun String.toNum(): Double? = replace(',', '.').toDoubleOrNull()

/** Kis kör alakú ikonhely színes háttérrel. */
@Composable
fun IconBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, size: Int = 38) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size((size * 0.52).dp), tint = color)
    }
}

/** Oszlopdiagram (pl. heti alvás, mozgás, költés); a cél szaggatott vonal nélkül, csak színnel. */
@Composable
fun MiniBars(values: List<Double>, labels: List<String>, color: Color, goal: Double? = null, modifier: Modifier = Modifier) {
    val mx = maxOf(values.maxOrNull() ?: 0.0, goal ?: 0.0, 1.0)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, v ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 2.dp).height(100.dp), contentAlignment = Alignment.BottomCenter) {
                    val h = (v / mx * 100).coerceAtLeast(if (v > 0) 4.0 else 2.0)
                    val hit = goal != null && v >= goal
                    Box(Modifier.fillMaxWidth().height(h.dp).clip(RoundedCornerShape(6.dp))
                        .background(if (v == 0.0) Noc.well else if (hit) color else color.copy(alpha = 0.55f)))
                }
                Text(labels.getOrElse(i) { "" }, style = MaterialTheme.typography.labelSmall, color = Noc.text3, maxLines = 1)
            }
        }
    }
}

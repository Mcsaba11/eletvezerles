package hu.lifeos.app.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hu.lifeos.app.R
import hu.lifeos.app.food.Food
import hu.lifeos.app.food.FoodLogic
import hu.lifeos.app.food.FoodState
import hu.lifeos.app.ui.theme.StatusColors

fun rateColor(r: String): Color = when (r) {
    "g" -> StatusColors.ok
    "m" -> StatusColors.warn
    "b" -> StatusColors.bad
    else -> Color.Gray
}

@Composable
fun RatePill(r: String, label: String?) {
    if (label.isNullOrBlank()) return
    val c = rateColor(r)
    Text(
        label, style = MaterialTheme.typography.labelSmall, color = c,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(c.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

fun fmt(v: Double, d: Int = 0) = FoodLogic.fmt(v, d)

/** Kereső mező javaslatlistával (előtagos találatok elöl, utána a tartalmazók). */
@Composable
fun FoodSearchField(
    s: FoodState, logic: FoodLogic, rtxt: Map<String, String>, label: String,
    excludeRecipes: Boolean = false, onPick: (Food) -> Unit, modifier: Modifier = Modifier,
) {
    var q by remember { mutableStateOf("") }
    var open by remember { mutableStateOf(false) }
    val nq = FoodLogic.norm(q.trim())
    val matches = remember(nq, s.custom, s.recipes) {
        if (nq.isEmpty()) emptyList() else {
            val all = logic.allFoods(s).filter { !excludeRecipes || it.src != "recipe" }
            (all.filter { FoodLogic.norm(it.name).startsWith(nq) } + all.filter { !FoodLogic.norm(it.name).startsWith(nq) && FoodLogic.norm(it.name).contains(nq) }).take(10)
        }
    }
    Column(modifier) {
        OutlinedTextField(
            q, { q = it; open = true }, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
        )
        DropdownMenu(expanded = open && matches.isNotEmpty(), onDismissRequest = { open = false }, properties = androidx.compose.ui.window.PopupProperties(focusable = false)) {
            matches.forEach { f ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(f.name, modifier = Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.width(6.dp))
                            RatePill(f.r, rtxt[f.r])
                        }
                    },
                    onClick = { q = f.name; open = false; onPick(f) },
                )
            }
        }
    }
}

@Composable
fun <T> Picker(label: String, options: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(text(selected), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Outlined.ArrowDropDown, null)
        }
        DropdownMenu(open, { open = false }) {
            options.forEach { o -> DropdownMenuItem(text = { Text(text(o)) }, onClick = { onSelect(o); open = false }) }
        }
    }
}

@Composable
fun MacroBar(label: String, value: Double, goal: Double, unit: String, mode: String) {
    val pct = if (goal > 0) (value / goal).toFloat() else 0f
    val color = when {
        mode == "max" && value > goal * 1.05 -> StatusColors.bad
        mode == "min" && value >= goal -> StatusColors.ok
        else -> MaterialTheme.colorScheme.primary
    }
    Column(Modifier.padding(vertical = 4.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text("${fmt(value)} / ${fmt(goal)} $unit", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { pct.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
            color = color, trackColor = MaterialTheme.colorScheme.surfaceContainerHighest, strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {},
        )
    }
}

@Composable
fun StatTile(label: String, value: String, sub: String = "", highlight: Boolean = false, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp))
            .background(if (highlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge)
        if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Két oszlopos rács egyszerű elemekből (LazyColumn-on belül is használható). */
@Composable
fun TileGrid(tiles: List<@Composable (Modifier) -> Unit>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { it(Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ClickRow(onClick: () -> Unit, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 6.dp)) { content() }
}

@Composable
fun foodRtxt(r: String, rtxt: Map<String, String>) = rtxt[r] ?: stringResource(R.string.food_rate_none)

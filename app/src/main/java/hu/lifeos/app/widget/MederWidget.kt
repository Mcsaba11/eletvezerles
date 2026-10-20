package hu.lifeos.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import hu.lifeos.app.MainActivity
import hu.lifeos.app.R
import hu.lifeos.app.container
import hu.lifeos.app.insight.Insights
import hu.lifeos.app.data.Modules
import hu.lifeos.app.food.packFlags
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Kezdőképernyő-widget: Satori napok, böjt állapota, mai teendők; gombok a gyors műveletekhez. */
class MederWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val c = context.container
        if (!c.satori.loaded.value) c.satori.load()
        if (!c.food.loaded.value) c.food.load()
        val st = c.satori.state.value
        val fs = c.food.state.value
        val today = LocalDate.now().toString()
        val openTodos = c.db.todos().all().count { !it.done && it.dueDate != null && it.dueDate <= today }
        val settings = c.settings.flow.first()
        val satoriOn = settings.moduleOn(Modules.SATORI) && st.onboardingDone == true
        val fastOn = settings.moduleOn(Modules.FOOD) && fs.packFlags().fast
        val streak = st.cur().streak
        val bigLeft = if (satoriOn) "$streak " + context.getString(R.string.satori_day_word) else hu.lifeos.app.ui.Fmt.dayShort(LocalDate.now())
        val typeLabel = if (satoriOn) c.satori.content.type(st.type).label else context.getString(R.string.today)
        val f = Insights.fast(fs, c.food.logic)
        val fastText = if (!fastOn) "" else if (f.eating) context.getString(R.string.widget_eating, f.minutesLeft / 60, f.minutesLeft % 60)
        else context.getString(R.string.widget_fasting, f.minutesIn / 60, f.minutesIn % 60)

        fun open(action: String?) = actionStartActivity(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .also { if (action != null) it.putExtra(MainActivity.EXTRA_ACTION, action) }
        )

        val mode = settings.widgetMode
        c.mods.trackers.ensureLoaded()
        val td = c.mods.trackers.state.value
        val trackers = if (mode != "summary" && settings.moduleOn(Modules.TRACKERS)) td.trackers.filter { it.onHome }.take(4) else emptyList()
        val showSummary = mode != "trackers" || trackers.isEmpty()

        provideContent {
            val bg = ColorProvider(Color(0xFF0A0F1D))
            val card = ColorProvider(Color(0xFF161F36))
            val text = ColorProvider(Color(0xFFF8FAFC))
            val text2 = ColorProvider(Color(0xFF94A3B8))
            val cyan = ColorProvider(Color(0xFF00F2FE))
            val amber = ColorProvider(Color(0xFFF59E0B))
            val good = ColorProvider(Color(0xFF34D399))
            val sos = ColorProvider(Color(0xFFF43F5E))
            Column(GlanceModifier.fillMaxSize().background(bg).cornerRadius(24.dp).padding(14.dp).clickable(open(null))) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(context.getString(R.string.app_name), style = TextStyle(color = text, fontSize = 16.sp, fontWeight = FontWeight.Bold), modifier = GlanceModifier.defaultWeight())
                    Text(fastText, style = TextStyle(color = if (f.eating) cyan else amber, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                }
                if (showSummary) {
                    Spacer(GlanceModifier.height(8.dp))
                    Row(GlanceModifier.fillMaxWidth()) {
                        Column(GlanceModifier.defaultWeight().background(card).cornerRadius(16.dp).padding(10.dp)) {
                            Text(bigLeft, style = TextStyle(color = text, fontSize = 20.sp, fontWeight = FontWeight.Bold))
                            Text(typeLabel, style = TextStyle(color = amber, fontSize = 11.sp))
                        }
                        Spacer(GlanceModifier.width(8.dp))
                        Column(GlanceModifier.defaultWeight().background(card).cornerRadius(16.dp).padding(10.dp)) {
                            Text("$openTodos", style = TextStyle(color = text, fontSize = 20.sp, fontWeight = FontWeight.Bold))
                            Text(context.getString(R.string.widget_todos), style = TextStyle(color = cyan, fontSize = 11.sp))
                        }
                    }
                }
                if (trackers.isNotEmpty()) {
                    Spacer(GlanceModifier.height(8.dp))
                    Row(GlanceModifier.fillMaxWidth()) {
                        trackers.forEachIndexed { i, t ->
                            if (i > 0) Spacer(GlanceModifier.width(6.dp))
                            val v = td.value(t.id, today)
                            val hit = if (t.kind == "scale") v > 0 else v >= t.goal
                            val label = when (t.kind) {
                                "check" -> (if (hit) "✓ " else "○ ") + t.name
                                "count", "number" -> t.name + " " + hu.lifeos.app.mods.ui.fmtVal(v) + "/" + hu.lifeos.app.mods.ui.fmtVal(t.goal)
                                else -> t.name + if (v > 0) " " + v.toInt() else ""
                            }
                            val click = if (t.kind == "check" || t.kind == "count")
                                actionRunCallback<TrackerAction>(actionParametersOf(TrackerAction.KEY to t.id)) else open("open:trackers")
                            Box(GlanceModifier.defaultWeight().background(card).cornerRadius(14.dp).padding(8.dp).clickable(click), contentAlignment = Alignment.Center) {
                                Text(label, style = TextStyle(color = if (hit) good else text2, fontSize = 12.sp, fontWeight = FontWeight.Medium), maxLines = 2)
                            }
                        }
                    }
                }
                if (mode == "summary" || trackers.isEmpty()) {
                    Spacer(GlanceModifier.height(8.dp))
                    Row(GlanceModifier.fillMaxWidth()) {
                        Box(GlanceModifier.defaultWeight().background(card).cornerRadius(14.dp).padding(8.dp).clickable(open("capture")), contentAlignment = Alignment.Center) {
                            Text("+ " + context.getString(R.string.sc_capture), style = TextStyle(color = cyan, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                        }
                        Spacer(GlanceModifier.width(8.dp))
                        Box(GlanceModifier.background(card).cornerRadius(14.dp).padding(8.dp).clickable(open("breath")), contentAlignment = Alignment.Center) {
                            Text(context.getString(R.string.sc_breath_short), style = TextStyle(color = amber, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                        }
                        if (satoriOn) {
                            Spacer(GlanceModifier.width(8.dp))
                            Box(GlanceModifier.background(card).cornerRadius(14.dp).padding(8.dp).clickable(open("sos")), contentAlignment = Alignment.Center) {
                                Text("SOS", style = TextStyle(color = sos, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Widgetről pipálás / +1 egy saját követőhöz, az app megnyitása nélkül. */
class TrackerAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[KEY] ?: return
        val c = context.container
        c.mods.trackers.ensureLoaded()
        val today = LocalDate.now().toString()
        c.mods.trackers.updateNow { d ->
            val t = d.trackers.firstOrNull { it.id == id } ?: return@updateNow d
            val v = d.value(id, today)
            d.set(id, today, if (t.kind == "check") (if (v >= 1) 0.0 else 1.0) else v + 1)
        }
        MederWidget().update(context, glanceId)
    }

    companion object { val KEY = ActionParameters.Key<String>("tracker") }
}

class MederWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MederWidget()
}

object WidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    fun refresh(context: Context) {
        val app = context.applicationContext
        scope.launch { kotlinx.coroutines.delay(400); runCatching { MederWidget().updateAll(app) } }
    }
}

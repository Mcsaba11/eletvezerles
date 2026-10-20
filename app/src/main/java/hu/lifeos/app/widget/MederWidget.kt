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
        val streak = st.cur().streak
        val typeLabel = c.satori.content.type(st.type).label
        val f = Insights.fast(fs, c.food.logic)
        val fastText = if (f.eating) context.getString(R.string.widget_eating, f.minutesLeft / 60, f.minutesLeft % 60)
        else context.getString(R.string.widget_fasting, f.minutesIn / 60, f.minutesIn % 60)

        fun open(action: String?) = actionStartActivity(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .also { if (action != null) it.putExtra(MainActivity.EXTRA_ACTION, action) }
        )

        provideContent {
            val bg = ColorProvider(Color(0xFF0A0F1D))
            val card = ColorProvider(Color(0xFF161F36))
            val text = ColorProvider(Color(0xFFF8FAFC))
            val cyan = ColorProvider(Color(0xFF00F2FE))
            val amber = ColorProvider(Color(0xFFF59E0B))
            val sos = ColorProvider(Color(0xFFF43F5E))
            Column(GlanceModifier.fillMaxSize().background(bg).cornerRadius(24.dp).padding(14.dp).clickable(open(null))) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(context.getString(R.string.app_name), style = TextStyle(color = text, fontSize = 16.sp, fontWeight = FontWeight.Bold), modifier = GlanceModifier.defaultWeight())
                    Text(fastText, style = TextStyle(color = if (f.eating) cyan else amber, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                }
                Spacer(GlanceModifier.height(8.dp))
                Row(GlanceModifier.fillMaxWidth()) {
                    Column(GlanceModifier.defaultWeight().background(card).cornerRadius(16.dp).padding(10.dp)) {
                        Text("$streak " + context.getString(R.string.satori_day_word), style = TextStyle(color = text, fontSize = 20.sp, fontWeight = FontWeight.Bold))
                        Text(typeLabel, style = TextStyle(color = amber, fontSize = 11.sp))
                    }
                    Spacer(GlanceModifier.width(8.dp))
                    Column(GlanceModifier.defaultWeight().background(card).cornerRadius(16.dp).padding(10.dp)) {
                        Text("$openTodos", style = TextStyle(color = text, fontSize = 20.sp, fontWeight = FontWeight.Bold))
                        Text(context.getString(R.string.widget_todos), style = TextStyle(color = cyan, fontSize = 11.sp))
                    }
                }
                Spacer(GlanceModifier.height(8.dp))
                Row(GlanceModifier.fillMaxWidth()) {
                    Box(GlanceModifier.defaultWeight().background(card).cornerRadius(14.dp).padding(8.dp).clickable(open("capture")), contentAlignment = Alignment.Center) {
                        Text("+ " + context.getString(R.string.sc_capture), style = TextStyle(color = cyan, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                    }
                    Spacer(GlanceModifier.width(8.dp))
                    Box(GlanceModifier.background(card).cornerRadius(14.dp).padding(8.dp).clickable(open("breath")), contentAlignment = Alignment.Center) {
                        Text(context.getString(R.string.sc_breath_short), style = TextStyle(color = amber, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                    }
                    Spacer(GlanceModifier.width(8.dp))
                    Box(GlanceModifier.background(card).cornerRadius(14.dp).padding(8.dp).clickable(open("sos")), contentAlignment = Alignment.Center) {
                        Text("SOS", style = TextStyle(color = sos, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

class MederWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MederWidget()
}

object WidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    fun refresh(context: Context) {
        val app = context.applicationContext
        scope.launch { runCatching { MederWidget().updateAll(app) } }
    }
}

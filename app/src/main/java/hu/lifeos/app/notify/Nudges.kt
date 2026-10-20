package hu.lifeos.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import hu.lifeos.app.MainActivity
import hu.lifeos.app.R
import hu.lifeos.app.container
import hu.lifeos.app.data.AppSettings
import hu.lifeos.app.data.Modules
import hu.lifeos.app.mods.ui.LearnStats
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * A modulok értesítései: reggeli összefoglaló, lefekvés, tanulás, mozgás.
 * Mindegyik naponta egyszer fut (WorkManager), és csak akkor jelez, ha van mit mondania.
 */
object Nudges {
    const val CHANNEL = "nudges"
    const val MORNING = "morning"
    const val BEDTIME = "bedtime"
    const val LEARN = "learn"
    const val MOVE = "move"

    private fun work(kind: String) = "nudge_$kind"

    /** Az összes értesítés újraütemezése a beállítások szerint (indításkor és háttérbe lépéskor). */
    suspend fun scheduleAll(context: Context) {
        val c = context.container
        val s = c.settings.flow.first()
        val bedGoal = runCatching { LocalTime.parse(c.mods.sleep.state.value.bedGoal) }.getOrDefault(LocalTime.of(23, 0))
        plan(context, MORNING, s.notifMorning, s.morningTime)
        plan(context, BEDTIME, s.notifBedtime && s.moduleOn(Modules.SLEEP), bedGoal.minusMinutes(30).toString())
        plan(context, LEARN, s.notifLearn && s.moduleOn(Modules.LEARN), s.learnTime)
        plan(context, MOVE, s.notifMove && s.moduleOn(Modules.MOVE), s.moveTime)
        // A mini-appok emlékeztető kockái (a törölt kockák munkája a futáskor csendben véget ér).
        c.mods.miniapps.ensureLoaded()
        val mini = c.mods.miniapps.state.value
        mini.apps.flatMap { a -> a.blocks.filter { it.type == hu.lifeos.app.mods.BlockTypes.REMINDER } }.forEach { b ->
            plan(context, "mini_" + b.id, s.moduleOn(Modules.TRACKERS), b.cfg.time)
        }
    }

    private fun plan(context: Context, kind: String, on: Boolean, time: String) {
        val wm = WorkManager.getInstance(context)
        if (!on) { wm.cancelUniqueWork(work(kind)); return }
        val t = runCatching { LocalTime.parse(time) }.getOrDefault(LocalTime.of(8, 0))
        val now = LocalDateTime.now()
        var next = LocalDate.now().atTime(t)
        if (!next.isAfter(now.plusSeconds(30))) next = next.plusDays(1)
        val req = OneTimeWorkRequestBuilder<NudgeWorker>()
            .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("kind" to kind))
            .build()
        // A következő időpontra ütemez; a csere ugyanarra az időpontra esik, így nem tolja el.
        wm.enqueueUniqueWork(work(kind), ExistingWorkPolicy.REPLACE, req)
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.nudge_channel), NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    fun show(context: Context, id: Int, title: String, text: String, route: String?) {
        if (!Reminders.canNotify(context)) return
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (route != null) intent.putExtra(MainActivity.EXTRA_ACTION, "open:$route")
        val pi = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, n) }
    }

    /** A reggeli összefoglaló sorai (üres lista = nincs mit mondani). */
    suspend fun morningLines(context: Context, s: AppSettings): List<Pair<String, String>> {
        val c = context.container
        c.mods.loadAll()
        val today = LocalDate.now()
        val out = mutableListOf<Pair<String, String>>()
        if (s.moduleOn(Modules.PEOPLE)) {
            val people = c.mods.people.state.value.people
            people.filter { it.birthdayIn(today) == 0 }.forEach { out += "people" to context.getString(R.string.nudge_bday, it.name) }
            people.filter { it.birthdayIn(today) == 3 }.forEach { out += "people" to context.getString(R.string.nudge_bday_soon, it.name) }
            val due = people.filter { (it.dueIn(today) ?: 1) <= 0 && it.last() != null }
            if (due.isNotEmpty()) out += "people" to context.getString(R.string.nudge_people, due.take(3).joinToString(", ") { it.name })
        }
        if (s.moduleOn(Modules.HOUSE)) {
            val due = c.mods.house.state.value.chores.filter { it.dueIn(today) <= 0 }
            if (due.isNotEmpty()) out += "house" to context.getString(R.string.nudge_chores, due.take(3).joinToString(", ") { it.name })
        }
        if (s.moduleOn(Modules.MONEY)) {
            val ym = YearMonth.now().toString()
            val bills = c.mods.money.state.value.recurring.filter { !it.income && ym !in it.paid && it.day <= today.dayOfMonth }
            if (bills.isNotEmpty()) out += "money" to context.getString(R.string.nudge_bills, bills.take(3).joinToString(", ") { it.name })
        }
        if (s.moduleOn(Modules.GOALS)) {
            c.mods.goals.state.value.goals.filter { !it.done && it.deadline != null }.forEach { g ->
                val left = runCatching { ChronoUnit.DAYS.between(today, LocalDate.parse(g.deadline)) }.getOrNull()
                if (left != null && left in 0..3) out += "goals" to context.getString(R.string.nudge_goal, g.title, left.toInt())
            }
        }
        if (s.moduleOn(Modules.TASKS)) {
            val n = c.db.todos().all().count { !it.done && it.dueDate != null && it.dueDate <= today.toString() }
            if (n > 0) out += "todos" to context.getString(R.string.nudge_todos, n)
        }
        return out
    }
}

class NudgeWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val c = ctx.container
        val s = c.settings.flow.first()
        c.mods.loadAll()
        val today = LocalDate.now().toString()
        when (inputData.getString("kind")) {
            Nudges.MORNING -> {
                val lines = Nudges.morningLines(ctx, s)
                if (lines.isNotEmpty()) {
                    val route = lines.map { it.first }.distinct().singleOrNull()
                    Nudges.show(ctx, 2001, ctx.getString(R.string.nudge_morning_title), lines.joinToString("\n") { "• " + it.second }, route)
                }
            }
            Nudges.BEDTIME -> Nudges.show(ctx, 2002, ctx.getString(R.string.nudge_bed_title), ctx.getString(R.string.nudge_bed_text), "sleep")
            Nudges.LEARN -> {
                val d = c.mods.learn.state.value
                val m = LearnStats.minutesOn(d, today)
                if (m < d.dailyGoal) Nudges.show(ctx, 2003, ctx.getString(R.string.nudge_learn_title), ctx.getString(R.string.nudge_learn_text, d.dailyGoal - m), "learn")
            }
            Nudges.MOVE -> {
                val moved = c.mods.move.state.value.items.any { it.date == today }
                if (!moved) Nudges.show(ctx, 2004, ctx.getString(R.string.nudge_move_title), ctx.getString(R.string.nudge_move_text), "move")
            }
            else -> {
                val kind = inputData.getString("kind").orEmpty()
                if (kind.startsWith("mini_")) {
                    val bid = kind.removePrefix("mini_")
                    val doc = c.mods.miniapps.state.value
                    val app = doc.apps.firstOrNull { a -> a.blocks.any { it.id == bid } }
                    val b = app?.blocks?.firstOrNull { it.id == bid }
                    if (app == null || b == null) return Result.success()
                    Nudges.show(ctx, 3000 + (bid.hashCode() and 0xFFF), app.name, b.cfg.text.ifBlank { b.title.ifBlank { app.name } }, "mini/" + app.id)
                }
            }
        }
        Nudges.scheduleAll(ctx)
        return Result.success()
    }
}

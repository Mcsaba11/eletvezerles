package hu.lifeos.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import hu.lifeos.app.MainActivity
import hu.lifeos.app.R
import hu.lifeos.app.container
import hu.lifeos.app.data.AppSettings
import hu.lifeos.app.satori.SatoriLogic
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Napi emlékeztető. A telefon akkor is jelez, ha az app zárva van (WorkManager).
 * Ha a mai nap már le van zárva, nem zavar.
 */
object Reminders {
    private const val WORK = "daily_reminder"
    const val CHANNEL = "daily"

    fun schedule(context: Context, s: AppSettings) {
        val wm = WorkManager.getInstance(context)
        if (!s.reminderEnabled) { wm.cancelUniqueWork(WORK); return }
        val t = runCatching { LocalTime.parse(s.reminderTime) }.getOrDefault(LocalTime.of(20, 0))
        val now = LocalDateTime.now()
        var next = LocalDate.now().atTime(t)
        if (!next.isAfter(now.plusSeconds(30))) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMillis()
        val req = OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build()
        wm.enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, req)
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.notif_channel), NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val c = ctx.container
        val settings = c.settings.flow.first()
        c.satori.load()
        val st = c.satori.state.value
        val today = SatoriLogic.today()
        val closed = st.cur().dayOutcomeDate == today
        val openTodos = c.db.todos().all().count { !it.done && it.dueDate != null && it.dueDate <= today }

        if (Reminders.canNotify(ctx) && (!closed || openTodos > 0)) {
            Reminders.ensureChannel(ctx)
            val typeLabel = c.satori.content.type(st.type).label
            val text = when {
                !closed && openTodos > 0 -> ctx.getString(R.string.notif_both, typeLabel, openTodos)
                !closed -> ctx.getString(R.string.notif_dayclose, typeLabel)
                else -> ctx.getString(R.string.notif_todos, openTodos)
            }
            val pi = PendingIntent.getActivity(
                ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val n = NotificationCompat.Builder(ctx, Reminders.CHANNEL)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(ctx.getString(R.string.notif_title))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
            runCatching { NotificationManagerCompat.from(ctx).notify(1001, n) }
        }
        Reminders.schedule(ctx, settings)
        return Result.success()
    }
}

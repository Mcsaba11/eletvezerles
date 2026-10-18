package hu.lifeos.app.data

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone

data class PhoneCalendarInfo(
    val id: Long,
    val name: String,
    val account: String,
    val color: Int,
    val writable: Boolean,
)

data class PhoneEvent(
    val eventId: Long,
    val calendarId: Long,
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val location: String,
    val color: Int,
)

/**
 * A telefon naptára (ez szinkronizál a Google Naptárral is). Csak engedéllyel működik;
 * engedély nélkül üres listát ad, nem dob hibát.
 */
class PhoneCalendar(private val context: Context) {

    fun canRead(): Boolean = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    fun canWrite(): Boolean = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

    suspend fun calendars(): List<PhoneCalendarInfo> = withContext(Dispatchers.IO) {
        if (!canRead()) return@withContext emptyList()
        val out = mutableListOf<PhoneCalendarInfo>()
        val proj = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.VISIBLE,
        )
        runCatching {
            context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, proj, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    if (c.getInt(5) == 0) continue
                    val access = c.getInt(4)
                    out += PhoneCalendarInfo(
                        id = c.getLong(0),
                        name = c.getString(1) ?: "",
                        account = c.getString(2) ?: "",
                        color = c.getInt(3),
                        writable = access >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR,
                    )
                }
            }
        }
        out
    }

    suspend fun events(from: Long, to: Long, hidden: Set<Long>): List<PhoneEvent> = withContext(Dispatchers.IO) {
        if (!canRead()) return@withContext emptyList()
        val out = mutableListOf<PhoneEvent>()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, from)
            ContentUris.appendId(it, to)
        }.build()
        val proj = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.DISPLAY_COLOR,
        )
        runCatching {
            context.contentResolver.query(uri, proj, null, null, CalendarContract.Instances.BEGIN + " ASC")?.use { c ->
                while (c.moveToNext()) {
                    val calId = c.getLong(1)
                    if (calId in hidden) continue
                    val allDay = c.getInt(5) == 1
                    var begin = c.getLong(3)
                    var end = c.getLong(4)
                    if (allDay) {
                        // Az egész napos események UTC-éjféltől UTC-éjfélig tartanak: helyi időre igazítjuk.
                        val off = TimeZone.getDefault().getOffset(begin)
                        begin -= off; end -= off
                    }
                    out += PhoneEvent(
                        eventId = c.getLong(0), calendarId = calId, title = c.getString(2) ?: "",
                        begin = begin, end = end, allDay = allDay,
                        location = c.getString(6) ?: "", color = c.getInt(7),
                    )
                }
            }
        }
        out
    }

    suspend fun insert(calendarId: Long, title: String, begin: Long, end: Long, allDay: Boolean, location: String, note: String): Boolean =
        withContext(Dispatchers.IO) {
            if (!canWrite()) return@withContext false
            runCatching {
                val tz = if (allDay) "UTC" else TimeZone.getDefault().id
                val (b, e) = if (allDay) {
                    val off = TimeZone.getDefault().getOffset(begin)
                    (begin + off) to (end + off)
                } else begin to end
                val v = ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(CalendarContract.Events.TITLE, title)
                    put(CalendarContract.Events.DTSTART, b)
                    put(CalendarContract.Events.DTEND, e)
                    put(CalendarContract.Events.ALL_DAY, if (allDay) 1 else 0)
                    put(CalendarContract.Events.EVENT_TIMEZONE, tz)
                    put(CalendarContract.Events.EVENT_LOCATION, location)
                    put(CalendarContract.Events.DESCRIPTION, note)
                }
                context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, v) != null
            }.getOrDefault(false)
        }
}

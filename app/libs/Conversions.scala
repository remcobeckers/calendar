package libs

import java.text.SimpleDateFormat
import java.sql.Timestamp
import java.util.Date
import java.util.Calendar

object Conversions {

  val format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  implicit def stringToDate(s: String): Date = new Date(format.parse(s).getTime())
  implicit def dateToString(t: Date): String = format.format(t.getTime())

  implicit def dateToCalendar(d: Date): Calendar = {
    val c = Calendar.getInstance;
    c.setTime(d);
    c
  }

  def extractTimeInMillis(d: Date) = {
    ((d.get(Calendar.HOUR_OF_DAY) * 60 + d.get(Calendar.MINUTE)) * 60 + d.get(Calendar.SECOND)) * 1000
  }
}
package models

// Import the session management, including the implicit threadLocalSession
import org.scalaquery.session._
import org.scalaquery.session.Database.threadLocalSession
import org.scalaquery.ql._
import org.scalaquery.ql.TypeMapper._
import org.scalaquery.ql.extended.PostgresDriver.Implicit._
import org.scalaquery.ql.extended.{ ExtendedTable => Table }
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar

case class Event(id: Option[Int] = None, title: String, start: Date, end: Date, description: String, allDay: Boolean) {
  //   end: Timestamp, require(start before end)
}

object Events extends Table[Event]("event") {

  implicit val JavaUtilDateTypeMapper = MappedTypeMapper.base[Date, Timestamp](
    (d: Date) => new Timestamp(d.getTime),
    (t: Timestamp) => new Date(t.getTime()))

  // This is the primary key column
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

  def title = column[String]("title")
  def start = column[Date]("start")
  def end = column[Date]("end")
  def description = column[String]("description")
  def allDay = column[Boolean]("allDay")

  // Every table needs a * projection with the same type as the table's type parameter
  def * = id.? ~ title ~ start ~ end ~ description ~ allDay <> (Event.apply _, Event.unapply _)
  def findById(id: Int) = (for (e <- models.Events if e.id === id) yield e).take(1).firstOption

  def move(id: Int, dayDelta: Int, minuteDelta: Int, allDay: Boolean) = {
    val q = for (e <- Events if e.id === id) yield e.id ~ e.start ~ e.end ~ e.allDay
    q.mutate { r =>
      r.row = r.row.copy(r.row._1, addTime(r.row._2, dayDelta, minuteDelta), addTime(r.row._3, dayDelta, minuteDelta), allDay)
    }
  }

  def resize(id: Int, dayDelta: Int, minuteDelta: Int) = {
    val q = for (e <- Events if e.id === id) yield e.id ~ e.end
    q.mutate { r =>
      r.row = r.row.copy(r.row._1, addTime(r.row._2, dayDelta, minuteDelta))
    }
  }

  def update(newEvent: Event) = {
    val q = for (e <- Events if e.id === newEvent.id) yield e
    q.update(newEvent)
  }

  def insert(e: Event) = {
    title ~ start ~ end ~ description ~ allDay insert (e.title, e.start, e.end, e.description, e.allDay)
  }

  private def addTime(d: Date, dayDelta: Int, minuteDelta: Int) = {
    val startDate = Calendar.getInstance
    startDate.setTime(d)
    startDate.add(Calendar.MINUTE, minuteDelta)
    startDate.add(Calendar.DAY_OF_MONTH, dayDelta)
    startDate.getTime
  }

  def insertTestData = {
    Events.title ~ Events.start ~ end ~ description ~ allDay insertAll (
      ("Kapper", Timestamp.valueOf("2012-11-03 11:30:00"), Timestamp.valueOf("2012-11-03 12:30:00"), "Naar de kapper", false),
      ("Verjaardag Henk", Timestamp.valueOf("2012-11-10 00:00:00"), Timestamp.valueOf("2012-11-11 00:00:00"), "Klein biertje drinken", false),
      ("Dokter", Timestamp.valueOf("2012-11-14 08:15:00"), Timestamp.valueOf("2012-11-14 08:30:00"), "Pijn aan mijn teen", false))
  }

}


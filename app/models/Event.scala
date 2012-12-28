package models

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar

case class Event(id: Option[Int] = None, title: String, start: Date, end: Date, description: String, allDay: Boolean, userId: Int) {
  //   end: Timestamp, require(start before end)
}

trait EventsComponent {
  this: Profile with UserComponent =>
  import profile.simple._
  import Database.threadLocalSession

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
    def userId = column[Int]("userid")

    def user = foreignKey("USER_FK", userId, Users)(_.id)

    // Every table needs a * projection with the same type as the table's type parameter
    def * = id.? ~ title ~ start ~ end ~ description ~ allDay ~ userId <> (Event, Event.unapply _)

    def findById(id: Int, userId: Int) = (for (e <- Events if e.id === id && e.userId === userId) yield e).take(1).firstOption

    def findByUserId(userId: Int) = (for (e <- Events if e.userId === userId) yield e).list

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

    def update(newEvent: Event, userId: Int) = {
      val q = for (e <- Events if e.id === newEvent.id && e.userId === userId) yield e
      q.update(newEvent)
    }

    def insert(e: Event) = {
      title ~ start ~ end ~ description ~ allDay ~ userId insert (e.title, e.start, e.end, e.description, e.allDay, e.userId)
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
}


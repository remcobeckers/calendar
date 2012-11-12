import play.api.db.DB
import play.api.GlobalSettings
import play.api.Application
import play.api.Play.current
import org.scalaquery.session.Database
import models.Bars
import org.scalaquery.ql.extended.H2Driver.Implicit._
import org.scalaquery.session._
import org.scalaquery.session.Database.threadLocalSession
import models.Events
import models.Event
import java.sql.Timestamp

object Global extends GlobalSettings {

  override def onStart(app: Application) {

    lazy val database = Database.forDataSource(DB.getDataSource())

    database.withSession {
      (Bars.ddl ++ Events.ddl).create

      insertTestData
    }
  }

  def insertTestData = {
    Events.insertAll(
      Event(None, "Kapper", Timestamp.valueOf("2012-11-03 11:30:00"), Timestamp.valueOf("2012-11-03 12:30:00"), "Naar de kapper", false),
      Event(None, "Verjaardag Henk", Timestamp.valueOf("2012-11-10 00:00:00"), Timestamp.valueOf("2012-11-11 00:00:00"), "Klein biertje drinken", false),
      Event(None, "Dokter", Timestamp.valueOf("2012-11-14 08:15:00"), Timestamp.valueOf("2012-11-14 08:30:00"), "Pijn aan mijn teen", false))
  }
}
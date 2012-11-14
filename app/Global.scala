import play.api.db.DB
import play.api.GlobalSettings
import play.api.Application
import play.api.Play.current
import org.scalaquery.session.Database
import models.Bars
import org.scalaquery.ql.extended.PostgresDriver.Implicit._
import org.scalaquery.session._
import org.scalaquery.session.Database.threadLocalSession
import models.Events
import models.Event
import java.sql.Timestamp
import org.scalaquery.meta.MTable
import org.scalaquery.ql.extended.ExtendedTable
import org.scalaquery.ql.DDL

object Global extends GlobalSettings {
  lazy val existingTables = makeTableMap

  private def makeTableMap: Map[String, MTable] = {
    val tableList = MTable.getTables.list()
    tableList.map { t => (t.name.name, t) }.toMap
  }

  private def nonExistentTables(tables: Seq[ExtendedTable[_]]): Seq[DDL] = {
    for {
      t <- tables
      if !existingTables.contains(t.tableName)
    } yield t.ddl
  }

  private def createTables(tables: Seq[ExtendedTable[_]]): Unit = {
    val newTables = nonExistentTables(tables)
    if (newTables.size > 0) newTables.reduce(_ ++ _).create
  }

  override def onStart(app: Application) {

    lazy val database = Database.forDataSource(DB.getDataSource())

    database.withSession {
      createTables(List(Bars, Events))
      if (!existingTables.contains(Events.tableName)) Events.insertTestData
    }
  }

}
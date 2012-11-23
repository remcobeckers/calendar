import play.api.db.DB
import play.api.GlobalSettings
import play.api.Application
import play.api.Play.current
import org.scalaquery.session.Database
import org.scalaquery.ql.extended.PostgresDriver.Implicit._
import org.scalaquery.session._
import org.scalaquery.session.Database.threadLocalSession
import models.Events
import models.Event
import java.sql.Timestamp
import org.scalaquery.meta.MTable
import org.scalaquery.ql.extended.ExtendedTable
import org.scalaquery.ql.DDL
import play.api.mvc.RequestHeader
import play.api.mvc.Handler
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.Play
import play.api.libs.iteratee.Done
import play.api.libs.iteratee.Input
import play.api.mvc.Results
import models.Users
import org.scalaquery.simple.StaticQuery
import org.scalaquery.ql.Query
import play.api.Logger

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
    if (newTables.size > 1) newTables.reduce(_ ++ _).create
    else if (newTables.size == 1) newTables.head.create
  }

  /**
   * Redirect to HTTPS if the user is not using HTTPS
   */
  private def onNotHttps()(implicit request: RequestHeader) = Results.Redirect("https://" + request.domain + request.uri).flashing("info" -> "Always using HTTPS for security purposes")

  /**
   * Action to check that https if not execute onNotHttps method
   */
  def IsHttps[A](action: Action[A]): Action[(Action[A], A)] = {

    val authenticatedBodyParser = BodyParser { implicit request =>
      val proto = for (
        proto <- request.headers.get("x-forwarded-proto");
        if proto == "https"
      ) yield proto

      if (proto == None && Play.isProd)
        Done(Left(onNotHttps), Input.Empty)
      else
        action.parser(request).mapDone { body => body.right.map(innerBody => (action, innerBody)) }
    }

    Action(authenticatedBodyParser) { request =>
      val (innerAction, innerBody) = request.body
      innerAction(request.map(_ => innerBody))
    }

  }

  override def onStart(app: Application) {
    lazy val database = Database.forDataSource(DB.getDataSource())

    database.withSession {
      val addUserColumn = !existingTables.contains(Users.tableName)
      createTables(List(Events, Users))
      if (addUserColumn) addUserToEvents
    }
  }

  private def addUserToEvents(implicit session: Session) = {
    session.withPreparedStatement("ALTER TABLE event ADD COLUMN userid integer NOT NULL")(_.execute)
    session.withPreparedStatement("""ALTER TABLE event ADD CONSTRAINT "USER_FK" FOREIGN KEY (userid) REFERENCES "user" (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION""")(_.execute)
  }

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    //TODO: Add content-security-policy headers: http://www.html5rocks.com/en/tutorials/security/content-security-policy/
    super.onRouteRequest(request).map {
      case action: Action[_] => IsHttps(action)
      case other => other
    }
  }
}
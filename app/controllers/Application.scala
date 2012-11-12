package controllers

import org.scalaquery.ql._
import org.scalaquery.ql.TypeMapper._
import org.scalaquery.ql.extended.{ ExtendedTable => Table }
import org.scalaquery.ql.extended.H2Driver.Implicit._
import org.scalaquery.session._
import org.scalaquery.session._
import org.scalaquery.session.Database
import org.scalaquery.session.Database.threadLocalSession
import org.scalaquery.session.Database.threadLocalSession
import models.Bar
import models.Bars
import play.api._
import play.api.GlobalSettings
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.db.DB
import play.api.mvc._
import play.api.libs.json.Json

object Application extends Controller {

  lazy val database = Database.forDataSource(DB.getDataSource())

  val barForm = Form(
    mapping("name" -> text)((name) => Bar(None, name))((bar: Bar) => Some(bar.name)))

  def index = Action {
    Redirect(routes.Events.index)
    //    Ok(views.html.index(barForm))
  }

  def addBar = Action { implicit request =>
    barForm.bindFromRequest.value map {
      bar =>
        database withSession {
          (Bars insert bar)
        }
        Redirect(routes.Application.index())
    } getOrElse BadRequest
  }

  def getBars = Action {
    val json = database withSession {
      val bars = for (b <- Bars) yield b.name
      Json.toJson(bars.list)
    }
    Ok(json).as(JSON)
  }

}
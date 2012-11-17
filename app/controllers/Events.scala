package controllers

import libs.Conversions._
import play.api._
import play.api.GlobalSettings
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.db.DB
import play.api.mvc._
import play.api.libs.json.Json
import org.scalaquery.session.Database
import org.scalaquery.ql._
import org.scalaquery.ql.TypeMapper._
import org.scalaquery.ql.extended.PostgresDriver.Implicit._
import org.scalaquery.session.Database
import org.scalaquery.session.Database.threadLocalSession
import play.api.libs.json._
import java.text.SimpleDateFormat
import java.sql.Timestamp
import java.util.Date
import models.Event
import java.util.Calendar
import play.mvc.Security.Authenticated

object Events extends Controller with Secured {
  lazy val database = Database.forDataSource(DB.getDataSource())

  def includeOptionalTime(date: Date, time: Option[Date]) = {
    if (time.isDefined) new Date(date.getTime() + extractTimeInMillis(time.get)) else date
  }
  def extractOptionalTime(e: Event, d: Date): Option[Date] =
    Some(d)

  private val eventForm: Form[Event] = Form(mapping(
    "id" -> optional(number),
    "title" -> text,
    "startDate" -> date,
    "startTime" -> optional(date("HH:mm")),
    "endDate" -> date,
    "endTime" -> optional(date("HH:mm")),
    "description" -> text,
    "allDay" -> boolean)((id, title, startDate, startTime, endDate, endTime, description, allDay) => Event(id, title, includeOptionalTime(startDate, startTime), includeOptionalTime(endDate, endTime), description, allDay))((e: Event) => Some((e.id, e.title, e.start, extractOptionalTime(e, e.start), e.end, extractOptionalTime(e, e.end), e.description, e.allDay))))

  implicit object EventReads extends Format[Event] {
    def reads(json: JsValue) = Event(
      (json \ "id").as[Option[Int]],
      (json \ "title").as[String],
      (json \ "start").as[String],
      (json \ "end").as[String],
      (json \ "description").as[String],
      (json \ "allDay").as[Boolean])

    def writes(ts: Event) = JsObject(Seq(
      "title" -> JsString(ts.title),
      "start" -> JsString(ts.start),
      "end" -> JsString(ts.end),
      "description" -> JsString(ts.description),
      "allDay" -> JsBoolean(ts.allDay))
      ++ (ts.id match {
        case Some(n) => Seq("id" -> JsNumber(n))
        case _ => Seq()
      }))
  }

  def index = IsAuthenticated { _ =>
    Action {
      implicit request =>
        Ok(views.html.calendar())
    }
  }

  def events(start: String, end: String) = IsAuthenticated { _ =>
    Action {
      request =>
        val json = database withSession {
          val events = for (e <- models.Events) yield e
          Json.toJson(events.list)
        }
        Ok(json).as(JSON)
    }
  }

  def edit(eventId: Int) = IsAuthenticated { _ =>
    Action {
      implicit request =>
        val event = database withSession {
          models.Events.findById(eventId)
        }
        val form = event.map(eventForm.fill(_)) getOrElse eventForm
        Ok(views.html.editEvent(form))
    }
  }

  def move(eventId: Int) = IsAuthenticated { _ =>
    Action {
      implicit request =>
        val params = request.body.asFormUrlEncoded.get.map({ case (k, v) => k -> v.head })
        val msg = database withSession {
          models.Events.findById(eventId) match {
            case Some(event) =>
              models.Events.move(eventId, params("dayDelta").toInt, params("minuteDelta").toInt, params("allDay").toBoolean)
              "Event \"" + event.title + "\" moved!"
            case None => "Event not found"
          }
        }
        Ok(msg)
    }
  }

  def resize(eventId: Int) = IsAuthenticated { _ =>
    Action {
      implicit request =>
        val params = request.body.asFormUrlEncoded.get.map({ case (k, v) => k -> v.head })
        val msg = database withSession {
          models.Events.findById(eventId) match {
            case Some(event) =>
              models.Events.resize(eventId, params("dayDelta").toInt, params("minuteDelta").toInt)
              "Event \"" + event.title + "\" resized!"
            case None => "Event not found"
          }
        }
        Ok(msg)
    }
  }

  def newEvent(start: Option[String]) = IsAuthenticated { _ =>
    Action {
      implicit request =>
        val form = if (flash.get("error").isDefined) eventForm.bind(flash.data)
        else if (start.isDefined) eventForm.fill(Event(None, "", start.get, start.get, "", false))
        else eventForm
        Ok(views.html.editEvent(form))
    }
  }

  def save = IsAuthenticated { _ =>
    Action { implicit request =>
      val newEventForm = eventForm.bindFromRequest()

      newEventForm.fold(
        hasErrors = { form =>
          Redirect(routes.Events.newEvent(None)).flashing(Flash(form.data) + ("error" -> "Het is fout"))
        },
        success = { newEvent =>
          Logger.debug("event: " + newEvent)
          val message = database withSession {
            newEvent.id match {
              case Some(id) =>
                models.Events.update(newEvent)
                "\"" + newEvent.title + "\" updated!"
              case None =>
                models.Events.insert(newEvent)
                "New event \"" + newEvent.title + "\" created!"
            }
          }
          Redirect(routes.Events.index).flashing("success" -> message)
        })
    }
  }

}
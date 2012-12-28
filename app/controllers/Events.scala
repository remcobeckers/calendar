package controllers

import java.util.Date
import scala.math.BigDecimal.int2bigDecimal
import libs.Conversions.dateToString
import libs.Conversions.extractTimeInMillis
import libs.Conversions.stringToDate
import models.Event
import play.api.Logger
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms.boolean
import play.api.data.Forms.date
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.data.Forms.optional
import play.api.data.Forms.text
import play.api.db.DB
import play.api.libs.json.JsBoolean
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Flash
import models.DBAccess

object Events extends Controller with Secured with DBAccess {

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
    "allDay" -> boolean,
    "userId" -> text)((id, title, startDate, startTime, endDate, endTime, description, allDay, userId) => Event(id, title, includeOptionalTime(startDate, startTime), includeOptionalTime(endDate, endTime), description, allDay, userId.toInt))((e: Event) => Some((e.id, e.title, e.start, extractOptionalTime(e, e.start), e.end, extractOptionalTime(e, e.end), e.description, e.allDay, e.userId.toString))))

  implicit object EventWrites extends Writes[Event] {
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

  def events(start: String, end: String) = IsAuthenticated { userId =>
    Action {
      request =>
        val json = database withSession {
          val events = datamodel.Events.findByUserId(userId)
          Json.toJson(events)
        }
        Ok(json).as(JSON)
    }
  }

  def edit(eventId: Int) = IsAuthenticated { userId =>
    Action {
      implicit request =>
        val event = database withSession {
          datamodel.Events.findById(eventId, userId)
        }
        val form = event.map(eventForm.fill(_)) getOrElse eventForm
        Ok(views.html.editEvent(form))
    }
  }

  def move(eventId: Int) = IsAuthenticated { userId =>
    Action {
      implicit request =>
        val params = request.body.asFormUrlEncoded.get.map({ case (k, v) => k -> v.head })
        val msg = database withSession {
          datamodel.Events.findById(eventId, userId) match {
            case Some(event) =>
              datamodel.Events.move(eventId, params("dayDelta").toInt, params("minuteDelta").toInt, params("allDay").toBoolean)
              "Event \""+event.title+"\" moved!"
            case None => "Event not found"
          }
        }
        Ok(msg)
    }
  }

  def resize(eventId: Int) = IsAuthenticated { userId =>
    Action {
      implicit request =>
        val params = request.body.asFormUrlEncoded.get.map({ case (k, v) => k -> v.head })
        database withSession {
          datamodel.Events.findById(eventId, userId) match {
            case Some(event) =>
              for {
                dayDelta <- params.get("dayDelta")
                minuteDelta <- params.get("minuteDelta")
              } {
                datamodel.Events.resize(eventId, dayDelta.toInt, minuteDelta.toInt)
              }
              Ok("Event \""+event.title+"\" resized!")
            case None => NotFound("Event not found")
          }
        }
    }
  }

  def newEvent(start: Option[String]) = IsAuthenticated { userId =>
    Action {
      implicit request =>
        val form = if (flash.get("error").isDefined) eventForm.bind(flash.data)
        else if (start.isDefined) eventForm.fill(Event(None, "", start.get, start.get, "", false, userId))
        else eventForm
        Ok(views.html.editEvent(form))
    }
  }

  def save = IsAuthenticated { userId =>
    Action { implicit request =>
      val newEventForm = eventForm.bindFromRequest()

      newEventForm.fold(
        hasErrors = { form =>
          Redirect(routes.Events.newEvent(None)).flashing(Flash(form.data) + ("error" -> "Het is fout"))
        },
        success = { newEvent =>
          Logger.debug("event: "+newEvent)
          val message = database withSession {
            newEvent.id match {
              case Some(id) =>
                datamodel.Events.update(newEvent, userId)
                "\""+newEvent.title+"\" updated!"
              case None =>
                datamodel.Events.insert(newEvent)
                "New event \""+newEvent.title+"\" created!"
            }
          }
          Redirect(routes.Events.index).flashing("success" -> message)
        })
    }
  }

}
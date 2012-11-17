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
import play.api._
import play.api.GlobalSettings
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.db.DB
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.openid.OpenID
import play.api.libs.concurrent.Redeemed
import play.api.libs.concurrent.Thrown
import play.api.libs.iteratee._

object Application extends Controller with Secured {

  lazy val database = Database.forDataSource(DB.getDataSource())

  def index = Action { implicit request =>
    Ok(views.html.login())
  }

  def login = IsHttps {
    Action { implicit request =>
      val googleOpenid = "https://www.google.com/accounts/o8/id"
      AsyncResult(OpenID.redirectURL(googleOpenid, routes.Application.openIdVerify.absoluteURL(), Seq("email" -> "http://schema.openid.net/contact/email"))
        .extend(_.value match {
          case Redeemed(url) => Redirect(url)
          case Thrown(t) => Redirect(routes.Application.login)
        }))
    }
  }

  def openIdVerify = Action { implicit request =>
    AsyncResult(
      OpenID.verifiedId.extend(_.value match {
        case Redeemed(info) => {
          Redirect(routes.Events.index) withSession ("email" -> info.attributes("email"), "userid" -> info.id)
        }
        case Thrown(t) => {
          // Here you should look at the error, and give feedback to the user
          Redirect(routes.Application.index).flashing("error" -> ("Login failed " + t.getMessage()))
        }
      }))
  }

}
/**
 * Provide security features
 */
trait Secured {

  private def username(request: RequestHeader) = request.session.get("email")

  /**
   * Redirect to login if the user in not authorized.
   */
  private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Application.index)

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

  def IsAuthenticated[A](action: (String) ⇒ Action[A]) = IsHttps {
    Security.Authenticated(username, onUnauthorized)(action)
  }

}
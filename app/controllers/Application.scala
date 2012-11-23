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
import models.Users
import models.Users

object Application extends Controller with Secured {

  lazy val database = Database.forDataSource(DB.getDataSource())

  def index =
    Action { implicit request =>
      if (session.get("email") != None)
        Redirect(routes.Events.index)
      else
        Ok(views.html.login())
    }

  def login =
    Action { implicit request =>
      val googleOpenid = "https://www.google.com/accounts/o8/id"
      AsyncResult(OpenID.redirectURL(googleOpenid, routes.Application.openIdVerify.absoluteURL(),
        Seq("email" -> "http://schema.openid.net/contact/email",
          "firstname" -> "http://schema.openid.net/namePerson/first",
          "lastname" -> "http://schema.openid.net/namePerson/last"))
        .extend(_.value match {
          case Redeemed(url) => Redirect(url)
          case Thrown(t) => Redirect(routes.Application.login)
        }))
    }

  def openIdVerify = Action { implicit request =>
    AsyncResult(
      OpenID.verifiedId.extend(_.value match {
        case Redeemed(info) => {
          database withSession {
            val user = Users.findOrCreate(info.id) { user =>
              user.copy(email = info.attributes("email"), firstname = info.attributes("firstname"), lastname = info.attributes("lastname"))
            }
            user.id match {
              case Some(id) => Redirect(routes.Events.index) withSession ("userid" -> id.toString)
              case None => Redirect(routes.Application.index).flashing("error" -> "Login failed, user not found")
            }
          }
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

  private def userId(request: RequestHeader) = request.session.get("userid")

  /**
   * Redirect to login if the user in not authorized.
   */
  private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Application.index)

  def IsAuthenticated[A](f: (Int) => Action[A]) = Security.Authenticated(userId, onUnauthorized) { userId =>
    f(userId.toInt)
  }

}
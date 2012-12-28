import play.api.Application
import play.api.GlobalSettings
import play.api.Play
import play.api.Play.current
import play.api.libs.iteratee.Done
import play.api.libs.iteratee.Input
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.Handler
import play.api.mvc.RequestHeader
import play.api.mvc.Results

object Global extends GlobalSettings {

  /**
   * Redirect to HTTPS if the user is not using HTTPS
   */
  private def onNotHttps()(implicit request: RequestHeader) = Results.Redirect("https://"+request.domain + request.uri).flashing("info" -> "Always using HTTPS for security purposes")

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
  }

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    super.onRouteRequest(request).map {
      case action: Action[_] => IsHttps(action)
      case other => other
    }
  }
}
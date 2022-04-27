package controllers

import scala.concurrent.ExecutionContext.Implicits.global

import javax.inject._
import play.api.mvc._

import play.modules.reactivemongo.ReactiveMongoApi

import lib.Data._
import lib.Server
import lib.DBInterface

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(
  val controllerComponents: ControllerComponents,
  api: ReactiveMongoApi
) extends BaseController {

  val server = new Server(new DBInterface(api))

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }



  // ----------------------------------------------------------------------------------------------
  def screenings(date: String, from: String, to: String) = Action.async {
    _ => for {
      result <- server.serveListScreeningsRequest(date, from, to).value
    } yield responseFromEither(result)
  }

  // ----------------------------------------------------------------------------------------------
  def getScreening(screeningId: String) = Action.async {
    _ => for {
      result <- server.serveScreeningInfoRequst(screeningId).value
    } yield responseFromEither(result)
  }

  // ----------------------------------------------------------------------------------------------
  def postScreening(screeningId: String) = Action.async(parse.json) {
    implicit request => for {

      result <- server.serveReservationRequest(screeningId, request.body).value

    } yield responseFromEither(result)
  }

}

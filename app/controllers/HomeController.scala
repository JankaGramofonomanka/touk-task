package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._

import database.MockDataBase


/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

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

  def screenings(date: String, from: String, to: String) = Action { implicit request => 
    
    Ok(Json.arr(
      Json.obj("key" -> "val"),
      Json.obj("val" -> "key"),
    ))
  }

  def getScreening(id: String) = Action { _ =>
    Ok(Json.obj("key" -> "val", "val" -> "key", "id" -> id))
  }

  def postScreening(id: String) = Action(parse.json) { implicit request =>
    Ok(Json.obj("received" -> Json.toJson(request.body)))
  }
}

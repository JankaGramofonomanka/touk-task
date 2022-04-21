package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._

import scala.util.{Failure, Success, Try}

import com.github.nscala_time.time.Imports._

import lib.DataDefs._
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
    val startStr = s"$date $from"
    val endStr = s"$date $to"
    
    val interval = for {
      start <- Try(DateTimeFormat.forPattern("dd-MM-yyyy HH:mm").parseDateTime(startStr))
      end   <- Try(DateTimeFormat.forPattern("dd-MM-yyyy HH:mm").parseDateTime(endStr))
    } yield (start, end)

    interval match {
      case Failure(error) => Status(400)("Invalid parameters")
      
      case Success((start, end)) => {
        val screenings = MockDataBase.screenings.toList.filter(
          t => start <= t._2.start && t._2.start + t._2.duration <= end
        )

        val screeningInfos = screenings.map(t => screeningInfoBasic(t._1, t._2))
        Ok(Json.toJson(screeningInfos))
      }
    }
  }

  def getScreening(screeningId: String) = Action { _ =>
    Ok(Json.obj("key" -> "val", "val" -> "key", "id" -> screeningId))

    val getScreening: ScreeningId => Either[(Int, String), ScreeningInfo] = id => {
      MockDataBase.screenings.get(id) match {
        case None       => Left((404, "screening with given id does not exist"))
        case Some(info) => Right(info)
      }
    }

    val result = for {
      info <- getScreening(screeningId)
      moreInfo <- screeningInfo(screeningId, info, getAvailibleSeats(_, _))
    } yield moreInfo

    result match {
      case Left((errorCode, errorMsg)) => Status(errorCode)(errorMsg)
      case Right(info) => Ok(info)
    }
  }

  def postScreening(id: String) = Action(parse.json) { implicit request =>
    Ok(Json.obj("received" -> Json.toJson(request.body)))
  }

  def getAvailibleSeats(
    screeningId: ScreeningId,
    room: RoomId
  ): Either[(Int, String), List[Seat]] = {

    val roomData = MockDataBase.rooms.get(room)
    roomData match {
      case None => Left((500, "Inconsistent data"))

      case Some((numRows, numColumns)) => {
        
        val allRows = (1 to numRows).toList
        val allColumns = (1 to numColumns).toList
        val allSeats = allRows.flatMap(n => allColumns.map((n, _)))

        val reservations = MockDataBase.reservations.filter(_.screening == screeningId)
        val reservedSeats = reservations.flatMap(_.seats)
        val availibleSeats = allSeats.filterNot(reservations.contains)

        Right(availibleSeats)
      }
    }
  }
}

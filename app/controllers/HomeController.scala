package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._

import scala.util.{Failure, Success, Try}

import com.github.nscala_time.time.Imports._

import lib.DataDefs._
import lib.MockDataBase
import lib.Lib._


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



  // ----------------------------------------------------------------------------------------------
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

  // ----------------------------------------------------------------------------------------------
  def getScreening(screeningId: String) = Action { _ =>
    Ok(Json.obj("key" -> "val", "val" -> "key", "id" -> screeningId))

    val result = for {
      info <- MockDataBase.screenings.get(screeningId).toRight(
        (404, "screening with given id does not exist"))
      moreInfo <- screeningInfo(screeningId, info, getAvailableSeats(_, _).map(_.seats))
    } yield moreInfo

    result match {
      case Left((errorCode, errorMsg)) => Status(errorCode)(errorMsg)
      case Right(info) => Ok(info)
    }
  }

  // ----------------------------------------------------------------------------------------------
  def postScreening(screeningId: String) = Action(parse.json) { implicit request =>

    
    val result = for {
      reservation <- processReservationData(screeningId, request.body).toRight((400, "invalid request body"))

      info <- MockDataBase.screenings.get(screeningId).toRight(
        (404, "screening with given id does not exist"))
      takenSeats <- getTakenSeats(screeningId, info.room)
      requestedSeats = reservation.seats.keys.toList
      
      _ <- Either.cond(
        takenSeats.seats.intersect(requestedSeats).isEmpty,
        (),
        (400, "seats alredy taken")
      )

      takenSeatsAfterReservation = seatListToArray(takenSeats.dim, takenSeats.seats ++ requestedSeats)
      _ <- Either.cond(
        seatsPacked(takenSeatsAfterReservation),
        (),
        (400, "seats left untaken between two reserved seats")
      )

      price = reservation.seats.values.map(ticketPrice(_)).sum
      
    } yield price

    result match {
      case Left((errorCode, errorMsg))  => Status(errorCode)(errorMsg)
      case Right(price)                 => Ok(Json.obj("price" -> price))
    }
    
    
  }

  // ----------------------------------------------------------------------------------------------
  def getAvailableSeats(
    screeningId: ScreeningId,
    room: RoomId
  ): Either[Error, AvailableSeats] = for {

    takenSeats <- getTakenSeats(screeningId, room)
    availableSeats = getAvailableSeatsList(takenSeats.dim, takenSeats.seats)
    
  } yield AvailableSeats(takenSeats.dim, availableSeats)

  def getTakenSeats(
    screeningId: ScreeningId,
    room: RoomId,
  ): Either[Error, TakenSeats] = for {

    dim <- MockDataBase.rooms.get(room).toRight((500, "Inconsistent data"))

    reservations = MockDataBase.reservations.filter(_.screening == screeningId)
    reservedSeats = reservations.flatMap(_.seats.keys)

  } yield TakenSeats(dim, reservedSeats)
  
}

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
      case Failure(error) => errorResponse(InvalidParameters)
      
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
      info <- getScreeningInfo(screeningId)
      
      moreInfo <- screeningInfo(screeningId, info, getRoomDimension, getReservations)
    } yield moreInfo

    result match {
      case Left(error)  => errorResponse(error)
      case Right(info)  => Ok(info)
    }
  }

  // ----------------------------------------------------------------------------------------------
  def postScreening(screeningId: String) = Action(parse.json) { implicit request =>

    
    val result = for {
      reservation <- processReservationData(screeningId, request.body).toRight(InvalidBody)

      info <- getScreeningInfo(screeningId)
      takenSeats <- getTakenSeats(screeningId, info.room, getRoomDimension, getReservations)
      requestedSeats = reservation.seats.keys.toList
      
      _ <- Either.cond(
        takenSeats.seats.intersect(requestedSeats).isEmpty,
        (),
        SeatsAlredyTaken
      )

      takenSeatsAfterReservation = seatListToArray(takenSeats.dim, takenSeats.seats ++ requestedSeats)
      _ <- Either.cond(
        seatsConnected(takenSeatsAfterReservation),
        (),
        SeatsNotConnected
      )

      price = reservation.seats.values.map(ticketPrice(_)).sum
      
    } yield price

    result match {
      case Left(error)  => errorResponse(error)
      case Right(price) => Ok(Json.obj("price" -> price))
    }
    
    
  }

  // ----------------------------------------------------------------------------------------------
  def getReservations(screeningId: ScreeningId): List[Reservation]
    = MockDataBase.reservations.filter(_.screening == screeningId)
  
  def getRoomDimension(roomId: RoomId): Either[Error, RoomDimension]
    = MockDataBase.rooms.get(roomId).toRight(InconsistentData)
  
  def getScreeningInfo(screeningId: ScreeningId): Either[Error, ScreeningInfo]
    = MockDataBase.screenings.get(screeningId).toRight(NoSuchScreening)
}

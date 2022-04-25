package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._

import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import cats.implicits._
import cats.data.EitherT
import java.math.BigInteger

import com.github.nscala_time.time.Imports._

import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.bson._
import reactivemongo.api.collections.GenericQueryBuilder
import reactivemongo.api.Cursor

import lib.DataDefs._
import lib.MockDataBase
import lib.Lib._


/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(
  val controllerComponents: ControllerComponents,
  api: ReactiveMongoApi
) extends BaseController {

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
  def getScreening(screeningId: String) = Action.async { _ => {

      val futureResult = for {
        info      <- getScreeningInfo(screeningId)
        moreInfo  <- screeningInfo(screeningId, info, getRoomDimension, getReservations)
      } yield moreInfo

      for {
        result <- futureResult.value
      } yield result match {
        case Left(error)  => errorResponse(error)
        case Right(info)  => Ok(info)
      }
    }
  }

  // ----------------------------------------------------------------------------------------------
  def postScreening(screeningId: String) = Action.async(parse.json) {
    implicit request => for {

      result <- serveReservationRequest(
        screeningId,
        request.body,
        getScreeningInfo,
        getRoomDimension,
        getReservations,
      ).value

    } yield result match {
      case Left(error) => errorResponse(error)
      case Right(price) => Ok(Json.obj("price" -> price))
    }
  }



  // ----------------------------------------------------------------------------------------------
  def getReservations(screeningId: ScreeningId): Future[List[Reservation]]
    = Future { MockDataBase.reservations.filter(_.screening == screeningId) }
  
  def getRoomDimension(roomId: RoomId): EitherT[Future, Error, RoomDimension]
    = EitherT (Future { MockDataBase.rooms.get(roomId).toRight(InconsistentData) })
  
  def getScreeningInfo(screeningId: ScreeningId): EitherT[Future, Error, ScreeningInfo]
    = EitherT (Future { MockDataBase.screenings.get(screeningId).toRight(NoSuchScreening) })
}

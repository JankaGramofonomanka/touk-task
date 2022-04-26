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
  def screenings(date: String, from: String, to: String) = Action.async {
    _ => {
      val dateFrom  = s"$date $from"
      val dateTo    = s"$date $to"

      val tryFrom = Try(DateTimeFormat.forPattern("dd-MM-yyyy HH:mm").parseDateTime(dateFrom))
      val tryTo   = Try(DateTimeFormat.forPattern("dd-MM-yyyy HH:mm").parseDateTime(dateTo))


      val futureResult = for {
        from  <- EitherT.fromOption[Future](tryFrom .toOption, InvalidParameters)
        to    <- EitherT.fromOption[Future](tryTo   .toOption, InvalidParameters)

        screenings <- getScreenings(from, to)
        screeningInfos = screenings.map(t => screeningInfoBasic(t._1, t._2))
      } yield Json.toJson(screeningInfos)

      for {
        result <- futureResult.value
      } yield result match {
        case Left(error)  => errorResponse(error)
        case Right(infos)  => Ok(infos)
      }
    }
  }

  // ----------------------------------------------------------------------------------------------
  def getScreening(screeningIdStr: String) = Action.async { _ => {

      val futureResult = for {
        screeningId <- EitherT.fromEither[Future](stringToObjectID(screeningIdStr, InvalidScreeningId))
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
  def postScreening(screeningIdStr: String) = Action.async(parse.json) {
    implicit request => for {

      result <- (for {
        screeningId <- EitherT.fromEither[Future](stringToObjectID(screeningIdStr, InvalidScreeningId))
        price <- serveReservationRequest(
          screeningId,
          request.body,
          getScreeningInfo,
          getRoomDimension,
          getReservations,
        )
      } yield price).value


    } yield result match {
      case Left(error) => errorResponse(error)
      case Right(price) => Ok(Json.obj("price" -> price))
    }
  }


  // ----------------------------------------------------------------------------------------------
  def getReservations(screeningId: ScreeningId): EitherT[Future, Error, List[Reservation]]
    = {
      val futureQueryResult: Future[List[BSONDocument]] = for {
        db <- api.database
        queryDoc = BSONDocument("screening-id" -> screeningId)
        reservations <- db.collection[JSONCollection]("reservations")
          .find(queryDoc)
          .cursor[BSONDocument]()
          .collect[List](1000, Cursor.FailOnError[List[BSONDocument]]())
      } yield reservations

      for {
        docs <- EitherT.right[Error](futureQueryResult)
        reservations <- EitherT.fromOption[Future](
          docs.traverse(processReservationBSON(_)),
          InconsistentData: Error,
        )
      } yield reservations
  }
  
  def getRoomDimension(roomId: RoomId): EitherT[Future, Error, RoomDimension]
    = for {
      db <- EitherT.right[Error](api.database)
      queryDoc = BSONDocument ("room-id" -> roomId)
      doc <- EitherT.fromOptionF[Future, Error, BSONDocument](
        db.collection("rooms").find(queryDoc).one[BSONDocument],
        InconsistentData: Error
      )

      dim <- EitherT.fromOption[Future](processRoomBSON(doc), InconsistentData: Error)
    } yield dim
  
  def getScreeningInfo(screeningId: ScreeningId): EitherT[Future, Error, ScreeningInfo]
    = for {
      db <- EitherT.right[Error](api.database)

      queryDoc = BSONDocument ("_id" -> screeningId)
      doc <- EitherT.fromOptionF[Future, Error, BSONDocument](
        db.collection[JSONCollection]("screenings").find(queryDoc).one[BSONDocument],
        NoSuchScreening
      )

      info <- EitherT.fromOption[Future](processScreeningBSON(doc), InconsistentData: Error)
    } yield info._2

  def getScreenings(from: DateTime, to: DateTime): EitherT[Future, Error, List[(ScreeningId, ScreeningInfo)]]
    = {
      val futureScreenings: Future[List[BSONDocument]] = for {
        db <- api.database

        queryDoc = BSONDocument(
          "start" -> BSONDocument("$gte" -> BSONDateTime(from.getMillis)),
          "end"   -> BSONDocument("$lte" -> BSONDateTime(to.getMillis)),
        )


        screenings <- db.collection[JSONCollection]("screenings")
          .find(queryDoc)
          .cursor[BSONDocument]()
          .collect[List](100, Cursor.FailOnError[List[BSONDocument]]())

      } yield screenings

      for {
        screenings <- EitherT.right[Error](futureScreenings)
        results <- EitherT.fromOption[Future](
          screenings.traverse(processScreeningBSON(_)),
          InconsistentData: Error,
        )
      } yield results
    }
}

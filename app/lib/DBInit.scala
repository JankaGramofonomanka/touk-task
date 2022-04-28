package lib

import scala.util.Try
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import cats.data.EitherT
import play.api.libs.json._
import reactivemongo.bson._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.{DefaultWriteResult, LastError, UpdateWriteResult}
import reactivemongo.api.collections.bson.BSONCollection
import com.github.nscala_time.time.Imports._

import lib.Data._

final case class InitialData(rooms: List[BSONDocument], screenings: List[BSONDocument])
class DBInit(api: ReactiveMongoApi) {

  def initDB(data: JsValue): EitherT[Future, Error, Unit] = for {
    initData <- EitherT.fromOption[Future](processData(data), InvalidBody: Error)
    _ <- insertData(initData)
  } yield ()

  def insertData(data: InitialData): EitherT[Future, Error, Unit] = EitherT(
    for {
      db <- api.database
      resRooms <- data.rooms.traverse(
        db.collection[BSONCollection]("rooms").insert(_))

      resScreenings <- data.screenings.traverse(
        db.collection[BSONCollection]("screenings").insert(_))


    } yield (resRooms, resScreenings) match {
      case (_: LastError, _: LastError) => Left(Unknown)
      case _                            => Right(())
    }
  )

  // Process data ---------------------------------------------------------------------------------
  def processData(data: JsValue): Option[InitialData] = data match {
    case JsObject(obj) => for {
      jsonDate        <- obj.get("date")
      jsonRooms       <- obj.get("rooms")
      jsonScreenings  <- obj.get("screenings")

      dateStr <- jsonDate.asOpt[String]
      date <- Try(DateTimeFormat.forPattern("dd-MM-yyyy").parseDateTime(dateStr)).toOption

      rooms       <- processRooms(jsonRooms)
      screenings  <- processScreenings(jsonScreenings, date)
    } yield InitialData(rooms, screenings)

    case _ => None
  }


  def processRooms(data: JsValue): Option[List[BSONDocument]] = data match {
    case JsArray(arr) => arr.toList.traverse(processRoom(_))
    case _            => None
  }

  def processRoom(data: JsValue): Option[BSONDocument] = data match {
    case JsObject(obj) => for {
      jsonRoomId    <- obj.get("room-id")
      jsonNumRows   <- obj.get("rows")
      jsonNumSeats  <- obj.get("seats-per-row")

      roomId    <- jsonRoomId   .asOpt[String]
      numRows   <- jsonNumRows  .asOpt[Int]
      numSeats  <- jsonNumSeats .asOpt[Int]
    } yield BSONDocument("room-id" -> roomId, "rows" -> numRows, "seats-per-row" -> numSeats)

    case _ => None
  }
  def processScreenings(data: JsValue, date: DateTime): Option[List[BSONDocument]] = data match {
    case JsArray(arr) => arr.toList.traverse(processScreening(_, date))
    case _            => None
  }

  def processScreening(data: JsValue, date: DateTime): Option[BSONDocument] = data match {
    case JsObject(obj) => for {
      jsonTitle     <- obj.get("title")
      jsonStart     <- obj.get("start")
      jsonDuration  <- obj.get("duration")
      jsonRoomId    <- obj.get("room-id")

      title     <- jsonTitle.asOpt[String]
      startStr  <- jsonStart.asOpt[String]
      duration  <- jsonDuration.asOpt[Int]
      roomId    <- jsonRoomId.asOpt[String]

      hour <- processHour(startStr)
      start = date + hour
      end   = start + duration.minutes

    } yield BSONDocument(
      "title"   -> title,
      "start"   -> BSONDateTime(start .getMillis),
      "end"     -> BSONDateTime(end   .getMillis),
      "room-id" -> roomId,
    )

    case _ => None
  }

  def processHour(s: String): Option[Duration] = {
    val arr = s.split(":")
    for {
      hourStr   <- arr.lift(0)
      minuteStr <- arr.lift(1)
      hour    <- Try(hourStr  .toInt).toOption
      minute  <- Try(minuteStr.toInt).toOption
    } yield hour.hours + minute.minutes

  }


}
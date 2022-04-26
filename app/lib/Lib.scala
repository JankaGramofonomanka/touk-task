package lib

import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json._

import scala.util.{Failure, Success, Try}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import cats.implicits._
import cats.data.EitherT
import java.math.BigInteger

import com.github.nscala_time.time.Imports._
import reactivemongo.bson._


import lib.DataDefs._

object Lib {


  def ticketPrice(ticket: TicketType): Double = ticket match {
    case Adult    => 25
    case Student  => 18
    case Child    => 12.5
  }

  def parseTicketType(str: String): Option[TicketType] = str match {
    case "adult"    => Some(Adult)
    case "student"  => Some(Student)
    case "child"    => Some(Child)
    case _          => None
  }


  def errorResponse(error: Error): Result = error match {
    case InvalidParameters  => Status(400)("invalid parameters")
    case SeatsAlredyTaken   => Status(400)("seats alredy taken")
    case SeatsNotConnected  => Status(400)("seats left untaken between two reserved seats")
    case InconsistentData   => Status(500)("inconsistent data")
    case NoSuchScreening    => Status(404)("screening with given id does not exist")
    case InvalidBody        => Status(400)("invalid request body")
    case InvalidScreeningId => Status(400)("Invalid screeningId")
  }



  // Json -----------------------------------------------------------------------------------------
  def stringToObjectID(str: String, ifFailure: Error): Either[Error, BSONObjectID] = {
    val result = Try({
      val bigInt = new BigInteger(str, 16)
      val byteArr = bigInt.toByteArray
      BSONObjectID(byteArr)
    })

    result match {
      case Success(id)  => Right(id)
      case Failure(_)   => Left(ifFailure)
    }
  }

  def objectIdToString(id: BSONObjectID): String = {
    val byteArr = id.valueAsArray
    val bigInt = new BigInteger(byteArr)
    bigInt.toString(16)
  }


  def screeningInfoBasic(screeningId: ScreeningId, info: ScreeningInfo): JsObject = {
    val startHour   = info.start.toLocalTime.getHourOfDay
    val startMinute = info.start.toLocalTime.getMinuteOfHour

    Json.obj(
      "screeningId" -> objectIdToString(screeningId),
      "title"       -> info.title,
      "start-time"  -> f"$startHour%02d:$startMinute%02d",
      "duration"    -> info.duration.toStandardMinutes.getMinutes,
    )
  }

  def screeningInfo(
    screeningId: ScreeningId,
    info: ScreeningInfo,
    getRoomDimension: RoomId => EitherT[Future, Error, RoomDimension],
    getReservations: ScreeningId => Future[List[Reservation]],
  ): EitherT[Future, Error, JsObject] = for {


    availableSeats <- getAvailableSeats(
      screeningId,
      info.room,
      getRoomDimension,
      getReservations
    )

    basicInfo = screeningInfoBasic(screeningId, info)

    date = DateTimeFormat.forPattern("dd-MM-yyyy").print(info.start)

    result = (
      basicInfo
      + ("date" -> Json.toJson(date))
      + ("room" -> Json.toJson(info.room))
      + ("rows" -> Json.toJson(availableSeats.dim.numRows))
      + ("seats-per-row" -> Json.toJson(availableSeats.dim.numColumns))
      + ("availible-seats" -> Json.toJson(availableSeats.seats))
    )

  } yield result


  def processReservationData(
    screeningId: ScreeningId,
    body: JsValue,
  //): EitherT[Future, Error, Reservation] = ???

  ): Option[Reservation] = body match {

    case JsObject(obj) => for {
      jsonName    <- obj.get("name")
      jsonSurname <- obj.get("surname")
      jsonSeats   <- obj.get("seats")

      name    <- jsonName   .asOpt[String]
      surname <- jsonSurname.asOpt[String]

      reserver = Person(name, surname)

      seatReservationList: List[(Seat, TicketType)] <- jsonSeats match {
        case JsArray(array) => array.toList.map(processSeatReservation(_)).sequence
        case _ => None
      }

      seatReservationMap: Map[Seat, TicketType] = seatReservationList.toMap

    } yield Reservation(screeningId, seatReservationMap, reserver)

    case _ => None
  }


  def processSeatReservation(jsonSeat: JsValue): Option[(Seat, TicketType)] = jsonSeat match {
    case JsObject(elem) => for {

      jsonSeat    <- elem.get("seat")
      jsonTicket  <- elem.get("ticket")

      seat        <- jsonSeat.asOpt[Seat]
      ticketType  <- jsonTicket.asOpt[String].flatMap(parseTicketType(_))

    } yield (seat -> ticketType)

    case _ => None
  }

  def validateReservation(
    screeningId: ScreeningId,
    reservation: Reservation,

    getScreeningInfo: ScreeningId => EitherT[Future, Error, ScreeningInfo],
    getRoomDimension: RoomId => EitherT[Future, Error, RoomDimension],
    getReservations:  ScreeningId => Future[List[Reservation]],

  ): EitherT[Future, Error, Unit] = for {

    info <- getScreeningInfo(screeningId)
    takenSeats <- getTakenSeats(screeningId, info.room, getRoomDimension, getReservations)

    requestedSeats = reservation.seats.keys.toList

    _ <- EitherT.cond[Future](
      takenSeats.seats.intersect(requestedSeats).isEmpty,
      (),
      SeatsAlredyTaken: Error
    )

    takenSeatsAfterReservation = seatListToArray(takenSeats.dim, takenSeats.seats ++ requestedSeats)
    _ <- EitherT.cond[Future](
      seatsConnected(takenSeatsAfterReservation),
      (),
      SeatsNotConnected: Error
    )

    //price = reservation.seats.values.map(ticketPrice(_)).sum
  } yield ()

  def calculatePrice(reservation: Reservation): Double = reservation.seats.values.map(ticketPrice(_)).sum

  def serveReservationRequest(
    screeningId:  ScreeningId,
    body:         JsValue,

    getScreeningInfo: ScreeningId => EitherT[Future, Error, ScreeningInfo],
    getRoomDimension: RoomId => EitherT[Future, Error, RoomDimension],
    getReservations:  ScreeningId => Future[List[Reservation]],

  ): EitherT[Future, Error, Double] = {
    for {

      reservation <- EitherT.fromOption[Future](
        processReservationData(screeningId, body),
        InvalidBody: Error
      )


      _ <- validateReservation(
        screeningId,
        reservation,
        getScreeningInfo,
        getRoomDimension,
        getReservations,
      )

      price = reservation.seats.values.map(ticketPrice(_)).sum

    } yield price
  }

  // Bson -----------------------------------------------------------------------------------------
  def bsonToString(bson: BSONValue): Option[String] = bson match {
    case BSONString(value)  => Some(value)
    case _                  => None
  }

  def bsonToDateTime(bson: BSONValue): Option[DateTime] = bson match {
    case BSONDateTime(value)  => Some(new DateTime(value))
    case _                    => None
  }

  def bsonToObjectId(bson: BSONValue): Option[BSONObjectID] = bson match {
    case BSONObjectID(value)  => Some(BSONObjectID(value))
    case _                    => None
  }

  def processScreeningBSON(doc: BSONDocument): Option[(ScreeningId, ScreeningInfo)] = for {

    bsonId    <- doc.get("_id")
    bsonTitle <- doc.get("title")
    bsonStart <- doc.get("start")
    bsonEnd   <- doc.get("end")
    bsonRoom  <- doc.get("room-id")

    id    <- bsonToObjectId (bsonId)
    title <- bsonToString   (bsonTitle)
    start <- bsonToDateTime (bsonStart)
    end   <- bsonToDateTime (bsonEnd)
    room  <- bsonToString   (bsonRoom)

    duration = new Duration(start, end)

    info = ScreeningInfo(title, start, duration, room)

  } yield (id, info)


  // Seats ----------------------------------------------------------------------------------------
  def getAvailableSeats(
    screeningId: ScreeningId,
    roomId: RoomId,
    getRoomDimension: RoomId => EitherT[Future, Error, RoomDimension],
    getReservations: ScreeningId => Future[List[Reservation]],
  ): EitherT[Future, Error, AvailableSeats] = for {

    takenSeats <- getTakenSeats(
      screeningId,
      roomId,
      getRoomDimension,
      getReservations
    )
    availableSeats = getAvailableSeatsArray(takenSeats.dim, takenSeats.seats)
    
  } yield AvailableSeats(takenSeats.dim, availableSeats)

  def getTakenSeats(
    screeningId: ScreeningId,
    roomId: RoomId,
    getRoomDimension: RoomId => EitherT[Future, Error, RoomDimension],
    getReservations: ScreeningId => Future[List[Reservation]],
  ): EitherT[Future, Error, TakenSeats] = for {

    dim <- getRoomDimension(roomId)
    reservations <- EitherT.right(getReservations(screeningId))
    reservedSeats = reservations.flatMap(_.seats.keys)

  } yield TakenSeats(dim, reservedSeats)
  
  def getAllSeatsArray(dim: RoomDimension): List[List[ColumnId]] = {
    (1 to dim.numRows).toList.map(_ => (1 to dim.numColumns).toList)
  }

  def getAllSeatsList(dim: RoomDimension): List[Seat] = {
    val allRows = (1 to dim.numRows).toList
    val allColumns = (1 to dim.numColumns).toList
    val allSeats = allRows.flatMap(n => allColumns.map((n, _)))

    allSeats
  }

  def getAvailableSeatsList(
    dim: RoomDimension,
    reservedSeats: List[Seat],
  ): List[Seat] = {

    val allSeats = getAllSeatsList(dim)
    val availibleSeats = allSeats.filterNot(reservedSeats.contains)

    availibleSeats
  }

  def getAvailableSeatsArray(
    dim: RoomDimension,
    reservedSeats: List[Seat],
  ): List[List[ColumnId]] = {
    
    val allSeats = getAllSeatsArray(dim)
    val takenSeats = getTakenSeatsArray(dim, reservedSeats)

    val availableSeats = allSeats.zip(takenSeats).map(t => t._1.filterNot(t._2.contains))
    availableSeats
  }

  def getTakenSeatsArray(
    dim: RoomDimension,
    reservedSeats: List[Seat],
  ): List[List[ColumnId]] = seatListToArray(dim, reservedSeats)

  def seatListToArray(
    dim: RoomDimension,
    seatList: List[Seat],
  ): List[List[ColumnId]] = {
    val rowNums = (1 to dim.numRows).toList
    val seatsDividedByRow = rowNums.map(r => seatList.filter(_._1 == r))
    val seatArray = seatsDividedByRow.map(row => row.map(seat => seat._2).sorted)
    seatArray
  }

  def seatsConnected(seats: List[List[ColumnId]]): Boolean = {
    val rowConnected: List[ColumnId] => Boolean = row => {
      val rowSorted = row.sorted
      rowSorted match {
        case Nil => true
        case (x :: xs) => rowSorted.last == x + rowSorted.length - 1
      }
    }

    seats.forall(rowConnected)
  }


}
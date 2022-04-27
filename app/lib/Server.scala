package lib


import scala.util.Try
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import cats.data.EitherT

import play.api.libs.json._
import com.github.nscala_time.time.Imports._

import lib.Data._
import lib.Seats._
import lib.DBInterface

class Server(dbInterface: DBInterface) {


  // Response preparation -------------------------------------------------------------------------
  def screeningInfoBasic(info: ScreeningInfo): JsObject = {
    val startHour   = info.start.toLocalTime.getHourOfDay
    val startMinute = info.start.toLocalTime.getMinuteOfHour

    Json.obj(
      "screeningId" -> objectIdToString(info.id),
      "title"       -> info.title,
      "start-time"  -> f"$startHour%02d:$startMinute%02d",
      "duration"    -> info.duration.toStandardMinutes.getMinutes,
    )
  }

  def screeningInfo(info: ScreeningInfo): EitherT[Future, Error, JsObject] = for {

    availableSeats <- getAvailableSeats(info.id, info.room)

    basicInfo = screeningInfoBasic(info)

    date = DateTimeFormat.forPattern("dd-MM-yyyy").print(info.start)

    result = (
      basicInfo
      + ("date" -> Json.toJson(date))
      + ("room" -> Json.toJson(info.room))
      + ("rows" -> Json.toJson(availableSeats.dim.numRows))
      + ("seats-per-row" -> Json.toJson(availableSeats.dim.numColumns))
      + ("available-seats" -> Json.toJson(availableSeats.seats))
    )

  } yield result


  // Request body processing ----------------------------------------------------------------------
  def processReservationData(
    screeningId:  ScreeningId,
    body:         JsValue,
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

      jsonRow     <- elem.get("row")
      jsonSeat    <- elem.get("seat")
      jsonTicket  <- elem.get("ticket")

      row         <- jsonRow.asOpt[Int]
      column      <- jsonSeat.asOpt[Int]
      ticketType  <- jsonTicket.asOpt[String].flatMap(parseTicketType(_))

      seat = Seat(row, column)

    } yield (seat -> ticketType)

    case _ => None
  }


  // Processing of data from the database ---------------------------------------------------------
  def getAvailableSeats(
    screeningId:  ScreeningId,
    roomId:       RoomId,
  ): EitherT[Future, Error, AvailableSeats] = for {

    takenSeats <- getTakenSeats(
      screeningId,
      roomId,
    )
    availableSeats = getAvailableSeatsArray(takenSeats.dim, takenSeats.seats)

  } yield AvailableSeats(takenSeats.dim, availableSeats)

  def getTakenSeats(
    screeningId:  ScreeningId,
    roomId:       RoomId,
  ): EitherT[Future, Error, TakenSeats] = for {

    dim           <- dbInterface.getRoomDimension(roomId)
    reservations  <- dbInterface.getReservations(screeningId)
    reservedSeats = reservations.flatMap(_.seats.keys)

  } yield TakenSeats(dim, reservedSeats)


  // Request serving ------------------------------------------------------------------------------
  def serveListScreeningsRequest(
    date: String,
    from: String,
    to:   String,
  ): EitherT[Future, Error, JsValue] = {

    val dateFrom  = s"$date $from"
    val dateTo    = s"$date $to"

    val tryFrom = Try(DateTimeFormat.forPattern("dd-MM-yyyy HH:mm").parseDateTime(dateFrom))
    val tryTo   = Try(DateTimeFormat.forPattern("dd-MM-yyyy HH:mm").parseDateTime(dateTo))


    for {
      from  <- EitherT.fromOption[Future](tryFrom .toOption, InvalidParameters)
      to    <- EitherT.fromOption[Future](tryTo   .toOption, InvalidParameters)

      screenings <- dbInterface.getScreenings(from, to)
      screeningsSorted = screenings.sortBy(info => (info.title, info.duration))
      screeningInfos = screeningsSorted.map(screeningInfoBasic(_))
    } yield Json.toJson(screeningInfos)
  }

  def serveScreeningInfoRequst(idStr: String): EitherT[Future, Error, JsObject] = for {

    screeningId <- EitherT.fromEither[Future](stringToObjectID(idStr, InvalidScreeningId))
    info        <- dbInterface.getScreeningInfo(screeningId)
    moreInfo    <- screeningInfo(info)

  } yield moreInfo

  def validateReservation(
    reservation: Reservation,
  ): EitherT[Future, Error, DateTime] = for {

    info        <- dbInterface.getScreeningInfo(reservation.screening)
    takenSeats  <- getTakenSeats(reservation.screening, info.room)

    requestedSeats = reservation.seats.keys.toList

    // Not to late to reserve seats
    expirationDate = info.start - 15.minutes
    _ <- EitherT.cond[Future](
      DateTime.now() < expirationDate,
      (),
      TooLateForReservation: Error
    )

    // Seats are not already reserved
    _ <- EitherT.cond[Future](
      takenSeats.seats.intersect(requestedSeats).isEmpty,
      (),
      SeatsAlreadyTaken: Error
    )

    // No free seats between reserved seats
    takenSeatsAfterReservation = seatListToArray(
      takenSeats.dim,
      takenSeats.seats ++ requestedSeats
    )
    _ <- EitherT.cond[Future](
      seatsConnected(takenSeatsAfterReservation),
      (),
      SeatsNotConnected: Error
    )

  } yield expirationDate

  def calculatePrice(reservation: Reservation): Double
    = reservation.seats.values.map(ticketPrice(_)).sum

  def serveReservationRequest(
    screeningIdStr: String,
    body:           JsValue,
  ): EitherT[Future, Error, JsObject] = {
    for {

      screeningId <- EitherT.fromEither[Future](
        stringToObjectID(screeningIdStr, InvalidScreeningId))

      reservation <- EitherT.fromOption[Future](
        processReservationData(screeningId, body),
        InvalidBody: Error
      )

      expirationDate <- validateReservation(reservation)

      _ <- dbInterface.insertReservation(reservation)

      price = reservation.seats.values.map(ticketPrice(_)).sum
      expirationDateStr = DateTimeFormat.forPattern("dd-MM-yyyy HH:mm").print(expirationDate)

    } yield Json.obj("price" -> price, "expiration-date" -> expirationDateStr)
  }


}
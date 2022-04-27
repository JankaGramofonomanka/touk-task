package lib

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import cats.implicits._
import cats.data.EitherT


import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.bson._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.Cursor
import reactivemongo.api.commands._

import com.github.nscala_time.time.Imports._

import lib.Data._


class DBInterface(api: ReactiveMongoApi) {

  // Getting data from the database----------------------------------------------------------------
  def getReservations(screeningId: ScreeningId): EitherT[Future, Error, List[Reservation]] = {
    val futureQueryResult: Future[List[BSONDocument]] = for {
      db <- api.database
      queryDoc = BSONDocument("screening-id" -> screeningId)
      reservations <- db.collection[BSONCollection]("reservations")
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

  def getRoomDimension(roomId: RoomId): EitherT[Future, Error, RoomDimension] = for {
    db <- EitherT.right[Error](api.database)
    queryDoc = BSONDocument ("room-id" -> roomId)
    doc <- EitherT.fromOptionF[Future, Error, BSONDocument](
      db.collection("rooms").find(queryDoc).one[BSONDocument],
      InconsistentData: Error
    )

    dim <- EitherT.fromOption[Future](processRoomBSON(doc), InconsistentData: Error)
  } yield dim

  def getScreeningInfo(screeningId: ScreeningId): EitherT[Future, Error, ScreeningInfo] = for {
    db <- EitherT.right[Error](api.database)

    queryDoc = BSONDocument ("_id" -> screeningId)
    doc <- EitherT.fromOptionF[Future, Error, BSONDocument](
      db.collection[BSONCollection]("screenings").find(queryDoc).one[BSONDocument],
      NoSuchScreening
    )

    info <- EitherT.fromOption[Future](processScreeningBSON(doc), InconsistentData: Error)
  } yield info._2

  def getScreenings(
    from: DateTime,
    to:   DateTime,
  ): EitherT[Future, Error, List[(ScreeningId, ScreeningInfo)]] = {
    val futureScreenings: Future[List[BSONDocument]] = for {
      db <- api.database

      queryDoc = BSONDocument(
        "start" -> BSONDocument("$gte" -> BSONDateTime(from.getMillis)),
        "end"   -> BSONDocument("$lte" -> BSONDateTime(to.getMillis)),
      )


      screenings <- db.collection[BSONCollection]("screenings")
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

  def insertReservation(reservation: Reservation): EitherT[Future, Error, Unit] = {
    val futureResult: Future[Either[Error, Unit]] = for {
      db <- api.database
      reservationDoc = reservationToBSON(reservation)
      res <- db.collection[BSONCollection]("reservations").insert(reservationDoc)

    } yield res match {
      case _: LastError           => Left(Unknown)
      case _: DefaultWriteResult  => Right(())
      case _: UpdateWriteResult   => Right(())
    }

    EitherT(futureResult)
  }

  // BSON processing ------------------------------------------------------------------------------
  def bsonToString(bson: BSONValue): Option[String] = bson match {
    case BSONString(value)  => Some(value)
    case _                  => None
  }

  def bsonToInt(bson: BSONValue): Option[Int] = bson match {
    case BSONInteger(value) => Some(value)
    case _                  => None
  }

  def bsonToDateTime(bson: BSONValue): Option[DateTime] = bson match {
    case BSONDateTime(value)  => Some(new DateTime(value))
    case _                    => None
  }

  def bsonToObjectId(bson: BSONValue): Option[BSONObjectID] = bson match {
    case objId: BSONObjectID  => Some(objId)
    case _                    => None
  }

  def bsonToArray(bson: BSONValue): Option[List[BSONValue]] = bson match {
    case arr: BSONArray => Some(arr.elements.map(_.value))
    case _              => None
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

  def processRoomBSON(doc: BSONDocument): Option[RoomDimension] = for {
    bsonRows        <- doc.get("rows")
    bsonSeatsPerRow <- doc.get("seats-per-row")

    rows        <- bsonToInt(bsonRows)
    seatsPerRow <- bsonToInt(bsonSeatsPerRow)

  } yield RoomDimension(rows, seatsPerRow)

  def processReservationBSON(doc: BSONDocument): Option[Reservation] = for {
    bsonScreeningId <- doc.get("screening-id")
    bsonSeats       <- doc.get("seats")
    bsonName        <- doc.get("name")
    bsonSurname     <- doc.get("surname")

    screeningId   <- bsonToObjectId(bsonScreeningId)
    bsonSeatArray <- bsonToArray(bsonSeats)
    seats         <- bsonSeatArray.traverse(processSeatBSON(_))
    name          <- bsonToString(bsonName)
    surname       <- bsonToString(bsonSurname)

  } yield Reservation(screeningId, seats.toMap, Person(name, surname))

  def processSeatBSON(bson: BSONValue): Option[(Seat, TicketType)] = bson match {
    case doc: BSONDocument => for {
      bsonRow     <- doc.get("row")
      bsonSeat    <- doc.get("seat")
      bsonTicket  <- doc.get("ticket")

      rowNum    <- bsonToInt(bsonRow)
      seatNum   <- bsonToInt(bsonSeat)
      ticketStr <- bsonToString(bsonTicket)

      ticketType <- parseTicketType(ticketStr)
      seat = Seat(rowNum, seatNum)

    } yield (seat, ticketType)

    case _ => None
  }

  def reservationToBSON(reservation: Reservation): BSONDocument = {

    val seats = reservation.seats.map(seatReservationToBSON(_))
    BSONDocument(
      "screening-id"  -> reservation.screening,
      "seats"         -> seats,
      "name"          -> reservation.reserver.name,
      "surname"       -> reservation.reserver.surname,
    )
  }

  def seatReservationToBSON(seatReservation: (Seat, TicketType)): BSONDocument = {
    val seat: Seat          = seatReservation._1
    val ticket: TicketType  = seatReservation._2
    BSONDocument(
      "row" -> seat.row, "seat" -> seat.column, "ticket" -> ticketTypeToString(ticket)
    )
  }


}
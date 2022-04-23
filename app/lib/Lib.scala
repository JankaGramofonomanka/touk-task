package lib

import play.api.libs.json._
import cats.implicits._

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

  // Json -----------------------------------------------------------------------------------------
  def screeningInfoBasic(screeningId: ScreeningId, info: ScreeningInfo): JsObject = {
    val startHour = info.start.toLocalTime.getHourOfDay
    val startMinute = info.start.toLocalTime.getMinuteOfHour

    Json.obj(
      "screeningId" -> screeningId,
      "title"       -> info.title,
      "start-time"  -> f"$startHour%02d:$startMinute%02d",
      "duration"    -> info.duration.toStandardMinutes.getMinutes,
    )
  }

  def screeningInfo(
    screeningId: ScreeningId,
    info: ScreeningInfo,
    getRoomDimension: RoomId => Either[Error, RoomDimension],
    getReservations: ScreeningId => List[Reservation],
  ): Either[Error, JsObject] = for {

    
    availableSeats <- getAvailableSeats(
      screeningId,
      info.room,
      getRoomDimension,
      getReservations
    )

    basicInfo = screeningInfoBasic(screeningId, info)

    result = (
      basicInfo
      + ("room" -> Json.toJson(info.room))
      + ("availible-seats" -> Json.toJson(availableSeats.seats))
    )
    
  } yield result


  def processReservationData(
    screeningId: ScreeningId, 
    body: JsValue,
  ): Option[Reservation] = body match {

    case JsObject(obj) => for {
      jsonName    <- obj.get("name")
      jsonSurname <- obj.get("surname")
      jsonSeats   <- obj.get("seats")

      name    <- jsonName   .asOpt[String]
      surname <- jsonSurname.asOpt[String]

      reserver = Person(name, surname)

      seatReservationList: List[(Seat, TicketType)] <- jsonSeats match {
        case JsArray(array) => array.toList.map(processSeatReservation(_)).traverse(x => x)
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


  // Seats ----------------------------------------------------------------------------------------
  def getAvailableSeats(
    screeningId: ScreeningId,
    roomId: RoomId,
    getRoomDimension: RoomId => Either[Error, RoomDimension],
    getReservations: ScreeningId => List[Reservation],
  ): Either[Error, AvailableSeats] = for {

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
    getRoomDimension: RoomId => Either[Error, RoomDimension],
    getReservations: ScreeningId => List[Reservation],

  ): Either[Error, TakenSeats] = for {

    dim <- getRoomDimension(roomId)
    reservations = getReservations(screeningId)
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

  def seatsPacked(seats: List[List[ColumnId]]): Boolean = {
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
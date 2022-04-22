package lib

import play.api.libs.json._

import lib.DataDefs._

object Lib {


  def ticketPrice(ticket: TicketType): Double = ticket match {
    case Adult    => 25
    case Student  => 18
    case Child    => 12.5
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
    getAvailibleSeats: (ScreeningId, RoomId) => Either[Error, List[Seat]],
  ): Either[Error, JsObject] = for {

    basicInfo <- Right(screeningInfoBasic(screeningId, info))
    seats <- getAvailibleSeats(screeningId, info.room).map(Json.toJson(_))
    
  } yield basicInfo + ("room" -> Json.toJson(info.room)) + ("availible-seats" -> Json.toJson(seats))

  def processReservationData(body: JsValue): Option[Reservation] = ???


  // Seats ----------------------------------------------------------------------------------------
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
  ): List[List[ColumnId]] = seatListToArray(dim, reservedSeats)

  def getTakenSeatsArray(
    dim: RoomDimension,
    reservedSeats: List[Seat],
  ): List[List[ColumnId]] = {
    
    val allSeats = getAllSeatsArray(dim)
    val availibleSeats = getAvailableSeatsArray(dim, reservedSeats)

    val takenSeats = allSeats.zip(availibleSeats).map(t => t._1.filterNot(t._2.contains))
    takenSeats
  }

  def seatListToArray(
    dim: RoomDimension,
    seatList: List[Seat],
  ): List[List[ColumnId]] = {
    val rowNums = (1 to dim.numRows).toList
    val seatsDividedByRow = rowNums.map(r => seatList.filter(_._1 == r))
    val seatArray = seatsDividedByRow.map(row => row.map(seat => seat._2))
    seatArray
  }

  def seatsPacked(seats: List[List[ColumnId]]): Boolean = {
    val rowConnected: List[ColumnId] => Boolean = row => row.sorted match {
      case Nil => true
      case (x :: xs) => row.last == x + row.length - 1
    }

    seats.forall(rowConnected)
  }


}
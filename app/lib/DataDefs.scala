package lib

import play.api.libs.json._

import com.github.nscala_time.time.Imports._

object DataDefs {
  type ScreeningId  = String
  type RoomId       = Int
  type RowId        = Int
  type ColumnId     = Int
  type Seat         = (RowId, ColumnId)
  type Error        = (Int, String)

  sealed trait TicketType
  final object Adult extends TicketType
  final object Student extends TicketType
  final object Child extends TicketType

  final case class RoomDimmension(numRows: Int, numColumns: Int)

  final case class Person(name: String, surname: String)

  final case class ScreeningInfo(
    title:    String,
    start:    DateTime,
    duration: Duration,
    room:     RoomId,
  )

  final case class Reservation(
    screening:  ScreeningId,
    seats:      Map[Seat, TicketType],
    reserver:   Person,
  )

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
}

package lib

import play.api.libs.json._

import com.github.nscala_time.time.Imports._

object DataDefs {
  type ScreeningId  = String
  type RoomId       = Int
  type RowId        = Int
  type ColumnId     = Int
  type Seat         = (RowId, ColumnId)

  final case class Person(name: String, surname: String)

  final case class ScreeningInfo(
    title:    String,
    start:    DateTime,
    duration: Duration,
    room:     RoomId,
  )

  final case class Reservation(
    screening:  ScreeningId,
    seats:      List[Seat],
    reserver:   Person,
  )

  def screeningInfoBasic(screeningId: ScreeningId, info: ScreeningInfo): JsValue = {
    val startHour = info.start.toLocalTime.getHourOfDay
    val startMinute = info.start.toLocalTime.getMinuteOfHour

    Json.obj(
      "screeningId" -> screeningId,
      "title"       -> info.title,
      "start-time"  -> f"$startHour%02d:$startMinute%02d",
      "duration"    -> info.duration.toStandardMinutes.getMinutes,
    )
  }
}

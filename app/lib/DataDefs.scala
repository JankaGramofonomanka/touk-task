package lib

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

  final case class RoomDimension(numRows: Int, numColumns: Int)
  
  final case class AvailableSeats(dim: RoomDimension, seats: List[List[ColumnId]])
  final case class TakenSeats(dim: RoomDimension, seats: List[Seat])

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
}

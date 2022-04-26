package lib

import com.github.nscala_time.time.Imports._
import reactivemongo.bson.BSONObjectID

object DataDefs {
  type ScreeningId  = BSONObjectID
  type RoomId       = String
  type RowId        = Int
  type ColumnId     = Int
  type Seat         = (RowId, ColumnId)


  sealed trait Error
  final object InvalidParameters  extends Error
  final object SeatsAlredyTaken   extends Error
  final object SeatsNotConnected  extends Error
  final object InconsistentData   extends Error
  final object NoSuchScreening    extends Error
  final object InvalidBody        extends Error
  final object InvalidScreeningId extends Error


  sealed trait TicketType
  final object Adult    extends TicketType
  final object Student  extends TicketType
  final object Child    extends TicketType

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

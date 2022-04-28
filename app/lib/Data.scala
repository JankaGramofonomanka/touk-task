package lib

import scala.util.{Failure, Success, Try}
import java.math.BigInteger

import play.api.mvc._
import play.api.libs.json._
import play.api.mvc.Results._
import com.github.nscala_time.time.Imports._
import reactivemongo.bson.BSONObjectID



object Data {

  // Data type definitions ------------------------------------------------------------------------
  type ScreeningId  = BSONObjectID
  type RoomId       = String
  type RowId        = Int
  type ColumnId     = Int


  case class Seat(row: RowId, column: ColumnId)


  sealed trait Error
  final object InvalidParameters      extends Error
  final object SeatsAlreadyTaken      extends Error
  final object SeatsNotConnected      extends Error
  final object InconsistentData       extends Error
  final object NoSuchScreening        extends Error
  final object InvalidBody            extends Error
  final object InvalidScreeningId     extends Error
  final object Unknown                extends Error
  final object TooLateForReservation  extends Error
  final object InvalidName            extends Error
  final object InvalidSurname         extends Error
  final object SeatOutOfRange         extends Error
  final object EmptyReservation       extends Error


  sealed trait TicketType
  final object Adult    extends TicketType
  final object Student  extends TicketType
  final object Child    extends TicketType

  final case class RoomDimension(numRows: Int, numColumns: Int)
  
  final case class AvailableSeats(dim: RoomDimension, seats: List[List[ColumnId]])
  final case class TakenSeats(dim: RoomDimension, seats: List[Seat])

  final case class Person(name: String, surname: String)

  final case class ScreeningInfo(
    id:       ScreeningId,
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

  // Utils ----------------------------------------------------------------------------------------
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

  def ticketTypeToString(t: TicketType): String = t match {
    case Adult    => "adult"
    case Student  => "student"
    case Child    => "child"
  }


  def errorResponse(error: Error): Result = error match {
    case InvalidParameters      => Status(400)("invalid parameters")
    case SeatsAlreadyTaken      => Status(400)("seats alredy taken")
    case SeatsNotConnected      => Status(400)("seats left untaken between two reserved seats")
    case InconsistentData       => Status(500)("inconsistent data")
    case NoSuchScreening        => Status(404)("screening with given id does not exist")
    case InvalidBody            => Status(400)("invalid request body")
    case InvalidScreeningId     => Status(400)("invalid screeningId")
    case Unknown                => Status(500)("unknown error")
    case TooLateForReservation  => Status(400)("too late for reservation")
    case InvalidName            => Status(400)("invalid name")
    case InvalidSurname         => Status(400)("invalid surname")
    case SeatOutOfRange         => Status(400)("row or seat number out of range")
    case EmptyReservation       => Status(400)("empty reservation")
  }

  def responseFromEither(either: Either[Error, JsValue]): Result = either match {
    case Left(error) => errorResponse(error)
    case Right(json) => Ok(json)
  }

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
}

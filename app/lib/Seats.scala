package lib

import lib.Data._

object Seats {

  def getAllSeatsArray(dim: RoomDimension): List[List[ColumnId]] = {
    (1 to dim.numRows).toList.map(_ => (1 to dim.numColumns).toList)
  }

  def getAllSeatsList(dim: RoomDimension): List[Seat] = {
    val allRows = (1 to dim.numRows).toList
    val allColumns = (1 to dim.numColumns).toList
    val allSeats = allRows.flatMap(n => allColumns.map(Seat(n, _)))

    allSeats
  }

  def getAvailableSeatsList(
                             dim:            RoomDimension,
                             reservedSeats:  List[Seat],
                           ): List[Seat] = {

    val allSeats = getAllSeatsList(dim)
    val availibleSeats = allSeats.filterNot(reservedSeats.contains)

    availibleSeats
  }

  def getAvailableSeatsArray(
                              dim:            RoomDimension,
                              reservedSeats:  List[Seat],
                            ): List[List[ColumnId]] = {

    val allSeats = getAllSeatsArray(dim)
    val takenSeats = getTakenSeatsArray(dim, reservedSeats)

    val availableSeats = allSeats.zip(takenSeats).map(t => t._1.filterNot(t._2.contains))
    availableSeats
  }

  def getTakenSeatsArray(
                          dim:            RoomDimension,
                          reservedSeats:  List[Seat],
                        ): List[List[ColumnId]] = seatListToArray(dim, reservedSeats)

  def seatListToArray(
                       dim:      RoomDimension,
                       seatList: List[Seat],
                     ): List[List[ColumnId]] = {
    val rowNums = (1 to dim.numRows).toList
    val seatsDividedByRow = rowNums.map(r => seatList.filter(_.row == r))
    val seatArray = seatsDividedByRow.map(row => row.map(seat => seat.column).sorted)
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
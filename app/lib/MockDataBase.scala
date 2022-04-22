package lib

import com.github.nscala_time.time.Imports._

import lib.DataDefs._

object MockDataBase {

  

  //                      rows  columns
  val rooms: Map[RoomId, RoomDimmension] = Map(
      1 -> RoomDimmension(15, 20),
      2 -> RoomDimmension(10, 15),
      3 -> RoomDimmension(10, 10),
  )


  

  val screenings: Map[ScreeningId, ScreeningInfo] = Map(
    "fastandfurious24_1" -> ScreeningInfo(
        "Fast & Furious 24",
        new DateTime(2022, 4, 30, 15, 0),
        1.hours + 46.minutes,
        1
      ),

    "scarface_1" -> ScreeningInfo(
        "Scarface",
        new DateTime(2022, 4, 30, 17, 0),
        2.hours + 50.minutes,
        1
      ),

    "shrek_1" -> ScreeningInfo(
        "Shrek",
        new DateTime(2022, 4, 30, 14, 30),
        95.minutes,
        2
      ),
    
    "shrek_2" -> ScreeningInfo(
        "Shrek",
        new DateTime(2022, 4, 30, 16, 30),
        95.minutes,
        2
      ),

    "12angrymen" -> ScreeningInfo(
        "12 Angry People",
        new DateTime(2022, 4, 30, 16, 0),
        96.minutes,
        3
      ),

    "fastandfurious24_2" -> ScreeningInfo(
        "Fast & Furious 24",
        new DateTime(2022, 5, 1, 15, 0),
        1.hours + 46.minutes,
        1
      ),
    
    "scarface_2" -> ScreeningInfo(
        "Scarface",
        new DateTime(2022, 5, 1, 17, 0),
        2.hours + 50.minutes,
        1
      ),

    "shrek_3" -> ScreeningInfo(
        "Shrek",
        new DateTime(2022, 5, 1, 14, 30),
        95.minutes,
        2
      ),
    
    "shrek_4" -> ScreeningInfo(
        "Shrek",
        new DateTime(2022, 5, 1, 16, 30),
        95.minutes,
        2
      ),

    "pulpfiction" -> ScreeningInfo(
        "Pulp Fiction",
        new DateTime(2022, 5, 1, 17, 0),
        154.minutes,
        3
      ),
  )

  val reservations: List[Reservation] = List(
    Reservation(
      "scarface_1",
      Map((15, 10) -> Child, (15, 11) -> Adult, (15, 12) -> Child),
      Person("Don", "Corleone")
    ),
    
    Reservation(
      "scarface_1",
      Map((15, 6) -> Student, (15, 7) -> Adult, (15, 8) -> Adult, (15, 9) -> Child),
      Person("John", "Smith")
    ),
    
    Reservation(
      "scarface_2",
      Map((10, 8) -> Student, (10, 9) -> Student, (10, 10) -> Student),
      Person("Buck", "Rodgers")
    ),

    Reservation(
      "12angrymen",
      Map((8, 4) -> Adult),
      Person("John", "Galt")
    ),

    Reservation(
      "12angrymen",
      Map((8, 5) -> Adult),
      Person("Henry", "Rearden")
    ),

    Reservation(
      "shrek_1",
      Map((8, 5) -> Child, (8, 6) -> Child, (8, 7) -> Adult, (9, 6) -> Child, (9, 7) -> Child),
      Person("Michael", "Jackson")
    ),

    Reservation(
      "shrek_3",
      Map((7, 8) -> Child, (7, 9) -> Child, (7, 10) -> Adult),
      Person("Bugs", "Bunny")
    ),

    Reservation(
      "shrek_4",
      Map((7, 8) -> Child, (7, 9) -> Adult),
      Person("Mao", "Tse-Tung")
    ),

    Reservation(
      "shrek_2",
      Map((1, 4) -> Adult, (1, 5) -> Child, (1, 6) -> Adult),
      Person("Antonio", "Montana")
    ),

    Reservation(
      "pulpfiction",
      Map((1, 4) -> Adult, (1, 5) -> Student, (1, 6) -> Student),
      Person("Betty", "Boop")
    ),

    Reservation(
      "fastandfurious24_1",
      Map((1, 10) -> Adult),
      Person("Ray", "Charles")
    ),

    Reservation(
      "fastandfurious24_2",
      Map((8, 5) -> Adult, (8, 6) -> Adult, (8, 7) -> Adult, (9, 6) -> Adult, (9, 7) -> Adult),
      Person("The", "Pope")
    ),

  )

}
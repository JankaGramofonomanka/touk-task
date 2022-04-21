package database

import com.github.nscala_time.time.Imports._

import lib.DataDefs._

object MockDataBase {

  

    
  val rooms: Map[RoomId, (Int, Int)] = Map(
      1 -> (20, 15),
      2 -> (15, 10),
      3 -> (10, 10),
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
      List((15, 10), (15, 11), (15, 12)),
      Person("Don", "Corleone")
    ),
    
    Reservation(
      "scarface_1",
      List((15, 6), (15, 7), (15, 8), (15, 9)),
      Person("John", "Smith")
    ),
    
    Reservation(
      "scarface_2",
      List((10, 8), (10, 9), (10, 10)),
      Person("Buck", "Rodgers")
    ),

    Reservation(
      "12angrymen",
      List((8, 4)),
      Person("John", "Galt")
    ),

    Reservation(
      "12angrymen",
      List((8, 5)),
      Person("Henry", "Rearden")
    ),

    Reservation(
      "shrek_1",
      List((8, 5), (8, 6), (8, 7), (9, 6), (9, 7)),
      Person("Bugs", "Bunny")
    ),

    Reservation(
      "shrek_3",
      List((7, 8), (7, 9), (7, 10)),
      Person("Bugs", "Bunny")
    ),

    Reservation(
      "shrek_4",
      List((7, 8), (7, 9)),
      Person("Mao", "Tse-Tung")
    ),

    Reservation(
      "shrek_2",
      List((1, 4), (1, 5), (1, 6)),
      Person("Antonio", "Montana")
    ),

    Reservation(
      "pulpfiction",
      List((1, 4), (1, 5), (1, 6)),
      Person("Betty", "Boop")
    ),

  )

}
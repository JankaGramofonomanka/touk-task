# TouK task

## About
This is my solution to a recruitment task, which is described in 
`task_description.pdf`

The application will be implemented with accordance to the API defined in 
`touk-task-api.yaml`

## Prerequisites
To build and run the project you will need the `sbt` tool
(https://www.scala-sbt.org/download.html)

There is a demo, which requires the `jq` tool to run it, you can install it with:
```
sudo install jq
```


## Additional assumptions
- All screenings end before 12:00 PM.
- Screening rooms are rectangular, that is all rows have the same number of seats
- The expiration time of a reservation, is equal to the time when reservations 
  are no longer possible (15 minutes before the screening).

## Comments
- There is a `test/initDB` endpoint, which is not part of the API, and it is a 
  makeshift tool to initialize the database. This endpoint does not follow good
  practices, in particular it is not idempotent.

## Build and run
To build and run the project enter the project directory and executing the following:
```
./scripts/buildAndRun.sh
```
## Initialization of the Database
If you are running the app for the first time, the database will be empty.
To initialize the database, run the app and in another terminal execute the following:
```
./scripts/initDB.sh
```

The initial data is located in `scripts/initDB.json` and contains a set of 
rooms and screenings. For testing purposes it might be convenient to have the 
screenings start on the day, the testing is being done. 
For that purpose you can modify the `"date"` field of `scripts/initDB.json`,
You can also modify the start hours of individual screenings.

You can add more data to the database by modifying `scripts/initDB.json`, 
so that it contains only new data (re-running the script with the same data 
will insert duplicated data into the database).

The initialization of the database is done using the `/test/initDB` endpoint of
the app, therefore it is important that the app is running while the script is 
being executed.

## Database
If you wish to look into the database itself, you can set up your own MongoDB 
database and replace the `mongodb.uri` value with the url by which you will
connect to your database.
the url should be of the format:
`"mongodb+srv://<USERNAME>:<PASSWORD>@<CLUSTER-ADDRESS>/<DATABASE-NAME>"`
After that, you will need to recompile the project 
(restart `sbt` if you are in the `sbt` shell).

Before running the app you should create 3 collections named 
`rooms`, `screenings` and `reservations`.


## Demo
The demo is availible in 2 versions: interactive and non-interactive
To run the non-interactive demo, execute the following:
```
./scripts/demo.sh
```

To run the interactive demo, execute the following:
```
./scripts/demo_interactive.sh
```

The demo is making use of the `jq` command, you can install it with:
```
sudo install jq
```




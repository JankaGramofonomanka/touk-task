#!/bin/sh

URL="localhost:9000/test/initDB"

DATA=`cat scripts/initDB.json`

curl -X POST -H "Content-Type: application/json" -d "$DATA" $URL


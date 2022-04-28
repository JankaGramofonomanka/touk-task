#!/bin/bash

HOST="localhost:9000"
ALL_DATA=`cat scripts/demo.json`

printRequest() {
    METHOD=$1
    URL=$2
    HEADER=$3
    BODY=$4

    echo "METHOD:   $METHOD"
    echo ""
    echo "HEADERS:  $HEADER"
    echo ""
    echo "URL:      $URL"
    echo ""
    echo "BODY:"
    echo $BODY | jq
    echo ""
}

makeRequest() {
    METHOD=$1
    URL=$2
    HEADER=$3
    BODY=$4

    curl -X $METHOD -H "$HEADER" -d "$DATA" $URL
}

holdon() {
    echo ""
    echo "PRESS ENTER TO CONTINUE"
    read
}

echo ""
echo "---------------------------- LIST SCREENINGS ---------------------------"
DATE="30-04-2022"
FROM="12:00"
TO="20:00"
URL="$HOST/screenings?date=$DATE&from=$FROM&to=$TO"
printRequest "GET" $URL "" ""
SCREENINGS=`makeRequest "GET" $URL "" ""`
echo "RESPONSE:"
echo $SCREENINGS | jq
holdon

echo ""
echo "------------------------ REQUEST SCREENING INFO ------------------------"
SCREENING_ID=`echo $SCREENINGS | jq -r ".[0].screeningId"`
URL="$HOST/screenings/$SCREENING_ID"
printRequest "GET" $URL "" ""
SCREENING_INFO=`makeRequest "GET" $URL "" ""`
echo "RESPONSE:"
echo $SCREENING_INFO | jq
holdon

echo ""
echo "--------------------------- MAKE RESERVATION ---------------------------"
DATA=`echo $ALL_DATA | jq -r ".[0]"`
printRequest "POST" $URL "Content-Type: application/json" "$DATA"
RESULT=`makeRequest "POST" $URL "Content-Type: application/json" $DATA`
echo "RESPONSE:"
echo $RESULT
holdon

echo ""
echo "--------------------- UNSUCCCESSFUL RESERVATION 1 ----------------------"
DATA=`echo $ALL_DATA | jq -r ".[1]"`
printRequest "POST" $URL "Content-Type: application/json" "$DATA"
RESULT=`makeRequest "POST" $URL "Content-Type: application/json" $DATA`
echo "RESPONSE:"
echo $RESULT
holdon

echo ""
echo "--------------------- UNSUCCCESSFUL RESERVATION 2 ----------------------"
DATA=`echo $ALL_DATA | jq -r ".[2]"`
printRequest "POST" $URL "Content-Type: application/json" "$DATA"
RESULT=`makeRequest "POST" $URL "Content-Type: application/json" $DATA`
echo "RESPONSE:"
echo $RESULT


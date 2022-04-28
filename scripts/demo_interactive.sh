#!/bin/bash

HOST="localhost:9000"

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

enterSeat() {
    
    echo "enter row number:"
    read ROW
    echo "enter seat number:"
    read SEAT
    echo "enter ticket type:"
    read TICKET

    ENTER_SEAT_RESULT="{ \"row\": $ROW, \"seat\": $SEAT, \"ticket\": \"$TICKET\" }"
}

maybeContinue() {
    echo "continue (y/n)"
    read MAYBE_FINISH_RESULT
    
}

enterSeats() {
    echo "enter seats:"

    enterSeat
    
    CONTENT=$ENTER_SEAT_RESULT
    maybeContinue    

    while [ $MAYBE_FINISH_RESULT != "n" ]
    do
        enterSeat
        CONTENT="$CONTENT, $ENTER_SEAT_RESULT"
        maybeContinue
    done

    ENTER_SEATS_RESULT="[ $CONTENT ]"
    
}

echo ""
echo "---------------------------- LIST SCREENINGS ---------------------------"
echo "enter date:"
read DATE

echo "enter start hour:"
read FROM

echo "enter end hour:"
read TO

URL="$HOST/screenings?date=$DATE&from=$FROM&to=$TO"
printRequest "GET" $URL "" ""
SCREENINGS=`makeRequest "GET" $URL "" ""`
echo "RESPONSE:"
echo $SCREENINGS | jq

echo ""
echo "------------------------ REQUEST SCREENING INFO ------------------------"
echo "enter screening number (starting from 0)"
read SCREENING_NUM
SCREENING_ID=`echo $SCREENINGS | jq -r ".[$SCREENING_NUM].screeningId"`
URL="$HOST/screenings/$SCREENING_ID"
printRequest "GET" $URL "" ""
SCREENING_INFO=`makeRequest "GET" $URL "" ""`
echo "RESPONSE:"
echo $SCREENING_INFO | jq

echo ""
echo "--------------------------- MAKE RESERVATION ---------------------------"
echo "enter name:"
read NAME
echo "enter surname:"
read SURNAME
enterSeats
SEATS=$ENTER_SEATS_RESULT

DATA="{ \"name\": \"$NAME\", \"surname\": \"$SURNAME\", \"seats\": $SEATS}"
printRequest "POST" $URL "Content-Type: application/json" "$DATA"
RESULT=`makeRequest "POST" $URL "Content-Type: application/json" $DATA`
echo "RESPONSE:"
echo $RESULT

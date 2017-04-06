#!/bin/sh

cd `dirname $0`
java -jar secretaresse.jar > ~/.secretaresse/secretaresse.log 2>&1

if [ $? -ne 0 ]
then
  osascript -e 'tell app "Finder" to display dialog "Something went wrong with Secretaresse"'
fi


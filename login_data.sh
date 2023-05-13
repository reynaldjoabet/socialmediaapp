#!/bin/bash
echo " make sure the server is running"
echo " Sending requests to localhost:8080/api/users/login"

for i in {1..1}
 do
  curl --request POST  -v --header "Content-Type: application/json" --url 'http://localhost:8090/api/users/login' --data  '{
    "username":"test'$i'",
    "password":"12345'$i'"
   }' -H "Origin:http://localhost" -H "X-Csrf-Token:E96C522B730F1C27F" --cookie "csrf-token=E96C522B730F1C201F3EB636E8F3DEC9F35"


done

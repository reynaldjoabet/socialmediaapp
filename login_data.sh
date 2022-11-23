#!/bin/bash
echo " make sure the server is running"
echo " Sending requests to localhost:8080/api/users/login"

for i in {1..2000}
 do
  curl --request POST  -i -v --header "Content-Type: application/json" --url 'http://localhost:8080/api/users/login' --data  '{
    "username":"test'$i'",
    "password":"12345'$i'"
   }'


done

#!/bin/bash
echo " make sure the server is running"
echo " Sending requests to localhost:8080/api/users/register"

for i in {1001..2100}
 do
  curl --request POST -sL -i -v --header "Content-Type: application/json" --url 'http://localhost:8080/api/users/register' --data  '{
    "username":"test'$i'",
    "email":"test'$i'@gmail.com",
    "password":"12345'$i'",
    "name":"John Doe'$i'"
   }'


done

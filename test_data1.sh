#!/bin/bash
echo " make sure the server is running"
echo " Sending requests to localhost:8080/api/users/register"

for i in {11..40}
 do
  curl --request POST -sL -i -v --header "Content-Type: application/json" --url 'http://localhost:8090/api/users/register' --data  '{
    "username":"test'$i'",
    "email":"test'$i'@gmail.com",
    "password":"12345'$i'",
    "name":"John Doe'$i'"
   }'


done

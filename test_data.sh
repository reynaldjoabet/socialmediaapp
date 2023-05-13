#!/bin/bash
echo " make sure the server is running"
echo " Sending requests to localhost:8080/api/users/register"

for i in {1..2}
 do
  curl --request POST -sL -v --cookie "csrf-token=E96C522B730F1C27FA3C9CE146A4E40543E6E44873687E0DEC0A0DD929014" --header "Content-Type: application/json" --url 'http://localhost:8090/api/users/register' --data  '{
    "username":"test'$i'",
    "email":"test'$i'@gmail.com",
    "password":"12345'$i'",
    "name":"John Doe'$i'"
   }'


done

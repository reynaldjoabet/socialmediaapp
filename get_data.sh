#!/bin/bash
echo " make sure the server is running"
echo " Sending requests to localhost:8090//api/posts"

for i in {1..3}
 do
  curl --request GET -v  --url 'http://localhost:8090/api/posts' -H "Origin:http://localhost" -H "X-Csrf-Token:E96C522B730F1C0A0DD9290149" --cookie "csrf-token=E96C522B730F1"

done
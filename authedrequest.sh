#!/bin/bash
echo " make sure the server is running"
echo " Sending requests to localhost:8090/api/posts/add"


for i in {1..2}
 do
  curl --request POST -H -i -v --header "Authorization: Bearer e" --url 'http://localhost:8090/api/relationships/add' --data  '{
     "follower_user_id":'$i',
      "followed_user_id":'$i'
   }'


  

done
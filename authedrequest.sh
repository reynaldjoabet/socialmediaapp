#!/bin/bash
echo " make sure the server is running"
echo " Sending requests to localhost:8080/api/posts/add"


for i in {1..2}
 do
  curl --request POST  -i -v --header 'authorization: Bearer '$BEARER_TOKEN'' --url 'http://localhost:8080/api/relationships/add' --data  '{
     "follower_user_id":'$i',
      "followed_user_id":'$i'
   }'

 curl --request POST  -i -v --header 'authorization: Bearer '$BEARER_TOKEN'' --url 'http://localhost:8080/api/story/add' --data  '{
     "imageUrl":"test'$i'", 
     "userId":'$i'
   }'
  

done
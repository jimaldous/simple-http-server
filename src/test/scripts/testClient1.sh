#!/bin/bash
echo "---------- Test HTTP Client 1"
sleep 1
curl -X GET -H "Connection: keep-alive" http://localhost:8080
curl -X GET -H "Connection: keep-alive" http://localhost:8080/Shakespeare.txt
curl -X GET -H "Connection: keep-alive" http://localhost:8080/Quotes
curl -X GET -H "Connection: keep-alive" http://localhost:8080/Quotes/life.txt
curl -X GET -H "Connection: keep-alive" http://localhost:8080/Sonnets
curl -X GET -H "Connection: keep-alive" http://localhost:8080/Sonnets/Sonnet18.txt

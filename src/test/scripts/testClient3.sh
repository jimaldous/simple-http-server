#!/bin/bash
echo "---------- Test HTTP Client 3"
sleep 1
curl -X HEAD http://localhost:8080/Shakespeare.txt
curl -X HEAD http://localhost:8080/William.txt

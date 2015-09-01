#!/bin/bash
echo "----- Running HTTP Client Tests"
sleep 1
(./testClient1.sh) &
(./testClient2.sh) &
echo "----- Done"

#!/usr/bin/env bash

number="$1"

while ((number > 0)); do
  number=$((number - 1)) &&
    ./restart.sh &&
    java -jar test/target/test.jar "$2"
done

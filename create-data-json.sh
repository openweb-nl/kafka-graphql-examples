#!/usr/bin/env bash

[ -z "$1" ] && echo "no name provided" && exit 1
java -jar test/target/test.jar "$1"


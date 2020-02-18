#!/usr/bin/env bash

git submodule update --init &&
  cd frontend &&
  ./build-and-copy-frontend.sh &&
  cd ../topology && lein install && cd .. &&
  lein modules uberjar &&
  mkdir secrets && cd secrets && ../create-certs.sh && cd .. && rm -rf secrets &&
  docker-compose -f docker-bank.yml -f docker-prep.yml build --no-cache

#!/usr/bin/env bash

./clean.sh &&
  docker-compose -f docker-cluster.yml up -d &&
  sleep 10 &&
  ./setup-db-ch.sh db-ch 25432 &&
  ./setup-db-ge.sh db-ge 25431 &&
  ./synchronize.sh &&
  docker-compose -f docker-bank.yml up -d

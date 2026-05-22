#!/usr/bin/env bash
set -euo pipefail

echo "[dev] Starting dev stack"

docker compose \
  -f docker-compose.yml \
  -f docker-compose.dev.yml \
  up --build -d --wait

echo "[dev] Stack status"
docker compose \
  -f docker-compose.yml \
  -f docker-compose.dev.yml \
  ps

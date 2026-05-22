#!/usr/bin/env bash
set -euo pipefail

CLEAN=false
if [[ "${1-}" == "--clean" ]]; then
  CLEAN=true
fi

echo "[down] Stopping stack"
if [[ "$CLEAN" == "true" ]]; then
  echo "[down] Removing volumes"
  docker compose \
    -f docker-compose.yml \
    -f docker-compose.dev.yml \
    -f docker-compose.prod.yml \
    down -v
else
  docker compose \
    -f docker-compose.yml \
    -f docker-compose.dev.yml \
    -f docker-compose.prod.yml \
    down
fi

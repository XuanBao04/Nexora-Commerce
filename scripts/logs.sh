#!/usr/bin/env bash
set -euo pipefail

SERVICE="${1-}"

if [[ -n "$SERVICE" ]]; then
  echo "[logs] Following logs for service: $SERVICE"
  docker compose \
    -f docker-compose.yml \
    -f docker-compose.dev.yml \
    logs -f "$SERVICE"
else
  echo "[logs] Following logs for full stack"
  docker compose \
    -f docker-compose.yml \
    -f docker-compose.dev.yml \
    logs -f
fi

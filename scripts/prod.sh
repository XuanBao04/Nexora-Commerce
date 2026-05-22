#!/usr/bin/env bash
set -euo pipefail

echo "[prod] Stopping existing stack (if any)"
docker compose \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  down

echo "[prod] Building images (no cache)"
docker compose \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  build --no-cache

echo "[prod] Starting production stack"
docker compose \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  up -d

echo "[prod] Waiting for backend healthcheck"
for i in {1..30}; do
  if docker compose \
      -f docker-compose.yml \
      -f docker-compose.prod.yml \
      exec -T backend \
      curl -fsS http://localhost:8080/actuator/health >/dev/null; then
    echo "[prod] Backend healthy"
    exit 0
  fi
  echo "[prod] Healthcheck not ready ($i/30), retrying..."
  sleep 2
done

echo "[prod] Backend healthcheck failed"
exit 1

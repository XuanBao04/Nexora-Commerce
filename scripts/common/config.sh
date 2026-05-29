#!/usr/bin/env bash

# Project paths
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SCRIPTS_DIR="${PROJECT_ROOT}/scripts"
BACKEND_DIR="${PROJECT_ROOT}/backend"
FRONTEND_DIR="${PROJECT_ROOT}/frontend"

# Docker compose files
COMPOSE_FILE="${PROJECT_ROOT}/docker/docker-compose.yml"
COMPOSE_DEV="${PROJECT_ROOT}/docker/docker-compose.dev.yml"
COMPOSE_PROD="${PROJECT_ROOT}/docker/docker-compose.prod.yml"

# Environment
ENV_FILE="${PROJECT_ROOT}/.env"
if [[ -f "${ENV_FILE}" ]]; then
  set -a
  source "${ENV_FILE}"
  set +a
fi

# Service names
BACKEND_SERVICE="backend"
FRONTEND_SERVICE="frontend"
DB_SERVICE="db"
REDIS_SERVICE="redis"

# Ports (from env or defaults)
SERVER_PORT=${SERVER_PORT:-8080}
POSTGRES_PORT=${POSTGRES_PORT:-5432}
REDIS_PORT=${REDIS_PORT:-6379}
FRONTEND_PORT=${FRONTEND_PORT:-5173}

# Docker registry
DOCKER_REGISTRY=${DOCKER_REGISTRY:-""}
IMAGE_PREFIX=${DOCKER_REGISTRY:+${DOCKER_REGISTRY}/}"nexora"

# Set compose files based on environment
COMPOSE_ENV=${COMPOSE_ENV:-dev}
if [[ "${COMPOSE_ENV}" == "prod" ]]; then
  COMPOSE_FILES="-f ${COMPOSE_FILE} -f ${COMPOSE_PROD}"
else
  COMPOSE_FILES="-f ${COMPOSE_FILE} -f ${COMPOSE_DEV}"
fi

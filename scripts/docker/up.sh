#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../common/colors.sh"
source "${SCRIPT_DIR}/../common/config.sh"
source "${SCRIPT_DIR}/../common/helpers.sh"

print_help() {
  cat << EOF
${COLOR_CYAN}${COLOR_BOLD}Docker Compose Up - Start All Containers${COLOR_RESET}

${COLOR_BOLD}Usage:${COLOR_RESET}
  $(basename "$0") [OPTION]

${COLOR_BOLD}Options:${COLOR_RESET}
  --build        Rebuild images before starting
  --no-build     Don't rebuild images (default)
  --wait         Wait for services to be healthy
  --detach, -d   Run in background (default)
  --foreground   Run in foreground (attach logs)

${COLOR_BOLD}Environment:${COLOR_RESET}
  COMPOSE_ENV    Set to 'prod' for production environment (default: dev)

${COLOR_BOLD}Example:${COLOR_RESET}
  ./docker/up.sh                 # Start with existing images
  ./docker/up.sh --build         # Rebuild and start
  COMPOSE_ENV=prod ./docker/up.sh
EOF
}

if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
  print_help
  exit 0
fi

print_header "Docker Compose Up"

# Parse arguments
BUILD_FLAG=""
WAIT_FLAG=""
DETACH_FLAG="-d"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --build) BUILD_FLAG="--build" ;;
    --no-build) BUILD_FLAG="" ;;
    --wait) WAIT_FLAG="--wait" ;;
    --detach|-d) DETACH_FLAG="-d" ;;
    --foreground) DETACH_FLAG="" ;;
    --help|-h) print_help; exit 0 ;;
  esac
  shift
done

# Display configuration
print_section "Configuration"
echo -e "  Environment:  ${COLOR_GREEN}${COMPOSE_ENV}${COLOR_RESET}"
echo -e "  Compose File: ${COLOR_CYAN}${COMPOSE_FILES}${COLOR_RESET}"
echo ""

# Start containers
print_section "Starting Containers"
log_info "Running: docker compose ${COMPOSE_FILES} up ${DETACH_FLAG} ${BUILD_FLAG} ${WAIT_FLAG}"

docker compose ${COMPOSE_FILES} up ${DETACH_FLAG} ${BUILD_FLAG} ${WAIT_FLAG}

if [[ "${DETACH_FLAG}" == "-d" ]]; then
  print_section "Container Status"
  docker compose ${COMPOSE_FILES} ps
  
  print_section "Service URLs"
  echo -e "  Frontend:  ${COLOR_GREEN}http://localhost:${FRONTEND_PORT}${COLOR_RESET}"
  echo -e "  Backend:   ${COLOR_GREEN}http://localhost:${SERVER_PORT}${COLOR_RESET}"
  echo -e "  Swagger:   ${COLOR_GREEN}http://localhost:${SERVER_PORT}/swagger-ui.html${COLOR_RESET}"
  echo -e "  Database:  ${COLOR_CYAN}postgres://localhost:${POSTGRES_PORT}${COLOR_RESET}"
  echo -e "  Redis:     ${COLOR_CYAN}redis://localhost:${REDIS_PORT}${COLOR_RESET}"
  echo ""
  log_success "Containers started successfully"
fi

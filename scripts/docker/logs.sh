#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../common/colors.sh"
source "${SCRIPT_DIR}/../common/config.sh"
source "${SCRIPT_DIR}/../common/helpers.sh"

print_help() {
  cat << EOF
${COLOR_CYAN}${COLOR_BOLD}System Logs - View Container Logs${COLOR_RESET}

${COLOR_BOLD}Usage:${COLOR_RESET}
  $(basename "$0") [SERVICE] [OPTION]

${COLOR_BOLD}Services:${COLOR_RESET}
  all        All services (default)
  frontend   Frontend service
  backend    Backend service
  db         Database service
  redis      Redis service

${COLOR_BOLD}Options:${COLOR_RESET}
  --follow, -f   Follow logs (default)
  --tail N       Show last N lines (default: 50)
  --timestamps   Show timestamps

${COLOR_BOLD}Example:${COLOR_RESET}
  ./docker/logs.sh                 # All services, follow
  ./docker/logs.sh backend         # Backend only
  ./docker/logs.sh all --tail 100  # Last 100 lines
EOF
}

if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
  print_help
  exit 0
fi

print_header "System Logs"

# Parse arguments
SERVICE="${1:-all}"
FOLLOW="-f"
TAIL="50"
TIMESTAMPS=""

shift || true

while [[ $# -gt 0 ]]; do
  case "$1" in
    --follow|-f) FOLLOW="-f" ;;
    --tail)
      if [[ -n "${2:-}" ]] && [[ "${2}" =~ ^[0-9]+$ ]]; then
        TAIL="$2"
        shift
      fi
      ;;
    --timestamps) TIMESTAMPS="--timestamps" ;;
    --help|-h) print_help; exit 0 ;;
  esac
  shift
done

# Map service names
case "${SERVICE}" in
  all) SERVICE_NAMES="" ;;
  frontend) SERVICE_NAMES="${FRONTEND_SERVICE}" ;;
  backend) SERVICE_NAMES="${BACKEND_SERVICE}" ;;
  db|database) SERVICE_NAMES="${DB_SERVICE}" ;;
  redis) SERVICE_NAMES="${REDIS_SERVICE}" ;;
  *)
    log_error "Unknown service: ${SERVICE}"
    exit 1
    ;;
esac

log_info "Showing logs for: ${SERVICE:=all services}"
echo ""

docker compose ${COMPOSE_FILES} logs ${FOLLOW} --tail ${TAIL} ${TIMESTAMPS} ${SERVICE_NAMES}

#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../common/colors.sh"
source "${SCRIPT_DIR}/../common/config.sh"
source "${SCRIPT_DIR}/../common/helpers.sh"

print_help() {
  cat << EOF
${COLOR_CYAN}${COLOR_BOLD}Backend Stop - Stop & Remove Container${COLOR_RESET}

${COLOR_BOLD}Usage:${COLOR_RESET}
  $(basename "$0")

${COLOR_BOLD}Description:${COLOR_RESET}
  Stops and removes the Backend container.
  Volumes and network are preserved.

${COLOR_BOLD}Example:${COLOR_RESET}
  ./backend/stop.sh
EOF
}

if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
  print_help
  exit 0
fi

print_header "Backend Stop"

if docker compose ${COMPOSE_FILES} ps ${BACKEND_SERVICE} 2>/dev/null | grep -q "${BACKEND_SERVICE}"; then
  print_section "Stopping Backend container"
  docker compose ${COMPOSE_FILES} stop ${BACKEND_SERVICE}
  log_success "Backend stopped"
  
  print_section "Removing Backend container"
  docker compose ${COMPOSE_FILES} rm -f ${BACKEND_SERVICE}
  log_success "Backend container removed"
else
  log_warn "Backend container is not running"
fi

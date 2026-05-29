#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../common/colors.sh"
source "${SCRIPT_DIR}/../common/config.sh"
source "${SCRIPT_DIR}/../common/helpers.sh"

print_help() {
  cat << EOF
${COLOR_CYAN}${COLOR_BOLD}Frontend Stop - Stop & Remove Container${COLOR_RESET}

${COLOR_BOLD}Usage:${COLOR_RESET}
  $(basename "$0")

${COLOR_BOLD}Description:${COLOR_RESET}
  Stops and removes the Frontend container.
  Volumes and network are preserved.

${COLOR_BOLD}Example:${COLOR_RESET}
  ./frontend/stop.sh
EOF
}

if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
  print_help
  exit 0
fi

print_header "Frontend Stop"

if docker compose ${COMPOSE_FILES} ps ${FRONTEND_SERVICE} 2>/dev/null | grep -q "${FRONTEND_SERVICE}"; then
  print_section "Stopping Frontend container"
  docker compose ${COMPOSE_FILES} stop ${FRONTEND_SERVICE}
  log_success "Frontend stopped"
  
  print_section "Removing Frontend container"
  docker compose ${COMPOSE_FILES} rm -f ${FRONTEND_SERVICE}
  log_success "Frontend container removed"
else
  log_warn "Frontend container is not running"
fi

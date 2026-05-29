#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../common/colors.sh"
source "${SCRIPT_DIR}/../common/config.sh"
source "${SCRIPT_DIR}/../common/helpers.sh"

print_help() {
  cat << EOF
${COLOR_CYAN}${COLOR_BOLD}Fullstack Logs - View All Service Logs${COLOR_RESET}

${COLOR_BOLD}Usage:${COLOR_RESET}
  $(basename "$0") [SERVICE] [OPTION]

${COLOR_BOLD}Services:${COLOR_RESET}
  all        All services (default)
  frontend   Frontend only
  backend    Backend only
  db         Database only
  redis      Redis only

${COLOR_BOLD}Options:${COLOR_RESET}
  --follow, -f   Follow logs (default)
  --tail N       Show last N lines (default: 50)
  --timestamps   Include timestamps

${COLOR_BOLD}Example:${COLOR_RESET}
  ./fullstack/logs.sh                # View all logs
  ./fullstack/logs.sh backend        # Backend logs only
  ./fullstack/logs.sh all --tail 100 # Last 100 lines
EOF
}

if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
  print_help
  exit 0
fi

print_header "Fullstack Logs"

bash "${SCRIPT_DIR}/../docker/logs.sh" "$@"

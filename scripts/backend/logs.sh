#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../common/colors.sh"
source "${SCRIPT_DIR}/../common/config.sh"
source "${SCRIPT_DIR}/../common/helpers.sh"

print_help() {
  cat << EOF
${COLOR_CYAN}${COLOR_BOLD}Backend Logs${COLOR_RESET}

${COLOR_BOLD}Usage:${COLOR_RESET}
  $(basename "$0") [OPTION]

${COLOR_BOLD}Options:${COLOR_RESET}
  --follow, -f   Follow logs (default)
  --tail N       Show last N lines (default: 50)
  --help         Show this help message

${COLOR_BOLD}Example:${COLOR_RESET}
  ./backend/logs.sh              # Follow logs
  ./backend/logs.sh --tail 100   # Show last 100 lines
  ./backend/logs.sh -f --tail 20 # Follow last 20 lines
EOF
}

if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
  print_help
  exit 0
fi

# Parse arguments
FOLLOW="-f"
TAIL="50"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --follow|-f) FOLLOW="-f" ;;
    --tail)
      if [[ -n "${2:-}" ]] && [[ "${2}" =~ ^[0-9]+$ ]]; then
        TAIL="$2"
        shift
      fi
      ;;
    --help|-h) print_help; exit 0 ;;
  esac
  shift
done

print_header "Backend Logs"

log_info "Showing last ${TAIL} lines (streaming)..."
echo ""

docker compose ${COMPOSE_FILES} logs ${FOLLOW} --tail ${TAIL} ${BACKEND_SERVICE}

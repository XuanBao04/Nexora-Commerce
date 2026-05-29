#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../common/colors.sh"
source "${SCRIPT_DIR}/../common/config.sh"
source "${SCRIPT_DIR}/../common/helpers.sh"

print_help() {
  cat << EOF
${COLOR_CYAN}${COLOR_BOLD}Fullstack Restart - Restart All Services${COLOR_RESET}

${COLOR_BOLD}Usage:${COLOR_RESET}
  $(basename "$0") [OPTION]

${COLOR_BOLD}Options:${COLOR_RESET}
  --build        Rebuild images before restarting
  --wait         Wait for services to be healthy
  --foreground   Attach logs to terminal

${COLOR_BOLD}Example:${COLOR_RESET}
  ./fullstack/restart.sh              # Restart normally
  ./fullstack/restart.sh --build      # Rebuild and restart
EOF
}

if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
  print_help
  exit 0
fi

print_header "Fullstack Restart"

# Stop services
bash "${SCRIPT_DIR}/stop.sh"

# Wait a moment
sleep 2

# Start services
bash "${SCRIPT_DIR}/start.sh" "$@"

log_success "Fullstack restarted successfully"

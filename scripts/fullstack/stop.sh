#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../common/colors.sh"
source "${SCRIPT_DIR}/../common/config.sh"
source "${SCRIPT_DIR}/../common/helpers.sh"

print_help() {
  cat << EOF
${COLOR_CYAN}${COLOR_BOLD}Fullstack Stop - Stop All Services${COLOR_RESET}

${COLOR_BOLD}Usage:${COLOR_RESET}
  $(basename "$0") [OPTION]

${COLOR_BOLD}Options:${COLOR_RESET}
  --volumes      Remove volumes and data
  --clean        Stop and remove orphan containers

${COLOR_BOLD}Example:${COLOR_RESET}
  ./fullstack/stop.sh                 # Stop normally
  ./fullstack/stop.sh --volumes       # Stop and remove data
EOF
}

if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
  print_help
  exit 0
fi

print_header "Fullstack Stop"

# Parse arguments
DOCKER_DOWN_ARGS=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --volumes) DOCKER_DOWN_ARGS="--volumes" ;;
    --clean) DOCKER_DOWN_ARGS="--remove-orphans" ;;
    --help|-h) print_help; exit 0 ;;
  esac
  shift
done

bash "${SCRIPT_DIR}/../docker/down.sh" ${DOCKER_DOWN_ARGS}

log_success "Fullstack stopped successfully"

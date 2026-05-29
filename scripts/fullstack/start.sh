#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../common/colors.sh"
source "${SCRIPT_DIR}/../common/config.sh"
source "${SCRIPT_DIR}/../common/helpers.sh"

print_help() {
  cat << EOF
${COLOR_CYAN}${COLOR_BOLD}Fullstack Start - Start All Services${COLOR_RESET}

${COLOR_BOLD}Usage:${COLOR_RESET}
  $(basename "$0") [OPTION]

${COLOR_BOLD}Options:${COLOR_RESET}
  --build        Build images before starting
  --wait         Wait for services to be healthy
  --foreground   Attach logs to terminal

${COLOR_BOLD}Environment:${COLOR_RESET}
  COMPOSE_ENV    'dev' or 'prod' (default: dev)

${COLOR_BOLD}Example:${COLOR_RESET}
  ./fullstack/start.sh                # Start with existing images
  ./fullstack/start.sh --build        # Build and start
  COMPOSE_ENV=prod ./fullstack/start.sh --build
EOF
}

if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
  print_help
  exit 0
fi

print_header "Fullstack Start"

# Parse arguments
BUILD_IMAGES=false
WAIT_SERVICES=false
FOREGROUND=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --build) BUILD_IMAGES=true ;;
    --wait) WAIT_SERVICES=true ;;
    --foreground) FOREGROUND=true ;;
    --help|-h) print_help; exit 0 ;;
  esac
  shift
done

# Build if requested
if [[ "${BUILD_IMAGES}" == "true" ]]; then
  bash "${SCRIPT_DIR}/build.sh" --docker
fi

# Start containers
print_section "Starting Containers"

DOCKER_UP_ARGS=""
if [[ "${WAIT_SERVICES}" == "true" ]]; then
  DOCKER_UP_ARGS="--wait"
fi

if [[ "${FOREGROUND}" == "true" ]]; then
  bash "${SCRIPT_DIR}/../docker/up.sh" ${DOCKER_UP_ARGS} --foreground
else
  bash "${SCRIPT_DIR}/../docker/up.sh" ${DOCKER_UP_ARGS}
fi

log_success "Fullstack started successfully"

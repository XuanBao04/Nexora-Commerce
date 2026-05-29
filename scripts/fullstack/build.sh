#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../common/colors.sh"
source "${SCRIPT_DIR}/../common/config.sh"
source "${SCRIPT_DIR}/../common/helpers.sh"

print_help() {
  cat << EOF
${COLOR_CYAN}${COLOR_BOLD}Fullstack Build - Build Frontend & Backend${COLOR_RESET}

${COLOR_BOLD}Usage:${COLOR_RESET}
  $(basename "$0") [OPTION]

${COLOR_BOLD}Options:${COLOR_RESET}
  --skip-tests   Skip unit tests
  --docker       Build Docker images instead of JAR/dist
  --api-url URL  API URL for frontend build

${COLOR_BOLD}Example:${COLOR_RESET}
  ./fullstack/build.sh                    # Build both (with tests)
  ./fullstack/build.sh --skip-tests       # Build both (no tests)
  ./fullstack/build.sh --docker           # Build Docker images
  ./fullstack/build.sh --docker --skip-tests
EOF
}

if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
  print_help
  exit 0
fi

print_header "Fullstack Build"

# Parse arguments
SKIP_TESTS=false
DOCKER_BUILD=false
API_URL=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-tests) SKIP_TESTS=true ;;
    --docker) DOCKER_BUILD=true ;;
    --api-url)
      if [[ -n "${2:-}" ]]; then
        API_URL="$2"
        shift
      fi
      ;;
    --help|-h) print_help; exit 0 ;;
  esac
  shift
done

# Build backend
print_section "Building Backend"
if [[ "${DOCKER_BUILD}" == "true" ]]; then
  bash "${SCRIPT_DIR}/../backend/docker-build.sh" latest
else
  ARGS="--skip-tests" || ARGS=""
  if [[ "${SKIP_TESTS}" == "true" ]]; then
    bash "${SCRIPT_DIR}/../backend/build.sh" --skip-tests
  else
    bash "${SCRIPT_DIR}/../backend/build.sh"
  fi
fi

# Build frontend
print_section "Building Frontend"
if [[ "${DOCKER_BUILD}" == "true" ]]; then
  if [[ -n "${API_URL}" ]]; then
    bash "${SCRIPT_DIR}/../frontend/docker-build.sh" latest --api-url "${API_URL}"
  else
    bash "${SCRIPT_DIR}/../frontend/docker-build.sh" latest
  fi
else
  if [[ -n "${API_URL}" ]]; then
    bash "${SCRIPT_DIR}/../frontend/build.sh" --api-url "${API_URL}"
  else
    bash "${SCRIPT_DIR}/../frontend/build.sh"
  fi
fi

log_success "Fullstack build completed successfully"

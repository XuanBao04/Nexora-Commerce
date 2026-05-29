#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../common/colors.sh"
source "${SCRIPT_DIR}/../common/config.sh"
source "${SCRIPT_DIR}/../common/helpers.sh"

print_help() {
  cat << EOF
${COLOR_CYAN}${COLOR_BOLD}Docker Build - Build All Images${COLOR_RESET}

${COLOR_BOLD}Usage:${COLOR_RESET}
  $(basename "$0") [OPTION]

${COLOR_BOLD}Options:${COLOR_RESET}
  --frontend   Build frontend image only
  --backend    Build backend image only
  --all        Build all images (default)
  --tag TAG    Override image tag (default: latest)

${COLOR_BOLD}Example:${COLOR_RESET}
  ./docker/build.sh                    # Build all
  ./docker/build.sh --frontend         # Build frontend only
  ./docker/build.sh --backend          # Build backend only
  ./docker/build.sh --tag v1.0.0       # Build all with tag v1.0.0
EOF
}

if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
  print_help
  exit 0
fi

print_header "Docker Build - All Images"

# Parse arguments
BUILD_FRONTEND=true
BUILD_BACKEND=true
TAG=${TAG:-latest}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --frontend) BUILD_FRONTEND=true; BUILD_BACKEND=false ;;
    --backend) BUILD_BACKEND=true; BUILD_FRONTEND=false ;;
    --all) BUILD_FRONTEND=true; BUILD_BACKEND=true ;;
    --tag)
      if [[ -n "${2:-}" ]]; then
        TAG="$2"
        shift
      fi
      ;;
    --help|-h) print_help; exit 0 ;;
  esac
  shift
done

# Build frontend
if [[ "${BUILD_FRONTEND}" == "true" ]]; then
  bash "${SCRIPT_DIR}/../frontend/docker-build.sh" "${TAG}"
fi

# Build backend
if [[ "${BUILD_BACKEND}" == "true" ]]; then
  bash "${SCRIPT_DIR}/../backend/docker-build.sh" "${TAG}"
fi

log_success "All requested Docker images built"

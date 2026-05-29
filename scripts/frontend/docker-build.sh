#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../common/colors.sh"
source "${SCRIPT_DIR}/../common/config.sh"
source "${SCRIPT_DIR}/../common/helpers.sh"

print_help() {
  cat << EOF
${COLOR_CYAN}${COLOR_BOLD}Frontend Docker Build - Build Docker Image${COLOR_RESET}

${COLOR_BOLD}Usage:${COLOR_RESET}
  $(basename "$0") [TAG] [OPTIONS]

${COLOR_BOLD}Arguments:${COLOR_RESET}
  TAG            Image tag (default: latest)

${COLOR_BOLD}Options:${COLOR_RESET}
  --api-url URL  API base URL for build-time injection

${COLOR_BOLD}Example:${COLOR_RESET}
  ./frontend/docker-build.sh
  ./frontend/docker-build.sh v1.0.0
  ./frontend/docker-build.sh latest --api-url http://api.example.com/api
EOF
}

if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
  print_help
  exit 0
fi

print_header "Frontend Docker Build"

# Image configuration
TAG=${1:-latest}
IMAGE_NAME="${IMAGE_PREFIX}-frontend"
IMAGE_TAG="${IMAGE_NAME}:${TAG}"

# Parse options
VITE_API_URL=${VITE_API_URL:-/api}
shift || true

while [[ $# -gt 0 ]]; do
  case "$1" in
    --api-url)
      if [[ -n "${2:-}" ]]; then
        VITE_API_URL="$2"
        shift 2
      else
        shift
      fi
      ;;
  esac
done

print_section "Image Information"
echo -e "  Image:    ${COLOR_GREEN}${IMAGE_TAG}${COLOR_RESET}"
echo -e "  API URL:  ${COLOR_CYAN}${VITE_API_URL}${COLOR_RESET}"
echo -e "  Path:     ${COLOR_CYAN}${FRONTEND_DIR}${COLOR_RESET}"
echo ""

print_section "Building Docker Image"
log_info "Building Docker image: ${IMAGE_TAG}"

docker build \
  -f "${FRONTEND_DIR}/Dockerfile" \
  -t "${IMAGE_TAG}" \
  -t "${IMAGE_NAME}:latest" \
  --build-arg VITE_API_URL="${VITE_API_URL}" \
  "${FRONTEND_DIR}"

if [[ $(docker images -q "${IMAGE_TAG}" 2>/dev/null) ]]; then
  IMAGE_SIZE=$(docker images --format "{{.Size}}" "${IMAGE_TAG}")
  log_success "Docker image built successfully"
  log_success "Image: ${IMAGE_TAG} (${IMAGE_SIZE})"
else
  log_error "Failed to build Docker image"
  exit 1
fi

print_section "Next Steps"
echo -e "  Push:  docker push ${IMAGE_TAG}"
echo -e "  Run:   docker run -d -p 80:80 --name frontend ${IMAGE_TAG}"
echo -e "  Test:  docker run --rm -p 3000:80 ${IMAGE_TAG}"

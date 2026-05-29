#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../common/colors.sh"
source "${SCRIPT_DIR}/../common/config.sh"
source "${SCRIPT_DIR}/../common/helpers.sh"

print_help() {
  cat << EOF
${COLOR_CYAN}${COLOR_BOLD}Backend Docker Build - Build Docker Image${COLOR_RESET}

${COLOR_BOLD}Usage:${COLOR_RESET}
  $(basename "$0") [TAG]

${COLOR_BOLD}Arguments:${COLOR_RESET}
  TAG          Image tag (default: latest)

${COLOR_BOLD}Example:${COLOR_RESET}
  ./backend/docker-build.sh                # Build with 'latest' tag
  ./backend/docker-build.sh v1.0.0         # Build with 'v1.0.0' tag
EOF
}

if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
  print_help
  exit 0
fi

print_header "Backend Docker Build"

# Image configuration
TAG=${1:-latest}
IMAGE_NAME="${IMAGE_PREFIX}-backend"
IMAGE_TAG="${IMAGE_NAME}:${TAG}"

print_section "Image Information"
echo -e "  Image:  ${COLOR_GREEN}${IMAGE_TAG}${COLOR_RESET}"
echo -e "  Path:   ${COLOR_CYAN}${BACKEND_DIR}${COLOR_RESET}"
echo ""

print_section "Building Docker Image"

# Check if JAR exists, if not build it first
JAR_FILE="${BACKEND_DIR}/target/nexora-commerce-backend-1.0.0.jar"
if [[ ! -f "${JAR_FILE}" ]]; then
  log_warn "JAR not found, building..."
  bash "${SCRIPT_DIR}/build.sh" --skip-tests || {
    log_error "Failed to build JAR"
    exit 1
  }
fi

# Build Docker image
log_info "Building Docker image: ${IMAGE_TAG}"
docker build \
  -f "${BACKEND_DIR}/Dockerfile" \
  -t "${IMAGE_TAG}" \
  -t "${IMAGE_NAME}:latest" \
  "${BACKEND_DIR}"

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
echo -e "  Run:   docker run -d --name backend ${IMAGE_TAG}"
echo -e "  Test:  docker run --rm ${IMAGE_TAG} sh -c 'java -version'"

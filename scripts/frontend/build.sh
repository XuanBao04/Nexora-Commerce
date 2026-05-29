#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../common/colors.sh"
source "${SCRIPT_DIR}/../common/config.sh"
source "${SCRIPT_DIR}/../common/helpers.sh"

print_help() {
  cat << EOF
${COLOR_CYAN}${COLOR_BOLD}Frontend Build - Build for Production${COLOR_RESET}

${COLOR_BOLD}Usage:${COLOR_RESET}
  $(basename "$0") [OPTION]

${COLOR_BOLD}Options:${COLOR_RESET}
  --api-url URL  Set API base URL (default: /api)
  --help         Show this help message

${COLOR_BOLD}Example:${COLOR_RESET}
  ./frontend/build.sh                          # Build with default API
  ./frontend/build.sh --api-url /api           # Custom API path
  ./frontend/build.sh --api-url http://api.example.com/api
EOF
}

if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
  print_help
  exit 0
fi

print_header "Frontend Build"

# Check prerequisites
if ! command_exists node; then
  log_error "Node.js not found. Please install Node.js 20+"
  exit 1
fi

if ! command_exists pnpm; then
  log_error "pnpm not found. Install: npm install -g pnpm"
  exit 1
fi

# Parse arguments
VITE_API_URL="${VITE_API_URL:-/api}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --api-url)
      if [[ -n "${2:-}" ]]; then
        VITE_API_URL="$2"
        shift
      fi
      ;;
    --help|-h) print_help; exit 0 ;;
  esac
  shift
done

# Install dependencies if needed
if [[ ! -d "${FRONTEND_DIR}/node_modules" ]]; then
  print_section "Installing Dependencies"
  cd "${FRONTEND_DIR}"
  pnpm install
fi

print_section "Building Frontend"
log_info "API URL: ${VITE_API_URL}"

cd "${FRONTEND_DIR}"

# Build
VITE_API_URL="${VITE_API_URL}" pnpm build

# Check output
if [[ -d "${FRONTEND_DIR}/dist" ]]; then
  DIST_SIZE=$(du -sh "${FRONTEND_DIR}/dist" | cut -f1)
  log_success "Build completed successfully"
  log_success "Output: ${FRONTEND_DIR}/dist (${DIST_SIZE})"
else
  log_error "Build output not found: ${FRONTEND_DIR}/dist"
  exit 1
fi

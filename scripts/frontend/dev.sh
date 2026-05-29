#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../common/colors.sh"
source "${SCRIPT_DIR}/../common/config.sh"
source "${SCRIPT_DIR}/../common/helpers.sh"

print_help() {
  cat << EOF
${COLOR_CYAN}${COLOR_BOLD}Frontend Development - Run Local${COLOR_RESET}

${COLOR_BOLD}Usage:${COLOR_RESET}
  $(basename "$0")

${COLOR_BOLD}Description:${COLOR_RESET}
  Start Vite development server with hot module replacement (HMR).
  Proxy API requests to backend.

${COLOR_BOLD}Prerequisites:${COLOR_RESET}
  - Node.js 20+ and pnpm installed
  - Backend running (http://localhost:8080)

${COLOR_BOLD}Environment:${COLOR_RESET}
  VITE_API_URL   API base URL (default: http://localhost:8080/api)

${COLOR_BOLD}Example:${COLOR_RESET}
  ./frontend/dev.sh
  VITE_API_URL=http://api.example.com ./frontend/dev.sh
EOF
}

if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
  print_help
  exit 0
fi

print_header "Frontend Development Mode"

# Check prerequisites
if ! command_exists node; then
  log_error "Node.js not found. Please install Node.js 20+"
  exit 1
fi

if ! command_exists pnpm; then
  log_error "pnpm not found. Install: npm install -g pnpm"
  exit 1
fi

# Load environment
load_env

# Display info
print_section "Frontend Info"
VITE_API_URL=${VITE_API_URL:-"http://localhost:${SERVER_PORT}/api"}
NODE_VERSION=$(node --version)
PNPM_VERSION=$(pnpm --version)

echo -e "  API URL:      ${COLOR_GREEN}${VITE_API_URL}${COLOR_RESET}"
echo -e "  Dev Server:   ${COLOR_GREEN}http://localhost:${FRONTEND_PORT}${COLOR_RESET}"
echo -e "  Node:         ${COLOR_CYAN}${NODE_VERSION}${COLOR_RESET}"
echo -e "  pnpm:         ${COLOR_CYAN}${PNPM_VERSION}${COLOR_RESET}"
echo ""

# Install dependencies if needed
if [[ ! -d "${FRONTEND_DIR}/node_modules" ]]; then
  print_section "Installing Dependencies"
  cd "${FRONTEND_DIR}"
  pnpm install
fi

# Start dev server
print_section "Starting Vite Dev Server"
log_info "Running: pnpm dev"
echo ""

cd "${FRONTEND_DIR}"
VITE_API_URL="${VITE_API_URL}" pnpm dev

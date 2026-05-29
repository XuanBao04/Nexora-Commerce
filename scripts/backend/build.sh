#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../common/colors.sh"
source "${SCRIPT_DIR}/../common/config.sh"
source "${SCRIPT_DIR}/../common/helpers.sh"

print_help() {
  cat << EOF
${COLOR_CYAN}${COLOR_BOLD}Backend Build - Compile JAR${COLOR_RESET}

${COLOR_BOLD}Usage:${COLOR_RESET}
  $(basename "$0") [OPTION]

${COLOR_BOLD}Options:${COLOR_RESET}
  --clean       Build with clean (remove target directory first)
  --skip-tests  Skip running unit tests
  --help        Show this help message

${COLOR_BOLD}Example:${COLOR_RESET}
  ./backend/build.sh                 # Build with tests
  ./backend/build.sh --skip-tests    # Build without tests
  ./backend/build.sh --clean         # Clean build
EOF
}

if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
  print_help
  exit 0
fi

print_header "Backend Build - JAR Compilation"

# Parse arguments
SKIP_TESTS=false
CLEAN_FLAG=""

for arg in "$@"; do
  case "$arg" in
    --skip-tests) SKIP_TESTS=true ;;
    --clean) CLEAN_FLAG="clean" ;;
  esac
done

print_section "Building Backend"

cd "${BACKEND_DIR}"

# Build command
BUILD_CMD="mvn ${CLEAN_FLAG} package"

if [[ "${SKIP_TESTS}" == "true" ]]; then
  BUILD_CMD="${BUILD_CMD} -DskipTests"
  log_info "Skipping tests"
fi

log_info "Running: ${BUILD_CMD}"
eval "${BUILD_CMD}"

# Check output
JAR_FILE="${BACKEND_DIR}/target/nexora-commerce-backend-1.0.0.jar"
if [[ -f "${JAR_FILE}" ]]; then
  JAR_SIZE=$(du -h "${JAR_FILE}" | cut -f1)
  log_success "Build completed successfully"
  log_success "JAR: ${JAR_FILE} (${JAR_SIZE})"
else
  log_error "JAR file not found: ${JAR_FILE}"
  exit 1
fi

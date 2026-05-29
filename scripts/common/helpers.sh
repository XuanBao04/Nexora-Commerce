#!/usr/bin/env bash

source "$(dirname "${BASH_SOURCE[0]}")/colors.sh"

# Error handling
set -o pipefail

# Log functions
log_info() {
  echo -e "${COLOR_BLUE}[INFO]${COLOR_RESET} $*"
}

log_success() {
  echo -e "${COLOR_GREEN}[✓]${COLOR_RESET} $*"
}

log_error() {
  echo -e "${COLOR_RED}[✗ ERROR]${COLOR_RESET} $*" >&2
}

log_warn() {
  echo -e "${COLOR_YELLOW}[⚠ WARN]${COLOR_RESET} $*"
}

log_debug() {
  if [[ "${DEBUG:-false}" == "true" ]]; then
    echo -e "${COLOR_CYAN}[DEBUG]${COLOR_RESET} $*"
  fi
}

# Print header
print_header() {
  echo ""
  echo -e "${COLOR_CYAN}${COLOR_BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${COLOR_RESET}"
  echo -e "${COLOR_CYAN}${COLOR_BOLD}  $1${COLOR_RESET}"
  echo -e "${COLOR_CYAN}${COLOR_BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${COLOR_RESET}"
  echo ""
}

# Print section
print_section() {
  echo ""
  echo -e "${COLOR_YELLOW}▶ $1${COLOR_RESET}"
}

# Check if command exists
command_exists() {
  command -v "$1" >/dev/null 2>&1
}

# Check if service is running
is_service_running() {
  local service=$1
  docker compose ${COMPOSE_FILES} exec -T "${service}" sh -c "exit 0" >/dev/null 2>&1
}

# Wait for service to be healthy
wait_for_service() {
  local service=$1
  local timeout=${2:-30}
  local elapsed=0
  
  log_info "Waiting for ${service} to be healthy (${timeout}s timeout)..."
  
  while [[ $elapsed -lt $timeout ]]; do
    if is_service_running "${service}"; then
      log_success "${service} is ready"
      return 0
    fi
    sleep 1
    ((elapsed++))
  done
  
  log_error "${service} did not become healthy within ${timeout}s"
  return 1
}

# Handle errors
on_error() {
  local line=$1
  local code=$2
  log_error "Script failed at line ${line} with exit code ${code}"
  exit ${code}
}

trap 'on_error ${LINENO} $?' ERR

# Check prerequisites
check_prerequisites() {
  # Check for docker (required for all scripts)
  if ! command_exists "docker"; then
    log_error "Required command not found: docker"
    log_error "Please install Docker Desktop or Docker Engine"
    return 1
  fi
  
  # Check for docker compose (built-in with modern Docker or via docker-compose command)
  if ! docker compose version >/dev/null 2>&1 && ! command_exists "docker-compose"; then
    log_error "Docker Compose not found"
    log_error "Please install Docker Compose (comes with Docker Desktop, or install separately)"
    return 1
  fi
  
  return 0
}

# Load environment from .env
load_env() {
  if [[ -f "${ENV_FILE}" ]]; then
    set -a
    source "${ENV_FILE}"
    set +a
    log_debug "Environment loaded from ${ENV_FILE}"
  else
    log_warn "Environment file not found: ${ENV_FILE}"
  fi
}

# Print help banner
print_usage() {
  local script_name=$(basename "$0")
  log_info "Usage: ${script_name} $*"
}

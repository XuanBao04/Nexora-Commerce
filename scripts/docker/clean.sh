#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../common/colors.sh"
source "${SCRIPT_DIR}/../common/config.sh"
source "${SCRIPT_DIR}/../common/helpers.sh"

print_help() {
  cat << EOF
${COLOR_CYAN}${COLOR_BOLD}Docker Clean - Remove Unused Resources${COLOR_RESET}

${COLOR_BOLD}Usage:${COLOR_RESET}
  $(basename "$0") [OPTION]

${COLOR_BOLD}Options:${COLOR_RESET}
  --images       Remove unused images
  --volumes      Remove unused volumes
  --all          Remove all unused resources (images + volumes)
  --prune        Docker system prune (aggressive cleanup)

${COLOR_BOLD}Example:${COLOR_RESET}
  ./docker/clean.sh --images         # Remove unused images
  ./docker/clean.sh --all            # Remove unused images & volumes
  ./docker/clean.sh --prune          # System-wide cleanup
EOF
}

if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
  print_help
  exit 0
fi

print_header "Docker Clean - Remove Unused Resources"

# Parse arguments
CLEAN_IMAGES=false
CLEAN_VOLUMES=false
SYSTEM_PRUNE=false

if [[ $# -eq 0 ]]; then
  print_help
  exit 0
fi

while [[ $# -gt 0 ]]; do
  case "$1" in
    --images) CLEAN_IMAGES=true ;;
    --volumes) CLEAN_VOLUMES=true ;;
    --all) CLEAN_IMAGES=true; CLEAN_VOLUMES=true ;;
    --prune) SYSTEM_PRUNE=true ;;
    --help|-h) print_help; exit 0 ;;
  esac
  shift
done

# Clean images
if [[ "${CLEAN_IMAGES}" == "true" ]]; then
  print_section "Removing Unused Images"
  REMOVED=$(docker image prune -f --filter "dangling=true" 2>&1 | tail -1)
  log_success "Images cleaned: ${REMOVED}"
fi

# Clean volumes
if [[ "${CLEAN_VOLUMES}" == "true" ]]; then
  print_section "Removing Unused Volumes"
  REMOVED=$(docker volume prune -f 2>&1 | tail -1)
  log_success "Volumes cleaned: ${REMOVED}"
fi

# System prune
if [[ "${SYSTEM_PRUNE}" == "true" ]]; then
  print_section "Docker System Prune"
  log_warn "This will remove all stopped containers, unused networks, and dangling images"
  read -p "Continue? (yes/no): " confirm
  if [[ "${confirm}" == "yes" ]]; then
    docker system prune -f
    log_success "System cleanup completed"
  else
    log_info "Cancelled"
  fi
fi

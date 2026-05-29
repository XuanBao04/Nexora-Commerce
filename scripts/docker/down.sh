#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../common/colors.sh"
source "${SCRIPT_DIR}/../common/config.sh"
source "${SCRIPT_DIR}/../common/helpers.sh"

print_help() {
  cat << EOF
${COLOR_CYAN}${COLOR_BOLD}Docker Compose Down - Stop All Containers${COLOR_RESET}

${COLOR_BOLD}Usage:${COLOR_RESET}
  $(basename "$0") [OPTION]

${COLOR_BOLD}Options:${COLOR_RESET}
  --volumes, -v   Remove volumes (PostgreSQL & Redis data)
  --keep-volumes  Keep volumes (default)
  --remove-orphans Remove orphan containers

${COLOR_BOLD}Warning:${COLOR_RESET}
  Using --volumes will DELETE database and cache data!

${COLOR_BOLD}Example:${COLOR_RESET}
  ./docker/down.sh                    # Stop containers, keep data
  ./docker/down.sh --volumes          # Stop and DELETE all data
  COMPOSE_ENV=prod ./docker/down.sh
EOF
}

if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
  print_help
  exit 0
fi

print_header "Docker Compose Down"

# Parse arguments
VOLUMES_FLAG=""
ORPHANS_FLAG=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --volumes|-v)
      log_warn "WARNING: This will DELETE database and cache data!"
      read -p "Are you sure? (yes/no): " confirm
      if [[ "${confirm}" != "yes" ]]; then
        log_info "Cancelled"
        exit 0
      fi
      VOLUMES_FLAG="-v"
      ;;
    --keep-volumes) VOLUMES_FLAG="" ;;
    --remove-orphans) ORPHANS_FLAG="--remove-orphans" ;;
    --help|-h) print_help; exit 0 ;;
  esac
  shift
done

print_section "Configuration"
echo -e "  Environment:  ${COLOR_GREEN}${COMPOSE_ENV}${COLOR_RESET}"
echo -e "  Remove Data:  ${COLOR_YELLOW}${VOLUMES_FLAG:- No}${COLOR_RESET}"
echo ""

print_section "Stopping Containers"
log_info "Running: docker compose ${COMPOSE_FILES} down ${VOLUMES_FLAG} ${ORPHANS_FLAG}"

docker compose ${COMPOSE_FILES} down ${VOLUMES_FLAG} ${ORPHANS_FLAG}

log_success "All containers stopped"

if [[ -z "${VOLUMES_FLAG}" ]]; then
  echo -e "  ${COLOR_CYAN}Data volumes preserved${COLOR_RESET}"
fi

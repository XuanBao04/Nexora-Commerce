#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../common/colors.sh"
source "${SCRIPT_DIR}/../common/config.sh"
source "${SCRIPT_DIR}/../common/helpers.sh"

print_help() {
  cat << EOF
${COLOR_CYAN}${COLOR_BOLD}Backend Development - Run Local${COLOR_RESET}

${COLOR_BOLD}Usage:${COLOR_RESET}
  $(basename "$0")

${COLOR_BOLD}Description:${COLOR_RESET}
  Start Spring Boot backend in development mode with hot reload.
  Requires Maven 3.9+ and Java 21+

${COLOR_BOLD}Prerequisites:${COLOR_RESET}
  - Docker container for PostgreSQL (db)
  - Docker container for Redis (redis)
  - Maven installed locally
  - JDK 21 installed

${COLOR_BOLD}Environment:${COLOR_RESET}
  Set COMPOSE_ENV=prod to use production database

${COLOR_BOLD}Example:${COLOR_RESET}
  COMPOSE_ENV=dev ./backend/dev.sh
EOF
}

if [[ "${1:-}" == "--help" ]] || [[ "${1:-}" == "-h" ]]; then
  print_help
  exit 0
fi

print_header "Backend Development Mode"

# Ensure prerequisites
check_prerequisites

# Start database and redis
print_section "Starting infrastructure (db, redis)"
docker compose ${COMPOSE_FILES} up -d ${DB_SERVICE} ${REDIS_SERVICE}

wait_for_service "${DB_SERVICE}" 30 || {
  log_error "Database failed to start"
  exit 1
}

wait_for_service "${REDIS_SERVICE}" 30 || {
  log_error "Redis failed to start"
  exit 1
}

log_success "Infrastructure ready"

# Load environment
load_env

# Display connection info
print_section "Connection Info"
echo -e "  Database:  ${COLOR_GREEN}postgres://${POSTGRES_USER}@localhost:${POSTGRES_PORT}/${POSTGRES_DB}${COLOR_RESET}"
echo -e "  Redis:     ${COLOR_GREEN}redis://localhost:${REDIS_PORT}${COLOR_RESET}"
echo -e "  Backend:   ${COLOR_GREEN}http://localhost:${SERVER_PORT}${COLOR_RESET}"
echo -e "  Swagger:   ${COLOR_GREEN}http://localhost:${SERVER_PORT}/api/swagger-ui.html${COLOR_RESET}"
echo ""

# Run Spring Boot dev with Maven
print_section "Starting Backend (Spring Boot)"
log_info "Running: mvn spring-boot:run -Dspring-boot.run.arguments='--spring.profiles.active=dev'"

cd "${BACKEND_DIR}"
mvn clean spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev" \
  -Dspring-boot.run.jvmArguments="-Xmx1024m"

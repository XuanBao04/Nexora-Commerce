.PHONY: help \
	fe-dev fe-build fe-docker-build fe-logs fe-stop \
	be-dev be-build be-docker-build be-logs be-stop \
	docker-build docker-up docker-down docker-clean docker-logs \
	start stop restart build logs

SHELL := /bin/bash

# Load environment variables from .env
ifneq (,$(wildcard .env))
    include .env
    export
endif

# Colors
BLUE := \033[1;34m
GREEN := \033[1;32m
CYAN := \033[1;36m
YELLOW := \033[1;33m
RESET := \033[0m

# Default target
.DEFAULT_GOAL := help

help: ## Show this help
	@echo -e "$(CYAN)╔════════════════════════════════════════════════════╗$(RESET)"
	@echo -e "$(CYAN)║  Nexora Commerce - Development Scripts             ║$(RESET)"
	@echo -e "$(CYAN)╚════════════════════════════════════════════════════╝$(RESET)"
	@echo ""
	@echo -e "$(YELLOW)FRONTEND:$(RESET)"
	@grep "^fe-" $(MAKEFILE_LIST) | sed 's/^.*:.*##/  /' | sed 's/fe-//' 
	@echo ""
	@echo -e "$(YELLOW)BACKEND:$(RESET)"
	@grep "^be-" $(MAKEFILE_LIST) | sed 's/^.*:.*##/  /' | sed 's/be-//'
	@echo ""
	@echo -e "$(YELLOW)DOCKER:$(RESET)"
	@grep "^docker-" $(MAKEFILE_LIST) | sed 's/^.*:.*##/  /' | sed 's/docker-//'
	@echo ""
	@echo -e "$(YELLOW)FULLSTACK (QUICK):$(RESET)"
	@grep "^[a-z]*:.*##" $(MAKEFILE_LIST) | grep -v "^fe-\|^be-\|^docker-" | sed 's/:.*##/  /' 
	@echo ""

# ============================================================================
# FRONTEND TARGETS
# ============================================================================

fe-dev: ## Frontend: Run development server locally
	@bash scripts/frontend/dev.sh

fe-build: ## Frontend: Build dist for production
	@bash scripts/frontend/build.sh

fe-docker-build: ## Frontend: Build Docker image
	@bash scripts/frontend/docker-build.sh

fe-logs: ## Frontend: View container logs
	@bash scripts/frontend/logs.sh

fe-stop: ## Frontend: Stop container
	@bash scripts/frontend/stop.sh

# ============================================================================
# BACKEND TARGETS
# ============================================================================

be-dev: ## Backend: Run development server locally
	@bash scripts/backend/dev.sh

be-build: ## Backend: Build JAR package
	@bash scripts/backend/build.sh

be-docker-build: ## Backend: Build Docker image
	@bash scripts/backend/docker-build.sh

be-logs: ## Backend: View container logs
	@bash scripts/backend/logs.sh

be-stop: ## Backend: Stop container
	@bash scripts/backend/stop.sh

# ============================================================================
# DOCKER TARGETS
# ============================================================================

docker-build: ## Docker: Build all images (frontend + backend)
	@bash scripts/docker/build.sh --all

docker-up: ## Docker: Start all containers
	@bash scripts/docker/up.sh

docker-down: ## Docker: Stop all containers
	@bash scripts/docker/down.sh

docker-clean: ## Docker: Remove unused images & volumes
	@bash scripts/docker/clean.sh --all

docker-logs: ## Docker: View system logs (all services)
	@bash scripts/docker/logs.sh all

# ============================================================================
# FULLSTACK SHORTCUTS (RECOMMENDED)
# ============================================================================

build: ## Build: Frontend + Backend (local + Docker images)
	@bash scripts/fullstack/build.sh --docker

start: ## Start: Begin all services (containers)
	@bash scripts/fullstack/start.sh

stop: ## Stop: Halt all services
	@bash scripts/fullstack/stop.sh

restart: ## Restart: Stop & start all services
	@bash scripts/fullstack/restart.sh

logs: ## Logs: View all service logs
	@bash scripts/fullstack/logs.sh all

status: ## Status: Show running containers
	@docker compose -f docker/docker-compose.yml -f docker/docker-compose.dev.yml ps

clean: ## Clean: Full system cleanup (containers + images + volumes)
	@bash scripts/docker/down.sh --volumes
	@bash scripts/docker/clean.sh --prune

# ============================================================================
# DEVELOPMENT CONVENIENCE
# ============================================================================

dev-all: ## Dev: Start DB + Redis, then dev servers (frontend + backend)
	@echo -e "$(BLUE)Starting Infrastructure...$(RESET)"
	@docker compose -f docker/docker-compose.yml -f docker/docker-compose.dev.yml up -d db redis
	@echo ""
	@echo -e "$(GREEN)✓ Infrastructure ready$(RESET)"
	@echo ""
	@echo -e "$(CYAN)Next: Run in separate terminals:$(RESET)"
	@echo -e "  Terminal 1: $(GREEN)make be-dev$(RESET)    # Backend on :8080"
	@echo -e "  Terminal 2: $(GREEN)make fe-dev$(RESET)    # Frontend on :5173"

info: ## Info: Show configuration & URLs
	@echo ""
	@echo -e "$(CYAN)Project Configuration:$(RESET)"
	@echo -e "  Frontend:  $(GREEN)http://localhost:5173$(RESET)"
	@echo -e "  Backend:   $(GREEN)http://localhost:8080$(RESET)"
	@echo -e "  Swagger:   $(GREEN)http://localhost:8080/swagger-ui.html$(RESET)"
	@echo -e "  Database:  $(GREEN)postgres://localhost:5432$(RESET)"
	@echo -e "  Redis:     $(GREEN)redis://localhost:6379$(RESET)"
	@echo ""

# ============================================================================
# PRODUCTION
# ============================================================================

prod-build: ## Prod: Build all for production
	@COMPOSE_ENV=prod bash scripts/fullstack/build.sh --docker --skip-tests

prod-start: ## Prod: Start production stack
	@COMPOSE_ENV=prod bash scripts/fullstack/start.sh

prod-stop: ## Prod: Stop production stack
	@COMPOSE_ENV=prod bash scripts/fullstack/stop.sh

prod-logs: ## Prod: View production logs
	@COMPOSE_ENV=prod bash scripts/fullstack/logs.sh all

# ============================================================================
# UTILITIES
# ============================================================================

shell-backend: ## Util: Shell into backend container
	@docker compose -f docker/docker-compose.yml -f docker/docker-compose.dev.yml exec backend sh

shell-frontend: ## Util: Shell into frontend container
	@docker compose -f docker/docker-compose.yml -f docker/docker-compose.dev.yml exec frontend sh

shell-db: ## Util: Connect to PostgreSQL database
	@docker compose -f docker/docker-compose.yml -f docker/docker-compose.dev.yml exec db psql -U $${POSTGRES_USER} -d $${POSTGRES_DB}

redis-cli: ## Util: Connect to Redis
	@docker compose -f docker/docker-compose.yml -f docker/docker-compose.dev.yml exec redis redis-cli -a $${REDIS_PASSWORD}

test-backend: ## Test: Run backend tests
	@cd backend && mvn test

test-frontend: ## Test: Run frontend tests
	@cd frontend && pnpm test

lint-frontend: ## Lint: Run frontend linter
	@cd frontend && pnpm lint

format-frontend: ## Format: Format frontend code
	@cd frontend && pnpm format

.SILENT: help

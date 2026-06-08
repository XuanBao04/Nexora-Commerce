.PHONY: help \
	fe-dev fe-build fe-docker-build fe-logs fe-stop \
	be-dev be-build be-docker-build be-logs be-stop \
	docker-build docker-up docker-down docker-clean docker-logs \
	start stop restart build logs

SHELL := /bin/bash

# Nạp các biến môi trường từ file .env
ifneq (,$(wildcard .env))
    include .env
    export
endif

# Định nghĩa bảng màu sắc
BLUE := \033[1;34m
GREEN := \033[1;32m
CYAN := \033[1;36m
YELLOW := \033[1;33m
RESET := \033[0m

# Mục tiêu mặc định
.DEFAULT_GOAL := help

help: ## Hiển thị hướng dẫn sử dụng chi tiết
	@echo -e "$(CYAN)╔════════════════════════════════════════════════════╗$(RESET)"
	@echo -e "$(CYAN)║  Nexora Commerce - Các Lệnh Hỗ Trợ Phát Triển     ║$(RESET)"
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
	@echo -e "$(YELLOW)FULLSTACK (NHANH):$(RESET)"
	@grep "^[a-z]*:.*##" $(MAKEFILE_LIST) | grep -v "^fe-\|^be-\|^docker-" | sed 's/:.*##/  /' 
	@echo ""

# ============================================================================
# CÁC LỆNH CHO FRONTEND
# ============================================================================

fe-dev: ## Chạy dev server cho frontend ở máy cục bộ
	@bash scripts/frontend/dev.sh

fe-build: ## Biên dịch (build) thư mục dist cho production
	@bash scripts/frontend/build.sh

fe-docker-build: ## Build Docker image cho frontend
	@bash scripts/frontend/docker-build.sh

fe-logs: ## Xem logs của container frontend
	@bash scripts/frontend/logs.sh

fe-stop: ## Dừng container frontend
	@bash scripts/frontend/stop.sh

# ============================================================================
# CÁC LỆNH CHO BACKEND
# ============================================================================

be-dev: ## Chạy dev server cho backend ở máy cục bộ
	@bash scripts/backend/dev.sh

be-build: ## Biên dịch (build) file JAR cho backend
	@bash scripts/backend/build.sh

be-docker-build: ## Build Docker image cho backend
	@bash scripts/backend/docker-build.sh

be-logs: ## Xem logs của container backend
	@bash scripts/backend/logs.sh

be-stop: ## Dừng container backend
	@bash scripts/backend/stop.sh

# ============================================================================
# CÁC LỆNH DOCKER CHUNG
# ============================================================================

docker-build: ## Build tất cả các Docker image (frontend + backend)
	@bash scripts/docker/build.sh --all

docker-up: ## Khởi chạy tất cả các container dịch vụ
	@bash scripts/docker/up.sh

docker-down: ## Dừng và gỡ bỏ tất cả các container dịch vụ
	@bash scripts/docker/down.sh

docker-clean: ## Xóa bỏ các Docker image và volume không sử dụng
	@bash scripts/docker/clean.sh --all

docker-logs: ## Xem logs hệ thống của tất cả dịch vụ
	@bash scripts/docker/logs.sh all

# ============================================================================
# LỆNH RÚT GỌN CHO CẢ HỆ THỐNG FULLSTACK (KHUYÊN DÙNG)
# ============================================================================

build: ## Build toàn bộ Frontend + Backend (cục bộ + Docker images)
	@bash scripts/fullstack/build.sh --docker

start: ## Khởi chạy toàn bộ hệ thống các dịch vụ (container)
	@bash scripts/fullstack/start.sh

stop: ## Dừng toàn bộ hệ thống các dịch vụ
	@bash scripts/fullstack/stop.sh

restart: ## Khởi động lại toàn bộ hệ thống các dịch vụ
	@bash scripts/fullstack/restart.sh

logs: ## Xem logs của tất cả dịch vụ đang chạy
	@bash scripts/fullstack/logs.sh all

status: ## Hiển thị danh sách các container đang chạy và trạng thái của chúng
	@docker compose -f docker/docker-compose.yml -f docker/docker-compose.dev.yml ps

clean: ## Dọn dẹp sạch sẽ hệ thống (gồm container, images, volumes)
	@bash scripts/docker/down.sh --volumes
	@bash scripts/docker/clean.sh --prune

# ============================================================================
# TIỆN ÍCH PHÁT TRIỂN (CONVENIENCE)
# ============================================================================

dev-all: ## Khởi động DB + Redis trước, sau đó chỉ dẫn chạy dev server
	@echo -e "$(BLUE)Starting Infrastructure...$(RESET)"
	@docker compose -f docker/docker-compose.yml -f docker/docker-compose.dev.yml up -d db redis
	@echo ""
	@echo -e "$(GREEN)✓ Infrastructure ready$(RESET)"
	@echo ""
	@echo -e "$(CYAN)Next: Run in separate terminals:$(RESET)"
	@echo -e "  Terminal 1: $(GREEN)make be-dev$(RESET)    # Backend on :8080"
	@echo -e "  Terminal 2: $(GREEN)make fe-dev$(RESET)    # Frontend on :5173"

info: ## Hiển thị thông tin cấu hình cổng chạy và các địa chỉ URL dự án
	@echo ""
	@echo -e "$(CYAN)Project Configuration:$(RESET)"
	@echo -e "  Frontend:  $(GREEN)http://localhost:5173$(RESET)"
	@echo -e "  Backend:   $(GREEN)http://localhost:8080$(RESET)"
	@echo -e "  Swagger:   $(GREEN)http://localhost:8080/api/swagger-ui.html$(RESET)"
	@echo -e "  Database:  $(GREEN)postgres://localhost:5432$(RESET)"
	@echo -e "  Redis:     $(GREEN)redis://localhost:6379$(RESET)"
	@echo ""

# ============================================================================
# MÔI TRƯỜNG PRODUCTION
# ============================================================================

prod-build: ## Build toàn bộ hệ thống cho môi trường production
	@COMPOSE_ENV=prod bash scripts/fullstack/build.sh --docker --skip-tests

prod-start: ## Khởi chạy toàn bộ hệ thống ở môi trường production
	@COMPOSE_ENV=prod bash scripts/fullstack/start.sh

prod-stop: ## Dừng hoạt động toàn bộ hệ thống ở môi trường production
	@COMPOSE_ENV=prod bash scripts/fullstack/stop.sh

prod-logs: ## Xem logs hệ thống ở môi trường production
	@COMPOSE_ENV=prod bash scripts/fullstack/logs.sh all

# ============================================================================
# TIỆN ÍCH HỆ THỐNG
# ============================================================================

shell-backend: ## Truy cập terminal bên trong container backend
	@docker compose -f docker/docker-compose.yml -f docker/docker-compose.dev.yml exec backend sh

shell-frontend: ## Truy cập terminal bên trong container frontend
	@docker compose -f docker/docker-compose.yml -f docker/docker-compose.dev.yml exec frontend sh

shell-db: ## Truy cập trực tiếp dòng lệnh PostgreSQL của cơ sở dữ liệu
	@docker compose -f docker/docker-compose.yml -f docker/docker-compose.dev.yml exec db psql -U $${POSTGRES_USER} -d $${POSTGRES_DB}

redis-cli: ## Truy cập trực tiếp dòng lệnh Redis CLI
	@docker compose -f docker/docker-compose.yml -f docker/docker-compose.dev.yml exec redis redis-cli -a $${REDIS_PASSWORD}

test-backend: ## Chạy bộ kiểm thử (unit tests) của backend
	@cd backend && mvn test

test-frontend: ## Chạy bộ kiểm thử (unit tests) của frontend
	@cd frontend && pnpm test

lint-frontend: ## Kiểm tra lỗi cú pháp và định dạng frontend (Linter)
	@cd frontend && pnpm lint

format-frontend: ## Tự động định dạng code frontend (Prettier)
	@cd frontend && pnpm format

.SILENT: help

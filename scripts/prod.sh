#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Nexora Commerce - Production Stack Launcher CLI
# Supports running individual services and restarting them in production mode.
# ==============================================================================

COMPOSE_FILES="-f docker-compose.yml -f docker-compose.prod.yml"

print_help() {
  echo -e "\033[1;35mNexora Commerce Production CLI Utility\033[0m"
  echo -e "----------------------------------------"
  echo -e "Sử dụng:"
  echo -e "  \033[1;32m./prod.sh\033[0m                   Khởi chạy toàn bộ production stack (Frontend, Backend, DB, Redis)"
  echo -e "  \033[1;32m./prod.sh --be\033[0m              Khởi chạy riêng lẻ Backend trong Production (kèm DB & Redis)"
  echo -e "  \033[1;32m./prod.sh --fe\033[0m              Khởi chạy riêng lẻ Frontend trong Production"
  echo -e "  \033[1;32m./prod.sh --restart be\033[0m      Khởi động lại Backend production container"
  echo -e "  \033[1;32m./prod.sh --restart fe\033[0m      Khởi động lại Frontend production container"
  echo -e "  \033[1;32m./prod.sh --help | -h\033[0m       Hiển thị hướng dẫn này"
  echo -e "----------------------------------------"
}

wait_for_backend() {
  echo -e "\033[1;33m[prod]\033[0m Đang kiểm tra sức khỏe dịch vụ backend..."
  for i in {1..30}; do
    if docker compose ${COMPOSE_FILES} exec -T backend curl -fsS http://localhost:8080/actuator/health >/dev/null 2>&1; then
      echo -e "\033[1;32m[prod]\033[0m Dịch vụ Backend đã KHỎE MẠNH (Healthy)!"
      return 0
    fi
    echo -e "\033[1;30m[prod] Healthcheck chưa sẵn sàng ($i/30), thử lại sau 2 giây...\033[0m"
    sleep 2
  done
  echo -e "\033[1;31m[Lỗi] Backend healthcheck thất bại!\033[0m"
  return 1
}

# Hỗ trợ restart service trong production
if [[ $# -eq 2 && "$1" == "--restart" ]]; then
  SERVICE=""
  case "$2" in
    fe|frontend)
      SERVICE="frontend"
      ;;
    be|backend)
      SERVICE="backend"
      ;;
    *)
      echo -e "\033[1;31m[Lỗi]\033[0m Dịch vụ không xác định: '$2'. Vui lòng chọn 'fe' hoặc 'be'."
      exit 1
      ;;
  esac

  echo -e "\033[1;33m[prod]\033[0m Đang khởi động lại dịch vụ Production \033[1;32m${SERVICE}\033[0m..."
  docker compose ${COMPOSE_FILES} restart "${SERVICE}"
  
  if [[ "${SERVICE}" == "backend" ]]; then
    wait_for_backend
  fi

# Hỗ trợ chạy đơn lẻ từng service trong production
elif [[ $# -eq 1 ]]; then
  case "$1" in
    --be)
      echo -e "\033[1;33m[prod]\033[0m Đang dọn dẹp các service cũ (nếu có)..."
      docker compose ${COMPOSE_FILES} down backend || true
      
      echo -e "\033[1;33m[prod]\033[0m Đang build image mới cho \033[1;32mBackend\033[0m (no-cache)..."
      docker compose ${COMPOSE_FILES} build --no-cache backend
      
      echo -e "\033[1;33m[prod]\033[0m Đang khởi chạy dịch vụ Production \033[1;32mBackend\033[0m..."
      docker compose ${COMPOSE_FILES} up -d backend
      
      wait_for_backend
      ;;
    --fe)
      echo -e "\033[1;33m[prod]\033[0m Đang dọn dẹp các service cũ (nếu có)..."
      docker compose ${COMPOSE_FILES} down frontend || true
      
      echo -e "\033[1;33m[prod]\033[0m Đang build image mới cho \033[1;32mFrontend\033[0m (no-cache)..."
      docker compose ${COMPOSE_FILES} build --no-cache frontend
      
      echo -e "\033[1;33m[prod]\033[0m Đang khởi chạy dịch vụ Production \033[1;32mFrontend\033[0m..."
      docker compose ${COMPOSE_FILES} up -d frontend
      ;;
    --help|-h)
      print_help
      exit 0
      ;;
    *)
      echo -e "\033[1;31m[Lỗi]\033[0m Tham số không hợp lệ: '$1'."
      print_help
      exit 1
      ;;
  esac

# Mặc định: Dừng, build và chạy toàn bộ stack trong production
elif [[ $# -eq 0 ]]; then
  echo -e "\033[1;33m[prod]\033[0m Đang dừng toàn bộ các service production cũ (nếu có)..."
  docker compose ${COMPOSE_FILES} down
  
  echo -e "\033[1;33m[prod]\033[0m Đang build mới toàn bộ các image production (no-cache)..."
  docker compose ${COMPOSE_FILES} build --no-cache
  
  echo -e "\033[1;33m[prod]\033[0m Đang khởi chạy \033[1;35mtoàn bộ Production Stack\033[0m..."
  docker compose ${COMPOSE_FILES} up -d
  
  wait_for_backend
  
else
  echo -e "\033[1;31m[Lỗi]\033[0m Quá nhiều hoặc sai tham số đầu vào."
  print_help
  exit 1
fi

echo ""
echo -e "\033[1;34m[prod] Trạng thái các container production hiện tại:\033[0m"
docker compose ${COMPOSE_FILES} ps

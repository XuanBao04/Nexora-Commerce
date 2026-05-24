#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Nexora Commerce - Development Stack Launcher CLI
# Supports running individual services and restarting them.
# ==============================================================================

COMPOSE_FILES="-f docker-compose.yml -f docker-compose.dev.yml"

print_help() {
  echo -e "\033[1;36mNexora Commerce Dev CLI Utility\033[0m"
  echo -e "----------------------------------------"
  echo -e "Sử dụng:"
  echo -e "  \033[1;32m./dev.sh\033[0m                    Khởi chạy toàn bộ stack (Frontend, Backend, DB, Redis)"
  echo -e "  \033[1;32m./dev.sh --be\033[0m               Khởi chạy riêng lẻ Backend (kèm DB & Redis)"
  echo -e "  \033[1;32m./dev.sh --fe\033[0m               Khởi chạy riêng lẻ Frontend (kèm Backend & DB & Redis)"
  echo -e "  \033[1;32m./dev.sh --restart be\033[0m       Khởi động lại Backend container"
  echo -e "  \033[1;32m./dev.sh --restart fe\033[0m       Khởi động lại Frontend container"
  echo -e "  \033[1;32m./dev.sh --help | -h\033[0m        Hiển thị hướng dẫn này"
  echo -e "----------------------------------------"
}

# Hỗ trợ restart service
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

  echo -e "\033[1;33m[dev]\033[0m Đang khởi động lại dịch vụ \033[1;32m${SERVICE}\033[0m..."
  docker compose ${COMPOSE_FILES} restart "${SERVICE}"
  
# Hỗ trợ chạy đơn lẻ từng service
elif [[ $# -eq 1 ]]; then
  case "$1" in
    --be)
      echo -e "\033[1;33m[dev]\033[0m Đang khởi chạy riêng lẻ dịch vụ \033[1;32mBackend\033[0m (bao gồm DB & Redis)..."
      docker compose ${COMPOSE_FILES} up --build -d --wait backend
      ;;
    --fe)
      echo -e "\033[1;33m[dev]\033[0m Đang khởi chạy dịch vụ \033[1;32mFrontend\033[0m..."
      docker compose ${COMPOSE_FILES} up --build -d --wait frontend
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

# Mặc định: Chạy toàn bộ stack
elif [[ $# -eq 0 ]]; then
  echo -e "\033[1;33m[dev]\033[0m Đang khởi chạy \033[1;36mtoàn bộ Development Stack\033[0m..."
  docker compose ${COMPOSE_FILES} up --build -d --wait
  
else
  echo -e "\033[1;31m[Lỗi]\033[0m Quá nhiều hoặc sai tham số đầu vào."
  print_help
  exit 1
fi

echo ""
echo -e "\033[1;34m[dev] Trạng thái các container hiện tại:\033[0m"
docker compose ${COMPOSE_FILES} ps

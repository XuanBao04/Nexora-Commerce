#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Nexora Commerce - Stack Tear Down CLI
# Supports tearing down the entire stack or individual service containers.
# ==============================================================================

COMPOSE_FILES="-f docker-compose.yml -f docker-compose.dev.yml -f docker-compose.prod.yml"

print_help() {
  echo -e "\033[1;36mNexora Commerce Down CLI Utility\033[0m"
  echo -e "----------------------------------------"
  echo -e "Sử dụng:"
  echo -e "  \033[1;32m./down.sh\033[0m                    Dừng toàn bộ stack và giữ nguyên volumes"
  echo -e "  \033[1;32m./down.sh --clean\033[0m            Dừng toàn bộ stack và XÓA SẠCH volumes (PostgreSQL & Redis data)"
  echo -e "  \033[1;32m./down.sh --be\033[0m               Dừng và xóa riêng container Backend"
  echo -e "  \033[1;32m./down.sh --fe\033[0m               Dừng và xóa riêng container Frontend"
  echo -e "  \033[1;32m./down.sh --help | -h\033[0m        Hiển thị hướng dẫn này"
  echo -e "----------------------------------------"
}

if [[ $# -gt 1 ]]; then
  echo -e "\033[1;31m[Lỗi]\033[0m Quá nhiều tham số. Chỉ truyền tối đa 1 tham số."
  print_help
  exit 1
fi

if [[ $# -eq 0 ]]; then
  echo -e "\033[1;33m[down]\033[0m Đang dừng toàn bộ Development/Production Stack..."
  docker compose ${COMPOSE_FILES} down
elif [[ $# -eq 1 ]]; then
  case "$1" in
    --clean)
      echo -e "\033[1;31m[down]\033[0m Đang dừng toàn bộ Stack và \033[1;37;41mXÓA SẠCH DỮ LIỆU VOLUMES\033[0m..."
      docker compose ${COMPOSE_FILES} down -v
      ;;
    --be)
      echo -e "\033[1;33m[down]\033[0m Đang dừng và loại bỏ container \033[1;32mBackend\033[0m..."
      docker compose ${COMPOSE_FILES} rm -fs backend
      ;;
    --fe)
      echo -e "\033[1;33m[down]\033[0m Đang dừng và loại bỏ container \033[1;32mFrontend\033[0m..."
      docker compose ${COMPOSE_FILES} rm -fs frontend
      ;;
    --help|-h)
      print_help
      exit 0
      ;;
    *)
      echo -e "\033[1;31m[Lỗi]\033[0m Tham số không xác định: '$1'."
      print_help
      exit 1
      ;;
  esac
fi

echo -e "\033[1;32m[down]\033[0m Hoàn tất!"

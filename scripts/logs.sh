#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Nexora Commerce - Development Stack Logs CLI
# Supports tracking logs for the entire stack or individual services with aliases.
# ==============================================================================

COMPOSE_FILES="-f docker/docker-compose.yml -f docker/docker-compose.dev.yml"
TARGET_SERVICE=""

print_help() {
  echo -e "\033[1;36mNexora Commerce Logs CLI Utility\033[0m"
  echo -e "----------------------------------------"
  echo -e "Sử dụng:"
  echo -e "  \033[1;32m./logs.sh\033[0m                    Xem và theo dõi logs của toàn bộ stack"
  echo -e "  \033[1;32m./logs.sh be\033[0m                 Theo dõi logs của Backend"
  echo -e "  \033[1;32m./logs.sh fe\033[0m                 Theo dõi logs của Frontend"
  echo -e "  \033[1;32m./logs.sh db\033[0m                 Theo dõi logs của Database (PostgreSQL)"
  echo -e "  \033[1;32m./logs.sh redis\033[0m              Theo dõi logs của Redis"
  echo -e "  \033[1;32m./logs.sh --help | -h\033[0m        Hiển thị hướng dẫn này"
  echo -e "----------------------------------------"
}

if [[ $# -gt 1 ]]; then
  echo -e "\033[1;31m[Lỗi]\033[0m Quá nhiều tham số. Chỉ truyền tối đa 1 tên dịch vụ hoặc --help."
  print_help
  exit 1
fi

if [[ $# -eq 1 ]]; then
  case "$1" in
    be|backend)
      TARGET_SERVICE="backend"
      ;;
    fe|frontend)
      TARGET_SERVICE="frontend"
      ;;
    db|database|postgres)
      TARGET_SERVICE="db"
      ;;
    redis)
      TARGET_SERVICE="redis"
      ;;
    --help|-h)
      print_help
      exit 0
      ;;
    *)
      echo -e "\033[1;31m[Lỗi]\033[0m Dịch vụ không xác định: '$1'."
      print_help
      exit 1
      ;;
  esac
fi

if [[ -n "$TARGET_SERVICE" ]]; then
  echo -e "\033[1;33m[logs]\033[0m Đang theo dõi logs của dịch vụ \033[1;32m${TARGET_SERVICE}\033[0m (nhấn Ctrl+C để thoát)..."
  docker compose ${COMPOSE_FILES} logs -f "${TARGET_SERVICE}"
else
  echo -e "\033[1;33m[logs]\033[0m Đang theo dõi logs của \033[1;36mtoàn bộ Development Stack\033[0m (nhấn Ctrl+C để thoát)..."
  docker compose ${COMPOSE_FILES} logs -f
fi

#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Nexora Commerce - Redis CLI Wrapper
# This script provides a convenient way to interact with the Redis instance.
# ==============================================================================

# Load environment variables safely
if [ -f .env ]; then
    # Extract only Redis related variables to avoid parsing errors with complex strings in .env
    REDIS_HOST=$(grep "^REDIS_HOST=" .env | cut -d'=' -f2 | tr -d '\r')
    REDIS_PORT=$(grep "^REDIS_PORT=" .env | cut -d'=' -f2 | tr -d '\r')
    REDIS_PASSWORD=$(grep "^REDIS_PASSWORD=" .env | cut -d'=' -f2 | tr -d '\r')
fi

REDIS_HOST=${REDIS_HOST:-localhost}
REDIS_PORT=${REDIS_PORT:-6379}
REDIS_PASSWORD=${REDIS_PASSWORD:-redis123}

# Instructions/Help section (-h flag)
print_help() {
  echo -e "\033[1;36mNexora Commerce Redis CLI Utility\033[0m"
  echo -e "----------------------------------------"
  echo -e "Sử dụng:"
  echo -e "  \033[1;32m./scripts/redis-cli.sh\033[0m          Mở Redis CLI (Interactive Mode)"
  echo -e "  \033[1;32m./scripts/redis-cli.sh <lệnh>\033[0m   Chạy một lệnh Redis cụ thể"
  echo -e "  \033[1;32m./scripts/redis-cli.sh -h\033[0m       Hiển thị hướng dẫn này"
  echo -e ""
  echo -e "Các lệnh phổ biến:"
  echo -e "  \033[1;33mPING\033[0m                     Kiểm tra kết nối"
  echo -e "  \033[1;33mINFO\033[0m                     Xem thông tin server"
  echo -e "  \033[1;33mKEYS *\033[0m                   Liệt kê tất cả các key"
  echo -e "  \033[1;33mGET <key>\033[0m                Lấy giá trị của key"
  echo -e "  \033[1;33mFLUSHALL\033[0m                 Xóa sạch dữ liệu"
  echo -e "----------------------------------------"
}

# Check for -h or --help
if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
    print_help
    exit 0
fi

# If localhost, try to run inside docker container if redis-cli is not found on host
if [[ "$REDIS_HOST" == "localhost" || "$REDIS_HOST" == "127.0.0.1" ]]; then
    if ! command -v redis-cli &> /dev/null; then
        echo -e "\033[1;33m[Thông báo]\033[0m redis-cli không tìm thấy trên máy local. Đang thử kết nối qua Docker..."
        docker exec -it nexora-commerce-redis-1 redis-cli -a "$REDIS_PASSWORD" "$@"
        exit $?
    fi
fi

# Note: Using -4 to force IPv4 (as requested with -h4)
# Note: Using -h for host, -p for port, -a for password
echo "Kết nối tới Redis tại $REDIS_HOST:$REDIS_PORT (IPv4)..."

# If password is set, use -a. Be aware that -a puts password in process list.
# For security, one could use REDISCLI_AUTH environment variable.
export REDISCLI_AUTH="$REDIS_PASSWORD"

redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -4 "$@"

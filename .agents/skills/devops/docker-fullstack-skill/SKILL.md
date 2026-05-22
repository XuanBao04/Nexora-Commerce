---
name: docker-fullstack-skill
description: Enforce multi-stage Docker builds, security hardening, health checks, volume management, and production-ready container orchestration for Spring Boot + React applications.
---

## Scope & Activation Rules

Activate when:
- Creating or updating Dockerfile for backend (Spring Boot) or frontend (React)
- Configuring docker-compose for local development or staging
- Implementing health checks, restart policies, and logging
- Setting up networking, volumes, and environment variables
- Optimizing image size and build times
- Hardening container security (non-root user, minimal base images)
- Deploying to production with Kubernetes or Docker Swarm

## System Directives

### DO
- **Use Multi-Stage Builds**: Separate build stage (compile/bundle) from runtime stage (minimal dependencies).
- **Use Minimal Base Images**: `eclipse-temurin:21-jre-alpine` for Java, `node:20-alpine` for Node.js; never use `ubuntu:latest` or `centos:latest`.
- **Run as Non-Root User**: Create unprivileged user in Dockerfile; never run containers as root.
- **Define Health Checks**: Use `HEALTHCHECK` instruction or `healthcheck:` in docker-compose; monitor startup and liveness.
- **Implement Proper Logging**: Log to stdout/stderr; use container log drivers for aggregation.
- **Use Explicit Base Image Tags**: Never use `latest`. Pin to specific versions: `node:20.11.0-alpine`, `eclipse-temurin:21.0.1_12-jre-alpine`.
- **Leverage Layer Caching**: Order Dockerfile commands from least frequently changed to most frequently changed.
- **Set Resource Limits**: Define `memory`, `cpus` in docker-compose; prevent resource exhaustion.
- **Use Named Volumes**: For persistent data; avoid `docker run -v /some/path` (host mounts).
- **Implement Graceful Shutdown**: Handle SIGTERM; allow 30 seconds for cleanup before SIGKILL.
- **Separate Configs**: Use `.env` files or environment variables; never hardcode credentials in Dockerfile.
- **Scan Images for Vulnerabilities**: Use `trivy`, `grype`, or registry scanning before deployment.

### DO NOT
- Use `latest` tag in production. Always specify exact versions for reproducibility.
- Run as root user inside containers. Create dedicated user for application.
- Omit health checks. Orchestrators need signals to restart unhealthy containers.
- Include development dependencies in production images (npm dev packages, Maven plugins).
- Use `ADD` from remote URLs. Use `COPY` from local context; download files in RUN steps.
- Mount host directories in production. Use volumes managed by container runtime.
- Commit sensitive data to Dockerfile (API keys, passwords, tokens). Use secrets management.
- Skip image scanning for vulnerabilities. Always scan before production deployment.
- Use `CMD` and `ENTRYPOINT` interchangeably without understanding the difference. Use ENTRYPOINT for command, CMD for arguments.
- Create Dockerfiles that are tens of lines when they could be 10 with multi-stage builds.
- Ignore image size. Oversized images slow down deployment and consume storage.
- Run multiple processes in one container (use one container per service; use docker-compose for orchestration).

## Production Reference Implementation

### Backend Dockerfile (Spring Boot Multi-Stage)

```dockerfile
# Stage 1: Build Maven application
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy pom.xml and download dependencies (leverages Docker layer cache)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime with minimal dependencies
FROM eclipse-temurin:21-jre-alpine

# Install curl for health checks (minimal footprint)
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -g 1000 appgroup && \
    adduser -D -u 1000 -G appgroup appuser

WORKDIR /app

# Copy compiled JAR from builder stage
COPY --from=builder --chown=appuser:appgroup /build/target/nexora-commerce-backend-1.0.0.jar app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run application with JVM optimizations for containers
ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-XX:InitiatingHeapOccupancyPercent=35", \
    "-XX:+UseStringDeduplication", \
    "-Xms256m", \
    "-Xmx512m", \
    "-Djava.awt.headless=true", \
    "-Dspring.profiles.active=prod", \
    "-jar", \
    "app.jar"]
```

### Frontend Dockerfile (React Multi-Stage)

```dockerfile
# Stage 1: Build React application
FROM node:20.11.0-alpine AS builder

WORKDIR /build

# Copy package files
COPY package.json pnpm-lock.yaml ./

# Install pnpm and dependencies
RUN npm install -g pnpm && pnpm install --frozen-lockfile

# Copy source code
COPY . .

# Build production bundle
RUN pnpm run build

# Stage 2: Serve with Nginx
FROM nginx:1.25-alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/nginx.conf

# Copy built application from builder
COPY --from=builder --chown=nginx:nginx /build/dist /usr/share/nginx/html

# Create non-root user (nginx already does this by default)
USER nginx

# Expose port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -f http://localhost/health || exit 1

# Start Nginx
ENTRYPOINT ["nginx", "-g", "daemon off;"]
```

### Nginx Configuration (nginx.conf)

```nginx
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /var/run/nginx.pid;

events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';

    access_log /var/log/nginx/access.log main;

    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;
    client_max_body_size 20M;

    # Enable gzip compression
    gzip on;
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/xml text/javascript 
               application/json application/javascript application/xml+rss 
               application/rss+xml application/atom+xml image/svg+xml 
               text/x-component text/x-cross-domain-policy;

    # Health check endpoint
    server {
        listen 80;
        server_name _;

        location /health {
            access_log off;
            return 200 "healthy\n";
            add_header Content-Type text/plain;
        }
    }

    # Main application server
    server {
        listen 80 default_server;
        server_name _;
        root /usr/share/nginx/html;
        index index.html index.htm;

        # Cache static assets (images, fonts, CSS, JS)
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
            access_log off;
        }

        # SPA routing: fallback to index.html for all routes
        location / {
            try_files $uri /index.html;
            add_header Cache-Control "no-cache, no-store, must-revalidate";
            expires 0;
        }

        # API reverse proxy
        location /api/ {
            proxy_pass http://backend:8080;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection 'upgrade';
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_cache_bypass $http_upgrade;
            proxy_connect_timeout 60s;
            proxy_send_timeout 60s;
            proxy_read_timeout 60s;
        }

        # Security headers
        add_header X-Frame-Options "SAMEORIGIN" always;
        add_header X-XSS-Protection "1; mode=block" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header Referrer-Policy "no-referrer-when-downgrade" always;
        add_header Content-Security-Policy "default-src 'self' https: data: 'unsafe-inline' 'unsafe-eval';" always;
    }
}
```

### Docker Compose for Local Development

```yaml
version: '3.9'

services:
  mysql:
    image: mysql:8.0.35-alpine
    container_name: nexora-mysql
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: nexora_db
      MYSQL_USER: nexora_user
      MYSQL_PASSWORD: nexora_password
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./backend/src/main/resources/db/migration:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-prootpassword"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - nexora_network

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile.dev
    container_name: nexora-backend
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/nexora_db
      SPRING_DATASOURCE_USERNAME: nexora_user
      SPRING_DATASOURCE_PASSWORD: nexora_password
      SPRING_JPA_HIBERNATE_DDL_AUTO: validate
      JWT_SECRET: ${JWT_SECRET:-dev-secret-key-change-in-production}
      SPRING_PROFILES_ACTIVE: dev
    ports:
      - "8080:8080"
    depends_on:
      mysql:
        condition: service_healthy
    volumes:
      - ./backend/src:/app/src
      - ./backend/target:/app/target
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 10s
    networks:
      - nexora_network

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile.dev
    container_name: nexora-frontend
    environment:
      VITE_API_BASE_URL: http://localhost:8080/api
    ports:
      - "5173:5173"
    volumes:
      - ./frontend/src:/app/src
      - ./frontend/public:/app/public
    depends_on:
      - backend
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5173"]
      interval: 30s
      timeout: 5s
      retries: 3
    networks:
      - nexora_network

  nginx:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: nexora-nginx
    ports:
      - "80:80"
    depends_on:
      - backend
      - frontend
    volumes:
      - ./frontend/nginx.conf:/etc/nginx/nginx.conf:ro
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost/health"]
      interval: 30s
      timeout: 5s
      retries: 3
    networks:
      - nexora_network

volumes:
  mysql_data:
    driver: local

networks:
  nexora_network:
    driver: bridge
```

### Production Docker Compose with Resource Limits

```yaml
version: '3.9'

services:
  mysql:
    image: mysql:8.0.35-alpine
    container_name: nexora-mysql-prod
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    restart: unless-stopped
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 2G
        reservations:
          cpus: '0.5'
          memory: 1G
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD}"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - nexora_network

  backend:
    image: nexora/backend:${VERSION:-latest}
    container_name: nexora-backend-prod
    restart: unless-stopped
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/${MYSQL_DATABASE}
      SPRING_DATASOURCE_USERNAME: ${MYSQL_USER}
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      SPRING_PROFILES_ACTIVE: prod
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
    depends_on:
      mysql:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - nexora_network

  frontend:
    image: nexora/frontend:${VERSION:-latest}
    container_name: nexora-frontend-prod
    restart: unless-stopped
    deploy:
      resources:
        limits:
          cpus: '0.5'
          memory: 512M
        reservations:
          cpus: '0.25'
          memory: 256M
    depends_on:
      - backend
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost/health"]
      interval: 30s
      timeout: 5s
      retries: 3
    networks:
      - nexora_network
    ports:
      - "80:80"

volumes:
  mysql_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /var/lib/nexora/mysql

networks:
  nexora_network:
    driver: bridge
```

### Development Dockerfile for Backend (Hot Reload)

```dockerfile
# Dockerfile.dev - Development with hot reload
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Install Maven
RUN apk add --no-cache maven curl

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code (will be replaced by volume mount)
COPY src ./src

# Expose port
EXPOSE 8080

# Run Maven with spring-boot-devtools for hot reload
ENTRYPOINT ["mvn", "spring-boot:run", "-Dspring-boot.run.arguments=--spring.profiles.active=dev"]
```

## Anti-Patterns & Automated Fixes

### Anti-Pattern 1: Using `latest` Tag
**Problem**: Image tag doesn't reflect actual version; cannot reproduce builds reliably.
```dockerfile
# ❌ WRONG
FROM node:latest
FROM mysql:latest
```
**Fix**: Pin to specific versions.
```dockerfile
# ✅ CORRECT
FROM node:20.11.0-alpine
FROM mysql:8.0.35-alpine
```

### Anti-Pattern 2: Running as Root
**Problem**: Container compromise allows root-level access to host.
```dockerfile
# ❌ WRONG
FROM node:20-alpine
RUN npm install
CMD ["npm", "start"]
# Default runs as root
```
**Fix**: Create and switch to unprivileged user.
```dockerfile
# ✅ CORRECT
FROM node:20-alpine
RUN addgroup -g 1000 appgroup && adduser -D -u 1000 -G appgroup appuser
COPY --chown=appuser:appgroup . .
USER appuser
CMD ["npm", "start"]
```

### Anti-Pattern 3: No Health Checks
**Problem**: Orchestrator cannot detect unhealthy containers; keeps routing traffic to dead instances.
```yaml
# ❌ WRONG
services:
  backend:
    build: ./backend
    # No healthcheck defined
```
**Fix**: Define health checks.
```yaml
# ✅ CORRECT
services:
  backend:
    build: ./backend
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3
```

### Anti-Pattern 4: Single-Stage Build Including Dev Dependencies
**Problem**: Production image is bloated with dev tools; slows deployment and increases attack surface.
```dockerfile
# ❌ WRONG
FROM node:20-alpine
COPY . .
RUN npm install  # Includes devDependencies
EXPOSE 3000
CMD ["npm", "start"]
# Image size: ~500MB
```
**Fix**: Use multi-stage build to exclude dev dependencies.
```dockerfile
# ✅ CORRECT
FROM node:20-alpine AS builder
COPY . .
RUN npm install && npm run build

FROM node:20-alpine
COPY --from=builder /app/dist /app/dist
RUN npm install --production  # Only production dependencies
CMD ["npm", "start"]
# Image size: ~150MB
```

## Verification Commands

### Scan Image for Vulnerabilities
```bash
# Install trivy
curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b .

# Scan backend image
./trivy image nexora/backend:latest

# Scan frontend image
./trivy image nexora/frontend:latest
```

### Test Docker Compose Setup
```bash
# Build all images
docker-compose build

# Start services
docker-compose up -d

# Check service health
docker-compose ps

# View logs
docker-compose logs -f backend

# Test API endpoint
curl -X GET http://localhost:8080/actuator/health

# Stop services
docker-compose down
```

### Verify Non-Root User
```bash
# Check running user
docker exec nexora-backend id
# Should output: uid=1000(appuser) gid=1000(appgroup) groups=1000(appgroup)

docker exec nexora-frontend id
# Should output: uid=101(nginx) gid=101(nginx)...
```

### Check Image Size
```bash
# List image sizes
docker images | grep nexora

# Compare single vs multi-stage build
docker history nexora/backend:latest

# Detailed layer inspection
docker inspect nexora/backend:latest | jq '.RootFS'
```

### Test Health Checks
```bash
# Manually trigger health check
docker exec nexora-backend curl -f http://localhost:8080/actuator/health

# View health check status
docker inspect --format='{{json .State.Health}}' nexora-backend | jq

# Simulate service failure and observe restart
docker exec nexora-backend kill -9 1
# Container should restart within 30 seconds
docker ps | grep nexora-backend
```

### Production Deployment Test
```bash
# Build production images
docker-compose -f docker-compose.prod.yml build

# Start with resource limits
docker-compose -f docker-compose.prod.yml up -d

# Monitor resource usage
docker stats

# Load test
curl -N http://localhost/ | head -100
```

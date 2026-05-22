FROM node:20-alpine AS builder

WORKDIR /app

# Tăng memory tránh fail build
ENV NODE_OPTIONS="--max-old-space-size=4096"

# Cài đặt pnpm
RUN npm install -g pnpm

# Copy dependency trước để tận dụng cache
COPY package.json pnpm-lock.yaml ./

# Cài dependency (nhẹ hơn)
RUN pnpm install --frozen-lockfile

# Copy source
COPY . .

# Inject build-time API URL for Vite
ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL

# Build
RUN pnpm build


FROM nginx:alpine

# Xóa config default (tránh conflict)
RUN rm -rf /etc/nginx/conf.d/*

# Copy build
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy config nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
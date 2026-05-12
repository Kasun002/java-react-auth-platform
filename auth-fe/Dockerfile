# ============================================================
# Stage 1 — Install dependencies
# Separate layer so npm install is cached unless package.json changes
# ============================================================
FROM node:20-alpine AS deps

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --frozen-lockfile


# ============================================================
# Stage 2 — Build production bundle
# ============================================================
FROM deps AS build

COPY . .

# VITE_API_BASE_URL is baked into the JS bundle at build time.
# In k8s, nginx proxies /api/ → auth-service so the value is /api.
# Override at build time:  --build-arg VITE_API_BASE_URL=https://api.example.com
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build


# ============================================================
# Stage 3 — Production runtime (nginx)
# Serves the static bundle; proxies /api/ to the auth-service.
# Runs on port 8080 as non-root (nginx uid=101 in alpine).
# ============================================================
FROM nginx:stable-alpine AS runtime

# ── Security: nginx:stable-alpine ships UID 101 (nginx) ──────────────────────
# We drop root after setup; port 8080 does not require CAP_NET_BIND_SERVICE.

# Remove default config supplied by the image
RUN rm /etc/nginx/conf.d/default.conf

# Copy our custom nginx config
COPY nginx.conf /etc/nginx/nginx.conf

# Copy built assets from stage 2
COPY --from=build /app/dist /usr/share/nginx/html

# ── Create writable dirs required by readOnlyRootFilesystem in k8s ───────────
# nginx writes pid, temp files, and cache to these paths.
RUN mkdir -p /tmp/nginx/client-body \
             /tmp/nginx/proxy \
             /tmp/nginx/fastcgi \
             /var/cache/nginx \
    && chown -R nginx:nginx \
         /tmp/nginx \
         /var/cache/nginx \
         /usr/share/nginx/html \
    && chmod -R 755 /usr/share/nginx/html

USER nginx

EXPOSE 8080

# ── Docker-level health check (fallback if k8s probes are not configured) ────
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://localhost:8080/health || exit 1

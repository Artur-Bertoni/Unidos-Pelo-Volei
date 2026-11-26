FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG VITE_API_URL
ARG VITE_POWERSYNC_URL
ARG VITE_GOOGLE_CLIENT_ID
ARG VITE_VAPID_PUBLIC_KEY
ARG VITE_ENVIRONMENT
RUN npm run build

FROM caddy:2-alpine
COPY Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/dist /srv
EXPOSE 8080

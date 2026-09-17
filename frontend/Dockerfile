# Local dev (docker-compose) and a future on-prem/self-host path for enterprise
# clients. FRIS's own hosted instance deploys straight to Vercel, undockerized,
# matching board-portal — see the scaffold plan's Docker/CI/deploy section.
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm install
COPY . .
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80

#!/usr/bin/env bash
# Bring up the full stack with Docker. Requires only Docker.
#   ./scripts/run-stack.sh           # build + start
#   ./scripts/run-stack.sh --seed    # also load 100K+ SKU seed data
set -euo pipefail
cd "$(dirname "$0")/.."

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not installed: https://www.docker.com/products/docker-desktop/" >&2
  exit 1
fi

echo "Building and starting the stack..."
docker compose up -d --build

echo "Waiting for the gateway to become healthy..."
for _ in $(seq 1 60); do
  if curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; then break; fi
  sleep 5
done

if [[ "${1:-}" == "--seed" ]]; then
  echo "Generating and loading 100K+ SKU seed data..."
  python3 scripts/seed/generate_seed.py --skus 100000
  docker compose exec -T mysql mysql -uroot -proot < scripts/db/seed-items.sql
fi

cat <<'EOF'

Stack is up:
  Frontend     http://localhost:3001
  Gateway API  http://localhost:8080
  AI assistant http://localhost:8087
  Prometheus   http://localhost:9090
  Grafana      http://localhost:3000  (admin/admin)
  RabbitMQ UI  http://localhost:15672 (guest/guest)
  Nacos        http://localhost:8848/nacos

Stop with: docker compose down
EOF

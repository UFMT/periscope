#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

docker compose up --build -d

echo
echo "Periscope SPA:  http://localhost:8080/periscope/app/"
echo "API REST:       http://localhost:8080/periscope/rest/"
echo "OpenAPI:        http://localhost:8080/openapi"
echo "Health:         http://localhost:8080/periscope/rest/health"
echo "Login padrão:   admin / 123456"
echo "Logs:           docker compose logs -f periscope"

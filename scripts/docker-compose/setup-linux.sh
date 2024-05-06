#!/usr/bin/env bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
REPO_DIR="$SCRIPT_DIR"/../..

echo "Replacing user and group IDs in override file ..."
GID=$(id -g)
sed "s/\${UID}/$UID/g" "$SCRIPT_DIR/docker-compose.linux.override.yml" > "$REPO_DIR/docker-compose.override.yml"
sed -i "s/\${GID}/$GID/g" "$REPO_DIR/docker-compose.override.yml"

echo "Changing ownership of Docker Compose volume data directory ..."
sudo chown -R "$UID:$GID" "$REPO_DIR/scripts/docker-compose/volume-data"

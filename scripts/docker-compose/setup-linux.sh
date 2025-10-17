#!/usr/bin/env bash

set -e

#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
REPO_DIR="$SCRIPT_DIR"/../..

echo "Replacing user and group IDs in override file ..."
GID=$(id -g)
sed "s/\${UID}/$UID/g" "$SCRIPT_DIR/docker-compose.linux.override.yml" > "$REPO_DIR/docker-compose.override.yml"
sed -i "s/\${GID}/$GID/g" "$REPO_DIR/docker-compose.override.yml"

echo "Changing ownership of directories ..."
[ -d "$REPO_DIR/scripts/docker-compose/volume-data" ] && sudo chown -R "$UID:$GID" "$REPO_DIR/scripts/docker-compose/volume-data"
[ -d "$REPO_DIR/build" ] && sudo chown -R "$UID:$GID" "$REPO_DIR/build"

echo "Copying support environment scripts ..."
cp "$SCRIPT_DIR/fix-permissions.sh" "$REPO_DIR/fix-permissions.sh"
cp "$SCRIPT_DIR/check-for-running-containers.sh" "$REPO_DIR/check-for-running-containers.sh"

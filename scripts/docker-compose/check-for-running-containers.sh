#!/usr/bin/env bash

set -e

#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"

(
  cd "$SCRIPT_DIR"

  if sudo lsof -i@localhost:8080; then
    echo -e "\nPlease stop the currently running process on port 8080!"
    exit 1
  fi

  if docker compose ls --filter name=^zac$ | grep running; then
    echo -e "\nDocker compose already running!"
    exit 1
  fi
)

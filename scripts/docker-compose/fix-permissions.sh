#!/usr/bin/env bash

set -e

#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"

(
  cd "$SCRIPT_DIR"

  echo -e "\nFixing directory permissions ..."
  VOLUMES_DIR=scripts/docker-compose/volume-data
  if [ -d "$VOLUMES_DIR" ]; then
    sudo chown -R "$USER:$USER" "$VOLUMES_DIR" build/ output/
  fi
)

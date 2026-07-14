#!/bin/sh

#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
set -eu

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

npx doctoc --github --notitle --maxlevel 3 "$SCRIPT_DIR/inrichting-zaakafhandelcomponent.md"

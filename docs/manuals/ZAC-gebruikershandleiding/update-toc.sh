#!/bin/sh

#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

npx doctoc --github --notitle --maxlevel 3 "$SCRIPT_DIR/ZAC-gebruikershandleiding.md"

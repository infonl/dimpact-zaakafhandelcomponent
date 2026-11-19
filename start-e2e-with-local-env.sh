#!/bin/sh

#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

# Runs the end-to-end tests against the local kind stack of ./start-kind.sh.
# Please refer to the instructions in `docs/development/testing.md` before using this script.
# The arguments go to Playwright, for example: ./start-e2e-with-local-env.sh --ui

set -e

cd "$(dirname "$0")/src/e2e"

if [ "$SKIP_INSTALL" != "true" ]; then
  echo "Install dependencies ..."
  npm ci --ignore-scripts

  echo "Install the browser of Playwright ..."
  npx playwright install --with-deps chromium
fi

npm test -- "$@"

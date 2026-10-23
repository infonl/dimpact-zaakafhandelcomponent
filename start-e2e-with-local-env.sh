#!/bin/sh

#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

# Runs the end-to-end tests against the local kind stack of ./start-kind.sh.
# Please refer to the instructions in `docs/development/testing.md` before using this script.

set -e

cd "$(dirname "$0")"

ports_file="build/kind/${KIND_CLUSTER_NAME:-zac}/ports.env"
zac_port=$(sed -n 's/^HOST_PORT_ZAC=//p' "$ports_file" 2> /dev/null || true)

export E2E_TEST_USER_1_USERNAME="${E2E_TEST_USER_1_USERNAME:-beheerder1}"
export E2E_TEST_USER_1_PASSWORD="${E2E_TEST_USER_1_PASSWORD:-$E2E_TEST_USER_1_USERNAME}"
export E2E_TEST_USER_2_USERNAME="${E2E_TEST_USER_2_USERNAME:-behandelaar1}"
export E2E_TEST_USER_2_PASSWORD="${E2E_TEST_USER_2_PASSWORD:-$E2E_TEST_USER_2_USERNAME}"
export ZAC_URL="${ZAC_URL:-http://localhost:${zac_port:-8080}}"
export EXCLUDE_LIVE_SCENARIO_TAGS=true

echo "Running the end-to-end tests against $ZAC_URL ..."
exec ./start-e2e.sh

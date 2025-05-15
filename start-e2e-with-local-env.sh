#!/bin/sh

#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

# Please refer to the instructions in `docs/development/testing.md` before using this script.

export E2E_TEST_USER_1_USERNAME=testuser1
export E2E_TEST_USER_1_PASSWORD=testuser1
export E2E_TEST_USER_2_USERNAME=testuser2
export E2E_TEST_USER_2_PASSWORD=testuser2
export ZAC_URL=http://host.docker.internal:8080
export EXCLUDE_LIVE_SCENARIO_TAGS=true

./start-e2e.sh

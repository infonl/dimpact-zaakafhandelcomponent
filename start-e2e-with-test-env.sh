#!/bin/sh

#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

export APP_ENV=devlocal
export E2E_TEST_GROUP_A_ID=test_group_a
export E2E_TEST_GROUP_B_ID=test_group_b

op run --env-file="./.env.tpl" -- ./start-e2e.sh

#!/bin/sh

#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#

# Please refer to the instructions in `docs/development/testing.md` before using this script.

export E2E_TEST_USER_1_USERNAME=testuser1
export E2E_TEST_USER_1_PASSWORD=testuser1
export OPEN_FORMS_URL=http://host.docker.internal:8082
export ZAC_URL=http://host.docker.internal:8080

./start-e2e.sh

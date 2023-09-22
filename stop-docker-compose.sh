#!/bin/sh

#
# SPDX-FileCopyrightText: 2023 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#

# Uses the 1Password CLI tools to set up the environment variables for running Docker Compose and ZAC in IntelliJ.
# Please see docs/INSTALL.md for details on how to use this script.
# Note that it is not strictly required to use the 1Password CLI tools to stop Docker Compose,
# however by doing you do avoid Docker Compose warnings about variables not being set.

export APP_ENV=devlocal && op run --env-file="./.env.tpl" -- docker compose --profile zac down

#!/bin/sh

#
# SPDX-FileCopyrightText: 2023 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#

# Do a pull first to make sure we use the latest ZAC Docker Image
docker compose pull zac

# Uses the 1Password CLI tools to set up the environment variables for running Docker Compose and ZAC in IntelliJ.
# Please see docs/INSTALL.md for details on how to use this script.
export APP_ENV=devlocal && op run --env-file="./.env.tpl" -- docker compose --project-name zac up -d

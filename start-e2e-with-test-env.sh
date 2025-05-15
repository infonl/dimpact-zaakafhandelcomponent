#!/bin/sh

#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

export APP_ENV=devlocal

op run --env-file="./.env.tpl" -- ./start-e2e.sh

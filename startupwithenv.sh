#!/bin/sh

#
# SPDX-FileCopyrightText: 2023 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#

# Uses the 1Password CLI tools to set up the environment variables for running ZAC in IntelliJ.
# Please see docs/INSTALL.md for details on how to use this script.

op run --env-file="./.env.tpl" -- ./wildfly-26.1.2.Final/bin/standalone.sh

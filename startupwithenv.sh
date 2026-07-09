#!/bin/sh

#
# SPDX-FileCopyrightText: 2023 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#

# Uses the 1Password CLI tools to set up the environment variables for running ZAC in IntelliJ.
# Please see docs/development/INSTALL.md for details on how to use this script.

WILDFLY_VERSION=$(grep -E '<wildfly.version>' pom.xml | awk -F'[<>]' '{print $3}')

echo "Starting WildFly '$WILDFLY_VERSION' with environment variables from 1Password..."
# Note that we do not use masking as this does not work well in our context and will result in
# crashes of WildFly/ZAC when running in IntelliJ when you have configured DEBUG logging in WildFly
# with errors such as: "fatal error: concurrent map read and map write goroutine 2013 [running]:
# go.1password.io/op/op-cli/command/subprocess/masking.matches.add(...)"
op run --env-file="./.env.tpl" --no-masking -- ./wildfly-$WILDFLY_VERSION/bin/standalone.sh -b=0.0.0.0

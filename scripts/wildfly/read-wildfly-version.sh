#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

# Sets WILDFLY_VERSION from pom.xml's <wildfly.version> property. Meant to be sourced (not executed)
# by scripts that need it, so a single place validates that it was actually found.
# Please follow the instructions in 'updatingDependencies.md' when upgrading WildFly.

WILDFLY_VERSION=$(grep -E '<wildfly.version>' ../../pom.xml | awk -F'[<>]' '{print $3}')

if [ -z "$WILDFLY_VERSION" ]; then
    echo "Could not find a <wildfly.version> property in ../../pom.xml."
    exit 1
fi

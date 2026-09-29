#!/bin/sh

set -e

#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

# Verifies that the 'jboss-logmanager' version pinned in gradle/libs.versions.toml matches the
# version of the 'org.jboss.logmanager:jboss-logmanager' module actually bundled inside a locally
# installed WildFly server (installed via install-wildfly.sh). This module is not covered by any
# WildFly BOM, so its version cannot be verified automatically by Gradle or Renovate; this script
# replaces the manual check described in 'updatingDependencies.md'.

# Change to directory where this script is located
cd "$(dirname "$0")" || exit

. ./read-wildfly-version.sh
WILDFLY_SERVER_DIR=../../wildfly-$WILDFLY_VERSION
MODULE_XML="$WILDFLY_SERVER_DIR/modules/system/layers/base/org/jboss/logmanager/main/module.xml"

if [ ! -f "$MODULE_XML" ]; then
    echo "Could not find $MODULE_XML."
    echo "Please run install-wildfly.sh first to install WildFly $WILDFLY_VERSION locally."
    exit 1
fi

BUNDLED_VERSION=$(grep -o 'jboss-logmanager-[0-9][^"]*\.jar' "$MODULE_XML" | head -1 | sed -E 's/jboss-logmanager-(.*)\.jar/\1/')

if [ -z "$BUNDLED_VERSION" ]; then
    echo "Could not determine the bundled jboss-logmanager version from $MODULE_XML."
    exit 1
fi

CATALOG_VERSION=$(grep -E '^jboss-logmanager = "' ../../gradle/libs.versions.toml | awk -F'"' '{print $2}')

if [ -z "$CATALOG_VERSION" ]; then
    echo "Could not find a 'jboss-logmanager' version in ../../gradle/libs.versions.toml."
    exit 1
fi

if [ "$BUNDLED_VERSION" != "$CATALOG_VERSION" ]; then
    echo "Mismatch: WildFly $WILDFLY_VERSION bundles jboss-logmanager $BUNDLED_VERSION, but gradle/libs.versions.toml pins $CATALOG_VERSION."
    echo "Update the 'jboss-logmanager' version in gradle/libs.versions.toml to $BUNDLED_VERSION."
    exit 1
fi

echo "OK: gradle/libs.versions.toml's jboss-logmanager version ($CATALOG_VERSION) matches the version bundled in WildFly $WILDFLY_VERSION."

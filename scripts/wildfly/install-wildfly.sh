#!/bin/sh

set -e

#
# SPDX-FileCopyrightText: 2021 Atos, 2023 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

# Script to install WildFly and the required layers and packages and configure WildFly for ZAC.
# Note that this requires Galleon to be installed locally.

# Change to directory where this script is located
cd "$(dirname "$0")" || exit

# WildFly version, layers and data-sources are taken from pom.xml
# Please follow the instructions in 'updatingDependencies.md' when upgrading WildFly.
WILDFLY_VERSION=$(grep -E '<wildfly.version>' ../../pom.xml | awk -F'[<>]' '{print $3}')
WILDFLY_LAYERS=$(awk -F'[<>]' '/<layer>/{printf "%s,", $3}' ../../pom.xml | sed 's/,$//' | sed 's/postgresql-driver,\{0,1\}//')
WILDFLY_DATASOURCES_GALLEON_PACK_VERSION=$(grep -E '<wildfly-datasources-galleon-pack.version>' ../../pom.xml | awk -F'[<>]' '{print $3}')

WILDFLY_SERVER_DIR=../../wildfly-$WILDFLY_VERSION

export PATH
PATH=$PATH:$(pwd)/galleon/bin

echo ">>> Installing WildFly ..."
rm -fr "$WILDFLY_SERVER_DIR"
galleon.sh install wildfly#"$WILDFLY_VERSION" --dir="$WILDFLY_SERVER_DIR" --layers="$WILDFLY_LAYERS"
galleon.sh install org.wildfly:wildfly-datasources-galleon-pack:"$WILDFLY_DATASOURCES_GALLEON_PACK_VERSION" --dir="$WILDFLY_SERVER_DIR" --layers=postgresql-driver
"$WILDFLY_SERVER_DIR"/bin/jboss-cli.sh --file=install-wildfly.cli

# The Web Console can be enabled by:
# - adding the web-console layer to the --layers attribute
# - creating an admin userid/password combination by running: $WILDFLY_SERVER_DIR/bin/add-user.sh --user admin --group admin --password admin

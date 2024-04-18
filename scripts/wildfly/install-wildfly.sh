#!/bin/sh

#
# SPDX-FileCopyrightText: 2021 Atos, 2023 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#

# Script to install WildFly and the required layers and packages and configure WildFly for ZAC.
# Note that this requires Galleon to be installed locally.

# Change to directory where this script is located
cd "$(dirname "$0")" || exit

# WildFly version is taken from pom.xml
# Please follow the instructions in 'updatingDependencies.md' when upgrading WildFly.
export WILDFLY_VERSION=$(grep -E '<wildfly.version>' ../../pom.xml | awk -F'[<>]' '{print $3}')
export WILDFLY_DATASOURCES_GALLEON_PACK_VERSION=$(grep -E '<wildfly-datasources-galleon-pack.version>' ../../pom.xml | awk -F'[<>]' '{print $3}')

export WILDFLY_SERVER_DIR=../../wildfly-$WILDFLY_VERSION
export PATH=$PATH:$(pwd)/galleon/bin

echo ">>> Installing WildFly ..."
rm -fr $WILDFLY_SERVER_DIR
galleon.sh install wildfly#$WILDFLY_VERSION --dir=$WILDFLY_SERVER_DIR --layers=jaxrs-server,microprofile-health,microprofile-fault-tolerance,elytron-oidc-client,metrics,opentelemetry
galleon.sh install org.wildfly:wildfly-datasources-galleon-pack:$WILDFLY_DATASOURCES_GALLEON_PACK_VERSION --dir=$WILDFLY_SERVER_DIR --layers=postgresql-driver
$WILDFLY_SERVER_DIR/bin/jboss-cli.sh --file=install-wildfly.cli

# The Web Console can be enabled by:
# - adding the web-console layer to the --layers attribute
# - creating an admin userid/password combination by running: $WILDFLY_SERVER_DIR/bin/add-user.sh --user admin --group admin --password admin

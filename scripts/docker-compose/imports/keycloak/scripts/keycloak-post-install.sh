#!/bin/bash

#
# SPDX-FileCopyrightText: 2025 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

set -euo pipefail

echo -e "Setting the 'sslRequired' parameter to 'NONE' for the Keycloak master realm so that SSL is not required under any circumstances"

/opt/keycloak/bin/kcadm.sh config credentials --server http://keycloak:8080 --realm master --user admin --password admin
/opt/keycloak/bin/kcadm.sh update realms/master -s sslRequired=NONE

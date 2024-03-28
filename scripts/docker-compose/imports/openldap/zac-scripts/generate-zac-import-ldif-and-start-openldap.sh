#!/bin/bash

set -e

echo -e "Generating ZAC LDIF import file.."

# generate ZAC import LDIF by substituting variables in a template LDIF file with environment variables
# note that we cannot do this in a /docker-entrypoint-initdb.d/ script because OpenLDAP may already have started then
sed -f /zac-scripts/zac-import-ldif-substitutions.sed /zac-scripts/zac-ldap-setup-template.ldif > /tmp/zac-ldap-setup.ldif

echo -e "ZAC LDIF import file '/tmp/zac-ldap-setup.ldif' generated"

echo -e "Starting OpenLDAP.."

/opt/bitnami/scripts/openldap/entrypoint.sh "$@"

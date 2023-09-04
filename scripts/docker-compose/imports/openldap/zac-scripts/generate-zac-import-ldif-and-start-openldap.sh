#!/bin/bash

echo -e "Generating ZAC LDIF import file.."

# generate ZAC import LDIF by substituting variables in template with environment variables
# note that we cannot do this in an /docker-entrypoint-initdb.d/ script because OpenLDAP may already have started then
sed "s/\${TESTUSER1_EMAIL_ADDRESS}/${ZAC_LDAP_TESTUSER1_EMAIL_ADDRESS}/g; s/\${TESTUSER2_EMAIL_ADDRESS}/${ZAC_LDAP_TESTUSER2_EMAIL_ADDRESS}/g; s/\${GROUP_A_EMAIL_ADDRESS}/${ZAC_LDAP_GROUP_A_EMAIL_ADDRESS}/g;" /zac-scripts/zac-ldap-setup-template.ldif > /ldifs/zac-ldap-setup.ldif

echo -e "ZAC LDIF import file '/ldifs/zac-ldap-setup.ldif' generated"

echo -e "Starting OpenLDAP.."

/opt/bitnami/scripts/openldap/entrypoint.sh "$@"

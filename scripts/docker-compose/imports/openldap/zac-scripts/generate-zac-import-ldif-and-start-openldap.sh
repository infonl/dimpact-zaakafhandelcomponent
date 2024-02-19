#!/bin/bash

echo -e "Generating ZAC LDIF import file.."

# generate ZAC import LDIF by substituting variables in template with environment variables
# note that we cannot do this in an /docker-entrypoint-initdb.d/ script because OpenLDAP may already have started then
sed "s/\${TESTUSER1_EMAIL_ADDRESS}/${ZAC_TESTUSER1_EMAIL_ADDRESS}/g; s/\${TESTUSER2_EMAIL_ADDRESS}/${ZAC_TESTUSER2_EMAIL_ADDRESS}/g; s/\${RECORDMANAGER1_EMAIL_ADDRESS}/${ZAC_RECORD_MANAGER_1_EMAIL_ADDRESS}/g; s/\${FUNCTIONAL_ADMIN1_EMAIL_ADDRESS}/${ZAC_FUNCTIONAL_ADMIN_1_EMAIL_ADDRESS}/g; s/\${GROUP_A_EMAIL_ADDRESS}/${ZAC_GROUP_A_EMAIL_ADDRESS}/g; s/\${GROUP_FUNCTIONEEL_BEHEERDERS_EMAIL_ADDRESS}/${ZAC_GROUP_FUNCTIONEEL_BEHEERDERS_EMAIL_ADDRESS}/g; s/\${GROUP_RECORD_MANAGERS_EMAIL_ADDRESS}/${ZAC_GROUP_RECORD_MANAGERS_EMAIL_ADDRESS}/g;" /zac-scripts/zac-ldap-setup-template.ldif > /ldifs/zac-ldap-setup.ldif

echo -e "ZAC LDIF import file '/ldifs/zac-ldap-setup.ldif' generated"

echo -e "Starting OpenLDAP.."

/opt/bitnami/scripts/openldap/entrypoint.sh "$@"

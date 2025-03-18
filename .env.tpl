#
# SPDX-FileCopyrightText: <YYYY> Lifely
# SPDX-License-Identifier: EUPL-1.2+
#

# This file contains a list of environment variables that need to, or in case of optional variables can be, set when running ZAC locally.

# To use this file you need to use the 1Password CLI extensions.
# Please see the docs/INSTALL.md file for instructions.
# Please see .env.example for descriptions of the environment variables.

# -------------------------
# ZAC environment variables
# -------------------------

AUTH_REALM=zaakafhandelcomponent
AUTH_RESOURCE=zaakafhandelcomponent
AUTH_SECRET=keycloakZaakafhandelcomponentClientSecret
AUTH_SERVER=http://localhost:8081
BAG_API_CLIENT_MP_REST_URL=op://Dimpact/ZAC-.env-$APP_ENV/BAG/API_CLIENT_MP_REST_URL
BAG_API_KEY=op://Dimpact/ZAC-.env-$APP_ENV/BAG/API_KEY
BRON_ORGANISATIE_RSIN=123443210
BRP_API_CLIENT_MP_REST_URL=http://localhost:5010/haalcentraal/api/brp
BRP_API_KEY=dummyKey
BRP_ORIGIN_OIN=dummyOIN
BRP_DOELBINDING_DEFAULT=BRPACT-Totaal
BRP_VERWERKING_DEFAULT=zaakafhandelcomponent
CONTEXT_URL=http://localhost:8080
DB_HOST=localhost:54320
DB_NAME=zac
DB_PASSWORD=password
DB_USER=zac
FEATURE_FLAG_BPMN_SUPPORT=false
GEMEENTE_CODE=0001
GEMEENTE_MAIL=op://Dimpact/ZAC-.env-$APP_ENV/GEMEENTE/MAIL
GEMEENTE_NAAM=DummyGemeente
KEYCLOAK_ADMIN_CLIENT_ID=zaakafhandelcomponent-admin-client
KEYCLOAK_ADMIN_CLIENT_SECRET=zaakafhandelcomponentAdminClientSecret
KLANTINTERACTIES_API_CLIENT_MP_REST_URL=http://localhost:8002
KLANTINTERACTIES_API_TOKEN=dummyToken
KVK_API_CLIENT_MP_REST_URL=op://Dimpact/ZAC-.env-$APP_ENV/KVK/API_CLIENT_MP_REST_URL
KVK_API_KEY=op://Dimpact/ZAC-.env-$APP_ENV/KVK/API_KEY
# We use the Base2 system to calculate the max file size in bytes.
MAX_FILE_SIZE_MB=80
OBJECTS_API_CLIENT_MP_REST_URL=http://host.docker.internal:8010
OBJECTS_API_TOKEN=dummyZacObjectsToken
OBJECTTYPES_API_CLIENT_MP_REST_URL=http://host.docker.internal:8011
OBJECTTYPES_API_TOKEN=dummyZacObjectTypesToken
OFFICE_CONVERTER_CLIENT_MP_REST_URL=http://localhost:8083
OPA_API_CLIENT_MP_REST_URL=http://localhost:8181
OPEN_NOTIFICATIONS_API_SECRET_KEY=openNotificatiesApiSecretKey
SMARTDOCUMENTS_ENABLED=true
SMARTDOCUMENTS_AUTHENTICATION=op://Dimpact/ZAC-.env-$APP_ENV/SMARTDOCUMENTS/AUTHENTICATION
SMARTDOCUMENTS_CLIENT_MP_REST_URL=op://Dimpact/ZAC-.env-$APP_ENV/SMARTDOCUMENTS/CLIENT_MP_REST_URL
SMARTDOCUMENTS_FIXED_USER_NAME=op://Dimpact/ZAC-.env-$APP_ENV/SMARTDOCUMENTS/SMARTDOCUMENTS_FIXED_USER_NAME
SMTP_SERVER=op://Dimpact/ZAC-.env-$APP_ENV/SMTP/SERVER
SMTP_PORT=op://Dimpact/ZAC-.env-$APP_ENV/SMTP/PORT
SMTP_USERNAME=op://Dimpact/ZAC-.env-$APP_ENV/MAILJET/API_KEY
SMTP_PASSWORD=op://Dimpact/ZAC-.env-$APP_ENV/MAILJET/API_SECRET_KEY
SIGNALERINGEN_DELETE_OLDER_THAN_DAYS=14
SOLR_URL=http://localhost:8983
VERANTWOORDELIJKE_ORGANISATIE_RSIN=316245124
ZGW_API_CLIENT_MP_REST_URL=http://localhost:8001/
ZGW_API_CLIENTID=zac_client
ZGW_API_SECRET=openzaakZaakafhandelcomponentClientSecret
ZGW_API_URL_EXTERN=http://localhost:8001/

# -----------------------------------------
# e2e only environment variables
# -----------------------------------------
E2E_TEST_USER_1_USERNAME=op://Dimpact/e2etestuser1/username
E2E_TEST_USER_1_PASSWORD=op://Dimpact/e2etestuser1/password
E2E_TEST_USER_2_USERNAME=op://Dimpact/e2etestuser2/username
E2E_TEST_USER_2_PASSWORD=op://Dimpact/e2etestuser2/password
ZAC_URL=op://Dimpact/zaakafhandelcomponent-zac-dev/website
OPEN_FORMS_URL=op://Dimpact/open-formulieren-zac-dev/website

# -----------------------------------------
# Docker Compose only environment variables
# -----------------------------------------
DOCKER_COMPOSE_TEST_USER_1_EMAIL_ADDRESS=op://Dimpact/ZAC-.env-$APP_ENV/DOCKER_COMPOSE/TEST_USER_1_EMAIL_ADDRESS
DOCKER_COMPOSE_TEST_USER_2_EMAIL_ADDRESS=op://Dimpact/ZAC-.env-$APP_ENV/DOCKER_COMPOSE/TEST_USER_2_EMAIL_ADDRESS
DOCKER_COMPOSE_RECORD_MANAGER_1_EMAIL_ADDRESS=op://Dimpact/ZAC-.env-$APP_ENV/DOCKER_COMPOSE/RECORD_MANAGER_1_EMAIL_ADDRESS
DOCKER_COMPOSE_FUNCTIONAL_ADMIN_1_EMAIL_ADDRESS=op://Dimpact/ZAC-.env-$APP_ENV/DOCKER_COMPOSE/FUNCTIONAL_ADMIN_1_EMAIL_ADDRESS
DOCKER_COMPOSE_TESTUSER_DOMEIN_TEST_1_EMAIL_ADDRESS=op://Dimpact/ZAC-.env-$APP_ENV/DOCKER_COMPOSE/TESTUSER_DOMEIN_TEST_1_EMAIL_ADDRESS
DOCKER_COMPOSE_GROUP_A_EMAIL_ADDRESS=op://Dimpact/ZAC-.env-$APP_ENV/DOCKER_COMPOSE/GROUP_A_EMAIL_ADDRESS
DOCKER_COMPOSE_GROUP_FUNCTIONEEL_BEHEERDERS_EMAIL_ADDRESS=op://Dimpact/ZAC-.env-$APP_ENV/DOCKER_COMPOSE/GROUP_FUNCTIONEEL_BEHEERDERS_EMAIL_ADDRESS
DOCKER_COMPOSE_GROUP_RECORD_MANAGERS_EMAIL_ADDRESS=op://Dimpact/ZAC-.env-$APP_ENV/DOCKER_COMPOSE/GROUP_RECORD_MANAGERS_EMAIL_ADDRESS
DOCKER_COMPOSE_GROUP_DOMEIN_TEST_1_EMAIL_ADDRESS=op://Dimpact/ZAC-.env-$APP_ENV/DOCKER_COMPOSE/GROUP_DOMEIN_TEST_1_EMAIL_ADDRESS

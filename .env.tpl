# This file contains a list of environment variables that need to be set when running ZAC locally.
# Most variables are set to default values which assume that you have the corresponding service that
# is required by ZAC (like Open Zaak and Keycloak) running locally, for example using a Docker Compose setup.

# To use this file you can copy it to a file named '.env' and set or change variables where needed.

# NOT FULLY COMPLETE YET - IN PROGRESS

# Keycloak ZAC realm
AUTH_REALM=op://Dimpact/ZAC-.env-$APP_ENV/AUTH/REALM
# Keycloak ZAC client
AUTH_RESOURCE=op://Dimpact/ZAC-.env-$APP_ENV/AUTH/RESOURCE
# Keycloak ZAC client secret
AUTH_SECRET=op://Dimpact/ZAC-.env-devlocal/AUTH/SECRET
# Keycloak URL
AUTH_SERVER=op://Dimpact/ZAC-.env-$APP_ENV/AUTH/SERVER
# BAG API REST URL. By default we use the HaalCentraal BAG test environment.
BAG_API_CLIENT_MP_REST_URL=op://Dimpact/ZAC-.env-$APP_ENV/BAG/API_CLIENT_MP_REST_URL
# If you want to use the BAG test environment you will need to request an API key.
# Please see: https://vng-realisatie.github.io/Haal-Centraal-BAG-bevragen/getting-started-IB
BAG_API_KEY=op://Dimpact/ZAC-.env-$APP_ENV/BAG/API_KEY
# BRP API REST URL. This assumes you have the BRP mock running locally.
BRP_API_CLIENT_MP_REST_URL=op://Dimpact/ZAC-.env-$APP_ENV/BRP/API_CLIENT_MP_REST_URL
# Not used when using the BRP proxy
BRP_API_KEY=op://Dimpact/ZAC-.env-$APP_ENV/BRP/API_KEY
# Open Klant klanten API REST URL
CONTACTMOMENTEN_API_CLIENT_MP_REST_URL=op://Dimpact/ZAC-.env-$APP_ENV/CONTACTMOMENTEN/API_CLIENT_MP_REST_URL
# Open Klant API client ID
CONTACTMOMENTEN_API_CLIENTID=op://Dimpact/ZAC-.env-$APP_ENV/CONTACTMOMENTEN/API_CLIENTID
# Open Klant API client secret
CONTACTMOMENTEN_API_SECRET=op://Dimpact/ZAC-.env-$APP_ENV/CONTACTMOMENTEN/API_SECRET
# ZAC context URL (take care not to add a trailing slash)
CONTEXT_URL=op://Dimpact/ZAC-.env-$APP_ENV/CONTEXT_URL
# ZAC PostgreSQL database host and port string
DB_HOST=op://Dimpact/ZAC-.env-$APP_ENV/DB/HOST
# ZAC PostgreSQL database name
DB_NAME=op://Dimpact/ZAC-.env-$APP_ENV/DB/NAME
# ZAC PostgreSQL database password
DB_PASSWORD=op://Dimpact/ZAC-.env-$APP_ENV/DB/PASSWORD
# ZAC PostgreSQL database user
DB_USER=op://Dimpact/ZAC-.env-$APP_ENV/DB/USER
# Dutch municipality code of the municipality for which this instance of ZAC is configured
GEMEENTE_CODE=op://Dimpact/ZAC-.env-$APP_ENV/GEMEENTE/CODE
# ZAC municipality email address. Replace by an email address of your choice in order to receive emails from ZAC.
GEMEENTE_MAIL=op://Dimpact/ZAC-.env-$APP_ENV/GEMEENTE/MAIL
# ZAC municipality name
GEMEENTE_NAAM=op://Dimpact/ZAC-.env-$APP_ENV/GEMEENTE/NAAM
# Open Klant klanten API REST URL
KLANTEN_API_CLIENT_MP_REST_URL=op://Dimpact/ZAC-.env-$APP_ENV/KLANTEN/API_CLIENT_MP_REST_URL
# Open Klant klanten API ZAC client ID
KLANTEN_API_CLIENTID=op://Dimpact/ZAC-.env-$APP_ENV/KLANTEN/API_CLIENTID
# Open Klant klanten API ZAC client secret
KLANTEN_API_SECRET=op://Dimpact/ZAC-.env-$APP_ENV/KLANTEN/API_SECRET
# LDAP DN containing both ZAC users and groups
LDAP_DN=op://Dimpact/ZAC-.env-$APP_ENV/LDAP/DN
# LDAP bind user password
LDAP_PASSWORD=op://Dimpact/ZAC-.env-$APP_ENV/LDAP/PASSWORD
# LDAP URL
LDAP_URL=op://Dimpact/ZAC-.env-$APP_ENV/LDAP/URL
# LDAP bind user
LDAP_USER=op://Dimpact/ZAC-.env-$APP_ENV/LDAP/USER
# Please replace by your own Mailjet API key and secret key in order to send emails from ZAC.
# You will need to create a Mailjet account. A free account will suffice for testing purposes.
MAILJET_API_KEY=op://Dimpact/ZAC-.env-devlocal/MAILJET/API_KEY
MAILJET_API_SECRET_KEY=op://Dimpact/ZAC-.env-devlocal/MAILJET/API_SECRET_KEY
# Maximum file upload size in MB. Currently cannot be increased much over the default value due to technical limitations.
MAX_FILE_SIZE_MB=op://Dimpact/ZAC-.env-$APP_ENV/MAX_FILE_SIZE_MB
# Open Policy Agent API REST URL
OPA_API_CLIENT_MP_REST_URL=op://Dimpact/ZAC-.env-$APP_ENV/OPA/API_CLIENT_MP_REST_URL
# Open Notifications API secret key
OPEN_NOTIFICATIONS_API_SECRET_KEY=op://Dimpact/ZAC-.env-$APP_ENV/OPEN_NOTIFICATIONS/API_SECRET_KEY
# SmartDocuments API authentication token. Please contact SmartDocuments to request an API token.
SD_AUTHENTICATION=op://Dimpact/ZAC-.env-$APP_ENV/SD/AUTHENTICATION
# SmartDocuments API REST URL
SD_CLIENT_MP_REST_URL=op://Dimpact/ZAC-.env-$APP_ENV/SD/CLIENT_MP_REST_URL
# Solr URL
SOLR_URL=op://Dimpact/ZAC-.env-$APP_ENV/SOLR_URL
# VNG Referentielijsten (VRL) API URL
# Note that this default value requires an entry in /etc/hosts to 127.0.0.1
VRL_API_CLIENT_MP_REST_URL=op://Dimpact/ZAC-.env-$APP_ENV/VRL/API_CLIENT_MP_REST_URL
# Open Zaak API URL
ZGW_API_CLIENT_MP_REST_URL=op://Dimpact/ZAC-.env-$APP_ENV/ZGW/API_CLIENT_MP_REST_URL
# Open Zaak API ZAC client ID
ZGW_API_CLIENTID=op://Dimpact/ZAC-.env-$APP_ENV/ZGW/API_CLIENTID
# Open Zaak API ZAC client secret
ZGW_API_SECRET=op://Dimpact/ZAC-.env-$APP_ENV/ZGW/API_SECRET
# External Open Zaak API URL. Needs to be externally accessible to be able to use the Smart Documents SaaS service with ZAC.
ZGW_API_URL_EXTERN=op://Dimpact/ZAC-.env-$APP_ENV/ZGW/API_URL_EXTERN

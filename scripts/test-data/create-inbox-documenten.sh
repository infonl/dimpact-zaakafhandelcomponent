#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Creates inbox documenten in the local kind stack: documents in Open Zaak that belong to no zaak.
#
# ZAC adds a document to Inbox documenten when it receives the notification that a document was
# created in Open Zaak. When Open Notificaties runs (./start-kind.sh -n) it sends that notification;
# the script waits for it, and sends the notification to ZAC itself for every document that is not
# in the inbox after the wait, so the script also works without Open Notificaties.
#
# Prerequisites: curl, jq and openssl; the local kind stack running with ZAC.
#
# Usage:
#   ./scripts/test-data/create-inbox-documenten.sh [-n COUNT] [-w WAIT_SECONDS]
#
#   -n COUNT         number of inbox documenten to create (default: 5)
#   -w WAIT_SECONDS  how long to wait for Open Notificaties before notifying ZAC directly (default: 20)
#
# Environment variables (defaults are those of the local kind stack):
#   ZAC_URL, KEYCLOAK_URL, OPEN_ZAAK_URL, ZAC_USER, ZAC_PASSWORD
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)

ZAC_URL=${ZAC_URL:-http://localhost:8080}
KEYCLOAK_URL=${KEYCLOAK_URL:-http://localhost:8081}
OPEN_ZAAK_URL=${OPEN_ZAAK_URL:-http://localhost:8001}
ZAC_USER=${ZAC_USER:-beheerder1}
ZAC_PASSWORD=${ZAC_PASSWORD:-beheerder1}

# Values of the local kind stack, see .env.example
KEYCLOAK_REALM=zaakafhandelcomponent
KEYCLOAK_CLIENT_ID=zaakafhandelcomponent
KEYCLOAK_CLIENT_SECRET=keycloakZaakafhandelcomponentClientSecret
OPEN_ZAAK_CLIENT_ID=zac-client
OPEN_ZAAK_CLIENT_SECRET=openzaakZaakafhandelcomponentClientSecret
NOTIFICATIONS_API_KEY=openNotificatiesApiSecretKey

INFORMATIEOBJECTTYPE_BIJLAGE_UUID=b1933137-94d6-49bc-9e12-afe712512276
BRONORGANISATIE=123443210
DOCUMENTEN_API=$OPEN_ZAAK_URL/documenten/api/v1/enkelvoudiginformatieobjecten

COUNT=5
WAIT_SECONDS=20
while getopts "n:w:h" option; do
  case $option in
    n) COUNT=$OPTARG ;;
    w) WAIT_SECONDS=$OPTARG ;;
    *) sed -n '6,22p' "$0" | sed 's/^# \{0,1\}//'; exit 1 ;;
  esac
done

WORK_DIR=$(mktemp -d)
trap 'rm -rf "$WORK_DIR"' EXIT

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

base64url() {
  openssl base64 -A | tr '+/' '-_' | tr -d '='
}

open_zaak_token() {
  local header claims signing_input signature
  header=$(printf '{"alg":"HS256","typ":"JWT","client_identifier":"%s"}' "$OPEN_ZAAK_CLIENT_ID" | base64url)
  claims=$(printf '{"iss":"%s","client_id":"%s","iat":%s}' "$OPEN_ZAAK_CLIENT_ID" "$OPEN_ZAAK_CLIENT_ID" "$(date +%s)" | base64url)
  signing_input="$header.$claims"
  signature=$(printf '%s' "$signing_input" | openssl dgst -sha256 -hmac "$OPEN_ZAAK_CLIENT_SECRET" -binary | base64url)
  echo "$signing_input.$signature"
}

keycloak_token() {
  curl -sS --fail-with-body "$KEYCLOAK_URL/realms/$KEYCLOAK_REALM/protocol/openid-connect/token" \
    -d grant_type=password -d client_id="$KEYCLOAK_CLIENT_ID" -d client_secret="$KEYCLOAK_CLIENT_SECRET" \
    -d username="$ZAC_USER" -d password="$ZAC_PASSWORD" | jq -r .access_token
}

inbox_titels() {
  curl -sS --fail-with-body -X PUT "$ZAC_URL/rest/inboxdocumenten" \
    -H "Authorization: Bearer $ZAC_TOKEN" -H "Content-Type: application/json" \
    -d "$(jq -n --arg titel "$RUN_ID" '{titel: $titel, page: 0, maxResults: 1000}')" | jq -r '.resultaten[].titel'
}

RUN_ID="inbox-document-$(date +%Y%m%d-%H%M%S)"
OPEN_ZAAK_TOKEN=$(open_zaak_token)
ZAC_TOKEN=$(keycloak_token) || fail "could not log in to Keycloak as '$ZAC_USER'"

# The Documenten API only accepts the informatieobjecttype url as Open Zaak itself serves it
INFORMATIEOBJECTTYPE_URL=$(curl -sS --fail-with-body \
  "$OPEN_ZAAK_URL/catalogi/api/v1/informatieobjecttypen/$INFORMATIEOBJECTTYPE_BIJLAGE_UUID" \
  -H "Authorization: Bearer $OPEN_ZAAK_TOKEN" | jq -r .url) || fail "could not read the informatieobjecttype from Open Zaak"

FILES=("$SCRIPT_DIR/documents/fäkeTestDocument.pdf" "$SCRIPT_DIR/documents/fakeWordDocument.docx")
FORMATEN=("application/pdf" "application/vnd.openxmlformats-officedocument.wordprocessingml.document")

echo "Creating $COUNT inbox documenten in Open Zaak ($RUN_ID-*)..."
TITELS=()
DOCUMENT_UUIDS=()
for ((sequence = 1; sequence <= COUNT; sequence++)); do
  index=$(((sequence - 1) % ${#FILES[@]}))
  file=${FILES[$index]}
  titel="$RUN_ID-$sequence"
  base64 < "$file" | tr -d '\n' > "$WORK_DIR/inhoud"
  jq -n --rawfile inhoud "$WORK_DIR/inhoud" \
    --arg bronorganisatie "$BRONORGANISATIE" --arg creatiedatum "$(date +%Y-%m-%d)" --arg titel "$titel" \
    --arg informatieobjecttype "$INFORMATIEOBJECTTYPE_URL" --arg bestandsnaam "$(basename "$file")" \
    --argjson bestandsomvang "$(wc -c < "$file" | tr -d ' ')" --arg formaat "${FORMATEN[$index]}" \
    '{bronorganisatie: $bronorganisatie, creatiedatum: $creatiedatum, titel: $titel, auteur: "test-data",
      taal: "dut", informatieobjecttype: $informatieobjecttype, inhoud: $inhoud, bestandsnaam: $bestandsnaam,
      bestandsomvang: $bestandsomvang, formaat: $formaat, vertrouwelijkheidaanduiding: "openbaar",
      status: "in_bewerking"}' > "$WORK_DIR/document.json"
  document_url=$(curl -sS --fail-with-body "$DOCUMENTEN_API" \
    -H "Authorization: Bearer $OPEN_ZAAK_TOKEN" -H "Content-Type: application/json" \
    -d @"$WORK_DIR/document.json" | jq -r .url) || fail "could not create '$titel' in Open Zaak"
  TITELS+=("$titel")
  DOCUMENT_UUIDS+=("${document_url##*/}")
  echo "  created $titel"
done

echo "Waiting up to ${WAIT_SECONDS}s for Open Notificaties to put them in the inbox..."
for ((second = 0; second < WAIT_SECONDS; second++)); do
  [ "$(inbox_titels | grep -c "^$RUN_ID-")" -ge "$COUNT" ] && break
  sleep 1
done

in_inbox=$(inbox_titels)
for ((index = 0; index < ${#TITELS[@]}; index++)); do
  titel=${TITELS[$index]}
  grep -qx "$titel" <<< "$in_inbox" && continue
  document_url="$DOCUMENTEN_API/${DOCUMENT_UUIDS[$index]}"
  curl -sS --fail-with-body "$ZAC_URL/rest/notificaties" \
    -H "Authorization: $NOTIFICATIONS_API_KEY" -H "Content-Type: application/json" \
    -d "$(jq -n --arg url "$document_url" --arg aanmaakdatum "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
      '{kanaal: "documenten", resource: "enkelvoudiginformatieobject", actie: "create",
        hoofdObject: $url, resourceUrl: $url, aanmaakdatum: $aanmaakdatum}')" > /dev/null \
    || fail "could not notify ZAC of '$titel'"
  echo "  notified ZAC of $titel directly"
done

echo
echo "Inbox documenten now in ZAC: $(inbox_titels | grep -c "^$RUN_ID-") of $COUNT"
echo "  $ZAC_URL/documenten/inbox"
echo "  http://localhost:4200/documenten/inbox"

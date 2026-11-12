#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Creates ontkoppelde documenten in the local kind stack: it creates a zaak, uploads documents to it
# and detaches them again. Optionally some of them are locked before they are detached, or removed from
# Open Zaak after they are detached, so the list also shows the "vergrendeld" and the
# "ontbreekt in Open Zaak" indicaties.
#
# A document is removed from the Open Zaak database directly, not through the Documenten API: Open Zaak
# then sends no notification, and ZAC keeps the ontkoppelde document, as happens when a document
# disappears from Open Zaak in production. Through the API, Open Notificaties would make ZAC remove it.
#
# Prerequisites: curl, jq and kubectl; the local kind stack running with ZAC, and a configured zaaktype
# that the user may create zaken of.
#
# Usage:
#   ./scripts/test-data/create-ontkoppelde-documenten.sh [-n COUNT] [-l LOCKED] [-m MISSING] [-z ZAAKTYPE_UUID]
#
#   -n COUNT          number of ontkoppelde documenten to create (default: 5)
#   -l LOCKED         how many of them to lock before detaching them (default: 0)
#   -m MISSING        how many of them to remove from Open Zaak after detaching them (default: 0)
#   -z ZAAKTYPE_UUID  zaaktype of the zaak the documents are detached from (default: Test zaaktype 2)
#
# Environment variables (defaults are those of the local kind stack):
#   ZAC_URL, KEYCLOAK_URL, ZAC_USER, ZAC_PASSWORD, KIND_CLUSTER_NAME
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)

ZAC_URL=${ZAC_URL:-http://localhost:8080}
KEYCLOAK_URL=${KEYCLOAK_URL:-http://localhost:8081}
ZAC_USER=${ZAC_USER:-beheerder1}
ZAC_PASSWORD=${ZAC_PASSWORD:-beheerder1}
KUBE_CONTEXT=kind-${KIND_CLUSTER_NAME:-zac}

# Values of the local kind stack, see .env.example
KEYCLOAK_REALM=zaakafhandelcomponent
KEYCLOAK_CLIENT_ID=zaakafhandelcomponent
KEYCLOAK_CLIENT_SECRET=keycloakZaakafhandelcomponentClientSecret

ZAAK_GROUP_ID=behandelaars-test-1
ZAAK_GROUP_NAME="Test group behandelaars domein test 1"

COUNT=5
LOCKED=0
MISSING=0
ZAAKTYPE_UUID=fd2bf643-c98a-4b00-b2b3-9ae0c41ed425
while getopts "n:l:m:z:h" option; do
  case $option in
    n) COUNT=$OPTARG ;;
    l) LOCKED=$OPTARG ;;
    m) MISSING=$OPTARG ;;
    z) ZAAKTYPE_UUID=$OPTARG ;;
    *) sed -n '6,28p' "$0" | sed 's/^# \{0,1\}//'; exit 1 ;;
  esac
done
[ $((LOCKED + MISSING)) -le "$COUNT" ] || { echo "ERROR: LOCKED + MISSING may not exceed COUNT" >&2; exit 1; }

# The bijlage informatieobjecttype of the test zaaktypes, see zac_testdata.py
case $ZAAKTYPE_UUID in
  8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a) INFORMATIEOBJECTTYPE_UUID=4a689f8a-11d3-4ddd-ae26-00fb258305a5 ;;
  4f46d270-c4d8-4cfe-a3a1-cb86ae102656) INFORMATIEOBJECTTYPE_UUID=efc332f2-be3b-4bad-9e3c-49a6219c92ad ;;
  *) INFORMATIEOBJECTTYPE_UUID=b1933137-94d6-49bc-9e12-afe712512276 ;;
esac

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

remove_from_open_zaak() {
  kubectl --context "$KUBE_CONTEXT" -n zac exec -i deploy/postgres -- \
    psql -q -U postgres -d openzaak -v ON_ERROR_STOP=1 -v uuid="$1" <<'SQL'
BEGIN;
CREATE TEMP TABLE canonical_ids AS
  SELECT canonical_id AS id FROM documenten_enkelvoudiginformatieobject WHERE uuid = :'uuid';
UPDATE documenten_enkelvoudiginformatieobjectcanonical SET latest_version_id = NULL
  WHERE id IN (SELECT id FROM canonical_ids);
DELETE FROM documenten_bestandsdeel WHERE informatieobject_id IN (SELECT id FROM canonical_ids);
DELETE FROM documenten_gebruiksrechten WHERE informatieobject_id IN (SELECT id FROM canonical_ids);
DELETE FROM documenten_objectinformatieobject WHERE informatieobject_id IN (SELECT id FROM canonical_ids);
DELETE FROM documenten_verzending WHERE informatieobject_id IN (SELECT id FROM canonical_ids);
DELETE FROM documenten_enkelvoudiginformatieobject WHERE canonical_id IN (SELECT id FROM canonical_ids);
DELETE FROM documenten_enkelvoudiginformatieobjectcanonical WHERE id IN (SELECT id FROM canonical_ids);
COMMIT;
SQL
}

ZAC_TOKEN=$(curl -sS --fail-with-body "$KEYCLOAK_URL/realms/$KEYCLOAK_REALM/protocol/openid-connect/token" \
  -d grant_type=password -d client_id="$KEYCLOAK_CLIENT_ID" -d client_secret="$KEYCLOAK_CLIENT_SECRET" \
  -d username="$ZAC_USER" -d password="$ZAC_PASSWORD" | jq -r .access_token) \
  || fail "could not log in to Keycloak as '$ZAC_USER'"

zac() {
  local method=$1 path=$2
  shift 2
  curl -sS --fail-with-body -X "$method" "$ZAC_URL/rest$path" -H "Authorization: Bearer $ZAC_TOKEN" "$@"
}

RUN_ID="ontkoppeld-document-$(date +%Y%m%d-%H%M%S)"

echo "Creating a zaak of zaaktype $ZAAKTYPE_UUID..."
ZAAK_IDENTIFICATIE=$(zac POST /zaken/zaak -H "Content-Type: application/json" -d "$(jq -n \
  --arg zaaktype "$ZAAKTYPE_UUID" --arg startdatum "$(date +%Y-%m-%dT%H:%M:%S%z | sed -E 's/([0-9]{2})([0-9]{2})$/\1:\2/')" \
  --arg groep_id "$ZAAK_GROUP_ID" --arg groep_naam "$ZAAK_GROUP_NAME" --arg omschrijving "$RUN_ID" \
  '{zaak: {zaaktype: {uuid: $zaaktype}, startdatum: $startdatum, groep: {id: $groep_id, naam: $groep_naam},
    communicatiekanaal: "fakeCommunicatiekanaal1", vertrouwelijkheidaanduiding: "OPENBAAR",
    omschrijving: $omschrijving, toelichting: "Created by create-ontkoppelde-documenten.sh"}, bagObjecten: []}')" \
  | jq -r .identificatie) || fail "could not create the zaak; is zaaktype $ZAAKTYPE_UUID configured in ZAC?"
ZAAK_UUID=$(zac GET "/zaken/zaak/id/$ZAAK_IDENTIFICATIE" | jq -r .uuid) || fail "could not read zaak $ZAAK_IDENTIFICATIE"
echo "  created $ZAAK_IDENTIFICATIE"

FILES=("$SCRIPT_DIR/documents/fäkeTestDocument.pdf" "$SCRIPT_DIR/documents/fakeWordDocument.docx")
FORMATEN=("application/pdf" "application/vnd.openxmlformats-officedocument.wordprocessingml.document")

echo "Uploading $COUNT documents to $ZAAK_IDENTIFICATIE..."
DOCUMENT_UUIDS=()
for ((sequence = 1; sequence <= COUNT; sequence++)); do
  index=$(((sequence - 1) % ${#FILES[@]}))
  file=${FILES[$index]}
  # ZAC only accepts the creatiedatum with minute precision
  document_uuid=$(zac POST "/informatieobjecten/informatieobject/$ZAAK_UUID/$ZAAK_UUID" \
    -F "bestandsnaam=$(basename "$file")" -F "titel=$RUN_ID-$sequence" \
    -F "bestandsomvang=$(wc -c < "$file" | tr -d ' ')" -F "formaat=${FORMATEN[$index]}" \
    -F "informatieobjectTypeUUID=$INFORMATIEOBJECTTYPE_UUID" -F "vertrouwelijkheidaanduiding=OPENBAAR" \
    -F "status=in_bewerking" -F "creatiedatum=$(date +%Y-%m-%dT%H:%M%z | sed -E 's/([0-9]{2})([0-9]{2})$/\1:\2/')" \
    -F "auteur=test-data" -F "taal=dut" -F "file=@$file;type=${FORMATEN[$index]}" \
    | jq -r .uuid) || fail "could not upload $RUN_ID-$sequence"
  DOCUMENT_UUIDS+=("$document_uuid")
done

for ((index = 0; index < LOCKED; index++)); do
  zac POST "/informatieobjecten/informatieobject/${DOCUMENT_UUIDS[$index]}/lock?zaak=$ZAAK_UUID" > /dev/null \
    || fail "could not lock $RUN_ID-$((index + 1))"
  echo "  locked $RUN_ID-$((index + 1))"
done

echo "Detaching the documents..."
for document_uuid in "${DOCUMENT_UUIDS[@]}"; do
  zac PUT /zaken/zaakinformatieobjecten/ontkoppel -H "Content-Type: application/json" -d "$(jq -n \
    --arg zaak "$ZAAK_UUID" --arg document "$document_uuid" \
    '{zaakUUID: $zaak, documentUUID: $document, reden: "Created by create-ontkoppelde-documenten.sh"}')" > /dev/null \
    || fail "could not detach document $document_uuid"
done

for ((index = COUNT - MISSING; index < COUNT; index++)); do
  remove_from_open_zaak "${DOCUMENT_UUIDS[$index]}" > /dev/null || fail "could not remove $RUN_ID-$((index + 1)) from Open Zaak"
  echo "  removed $RUN_ID-$((index + 1)) from Open Zaak"
done

echo
echo "Created $COUNT ontkoppelde documenten ($RUN_ID-*): $LOCKED locked, $MISSING missing in Open Zaak"
echo "  $ZAC_URL/documenten/ontkoppelde"
echo "  http://localhost:4200/documenten/ontkoppelde"

#!/usr/bin/env bash

#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
set -e

help()
{
   echo "Deletes the zaak with the given zaaknummer (identificatie) from Open Zaak, using the ZGW Zaken API."
   echo "Open Zaak also deletes the related objects of the zaak, such as statussen, rollen and zaakinformatieobjecten."
   echo "Requires curl, openssl and jq."
   echo
   echo "Syntax: $0 [-u|c|s|y|h] <zaaknummer>"
   echo "options:"
   echo "-u     Base Open Zaak URL. Defaults to 'http://localhost:8001'."
   echo "-c     Open Zaak client ID. Defaults to 'zac_client'."
   echo "-s     Open Zaak client secret. Defaults to 'openzaakZaakafhandelcomponentClientSecret'."
   echo "-y     Delete the zaak without asking for confirmation."
   echo "-h     Print this help."
   echo
}

echoerr() {
  echo 1>&2;
  echo "$@" 1>&2;
  echo 1>&2;
}

base64url() {
  openssl base64 -A | tr '+/' '-_' | tr -d '='
}

createJwt() {
  local header payload unsignedToken signature
  header=$(printf '{"alg":"HS256","typ":"JWT"}' | base64url)
  payload=$(printf '{"iss":"%s","iat":%s,"client_id":"%s","user_id":"%s","user_representation":"%s"}' \
    "$clientId" "$(date +%s)" "$clientId" "$clientId" "$clientId" | base64url)
  unsignedToken="$header.$payload"
  signature=$(printf '%s' "$unsignedToken" | openssl dgst -binary -sha256 -hmac "$clientSecret" | base64url)
  echo "$unsignedToken.$signature"
}

openZaakBaseURL="http://localhost:8001"
clientId="zac_client"
clientSecret="openzaakZaakafhandelcomponentClientSecret"
skipConfirmation=false

while getopts 'u:c:s:yh' OPTION; do
  case $OPTION in
    u)
      openZaakBaseURL=$OPTARG
      ;;
    c)
      clientId=$OPTARG
      ;;
    s)
      clientSecret=$OPTARG
      ;;
    y)
      skipConfirmation=true
      ;;
    h)
      help
      exit;;
    \?)
      echoerr "Error: Invalid option"
      help
      exit 1;;
  esac
done
shift $((OPTIND - 1))

zaaknummer=$1
if [ -z "$zaaknummer" ]; then
  echoerr "Error: Missing zaaknummer"
  help
  exit 1
fi

for command in curl openssl jq; do
  if ! command -v "$command" > /dev/null; then
    echoerr "Error: '$command' is not installed"
    exit 1
  fi
done

jwt=$(createJwt)

echo "Searching for zaak '$zaaknummer' in Open Zaak on '$openZaakBaseURL' ..."
zaken=$(curl --silent --show-error --fail \
  -H "Authorization: Bearer $jwt" \
  -H "Accept-Crs: EPSG:4326" \
  --get --data-urlencode "identificatie=$zaaknummer" \
  "$openZaakBaseURL/zaken/api/v1/zaken")

numberOfZaken=$(echo "$zaken" | jq '.count')
if [ "$numberOfZaken" -ne 1 ]; then
  echoerr "Error: Expected exactly one zaak with zaaknummer '$zaaknummer', but found $numberOfZaken"
  exit 1
fi
zaakUrl=$(echo "$zaken" | jq -r '.results[0].url')
echo "Found zaak: $zaakUrl"

if [ "$skipConfirmation" = false ]; then
  read -r -p "Delete this zaak? This cannot be undone. [y/N] " answer
  if [ "$answer" != "y" ] && [ "$answer" != "Y" ]; then
    echo "Zaak not deleted."
    exit
  fi
fi

responseFile=$(mktemp)
httpStatus=$(curl --silent --show-error \
  -X DELETE \
  -H "Authorization: Bearer $jwt" \
  --output "$responseFile" \
  --write-out "%{http_code}" \
  "$zaakUrl")
if [ "$httpStatus" != "204" ]; then
  echoerr "Error: Open Zaak returned HTTP status $httpStatus. The response body is in '$responseFile'."
  exit 1
fi
rm "$responseFile"
echo "Deleted zaak '$zaaknummer'."

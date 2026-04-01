#!/usr/bin/env bash

#
# SPDX-FileCopyrightText: 2025 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
set -e

help()
{
   echo "Notifies ZAC about a product request. Note that the ZAC used endpoint requires API key authentication."
   echo
   echo "Syntax: $0 [-u|o|k|a|b|h]"
   echo "options:"
   echo "-u     Base ZAC URL. Defaults to 'http://localhost:8080'."
   echo "-o     Base Objecten API URL. Defaults to 'http://host.docker.internal:8010'"
   echo "-k     ZAC internal endpoints API key. Defaults to 'openNotificatiesApiSecretKey'."
   echo "-a     Use alternative object for application-specific email address test."
   echo "-b     Use alternative object for application-specific email address test with betrokkene."
   echo "-h     Print this help."
   echo
}

echoerr() {
  echo 1>&2;
  echo "$@" 1>&2;
  echo 1>&2;
}

zacBaseURL="http://localhost:8080"
objectenAPIURL="http://host.docker.internal:8010"
openNotificatiesApiSecretKey="openNotificatiesApiSecretKey"
objectUuid="7d23e7ad-4b9e-4cbf-a5fb-75aa4100fa4e"
altAObjectUuid="5658d286-9a84-4cde-b9af-6771bd599a06"
altBObjectUuid="a3278b18-0562-48cd-ab9b-ee05f2d433bb"

while getopts 'u:o:k:abh' OPTION; do
  case $OPTION in
    u)
      zacBaseURL=$OPTARG
      ;;
    o)
      objectenAPIURL=$OPTARG
      ;;
    k)
      openNotificatiesApiSecretKey=$OPTARG
      ;;
    a)
      objectUuid=$altAObjectUuid
      ;;
    b)
      objectUuid=$altBObjectUuid
      ;;
    h)
      help
      exit;;
    \?)
      echoerr "Error: Invalid option"
      help
      exit;;
  esac
done

echo "Checking Objecten API on $objectenAPIURL ..."
curl --silent --show-error --fail "$objectenAPIURL"/api/v2 > /dev/null

notification="{
  \"kanaal\": \"objecten\",
  \"resource\": \"object\",
  \"resourceUrl\": \"$objectenAPIURL/$objectUuid\",
  \"hoofdObject\": \"$objectenAPIURL/$objectUuid\",
  \"actie\": \"create\",
  \"aanmaakdatum\": \"$(date +'%Y-%m-%dT%H:%M:%S.000000Z[Europe/Amsterdam]')\",
  \"kenmerken\": {
    \"objectType\": \"$objectenAPIURL/021f685e-9482-4620-b157-34cd4003da6b\"
  }
}"

echo "Sending notification to ZAC using base URL: '$zacBaseURL'."
curl -i \
  -H "Content-Type: application/json" \
  -H "Authorization: ${openNotificatiesApiSecretKey}" \
  --data "$notification" \
  "${zacBaseURL}"/rest/notificaties


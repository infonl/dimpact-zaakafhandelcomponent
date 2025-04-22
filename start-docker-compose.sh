#!/bin/bash

set -e

#
# SPDX-FileCopyrightText: 2023 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#

help()
{
   echo "Starts the ZAC Docker Compose environment using the 1Password CLI tools to retrieve secrets."
   echo
   echo "Syntax: $0 [-d|h|z|b|l|m|t|o|n|a]"
   echo
   echo "General:"
   echo "   -d     Delete local Docker volume data before starting Docker Compose."
   echo "   -h     Print this Help."
   echo
   echo "ZAC options:"
   echo "   -z     Start last-known-good ZAC Docker container."
   echo "   -b     Build and start local ZAC Docker image."
   echo "   -l     Start locally built ZAC Docker image."
   echo
   echo "Additional components:"
   echo "   -m     Start the containers used for handling metrics and traces."
   echo "   -t     Start containers used for integration testing."
   echo "   -o     Start Objecten (required e.g. for the Dimpact productaanvraag flow)."
   echo "   -n     Start OpenNotificaties."
   echo "   -a     Start OpenArchiefbeheer."
   echo
}

echoerr() {
  echo 1>&2;
  echo "$@" 1>&2;
  echo 1>&2;
}

volumeDataFolder="./scripts/docker-compose/volume-data"
pullZac=false
buildZac=false
localZac=false
disableZacOpenTelemetry=true
profiles=()

[ -f fix-permissions.sh ] && ./fix-permissions.sh

while getopts ':dhzblmtona' OPTION; do
  case $OPTION in
    d)
      echo "Deleting local Docker volume data folder: '$volumeDataFolder'.."
      rm -rf $volumeDataFolder
      echo "Done"
      ;;
    h)
      help
      exit;;
    z)
      profiles+=("zac")
      pullZac=true
      ;;
    b)
      profiles+=("zac")
      buildZac=true
      ;;
    l)
      profiles+=("zac")
      localZac=true
      ;;
    m)
      profiles+=("metrics")
      disableZacOpenTelemetry=false
      ;;
    t)
      profiles+=("itest")
      ;;
    o)
      profiles+=("objecten")
      ;;
    n)
      profiles+=("opennotificaties")
      export OPENZAAK_NOTIFICATIONS_DISABLED=false
      ;;
    a)
      profiles+=("openarchiefbeheer")
      ;;
    \?)
      echoerr "Error: Invalid option"
      help
      exit;;
  esac
done

if [ "$pullZac" = "true" ] && [ "$buildZac" = "true" ]; then
    echoerr "Only one of -z and -b can be specified!"
    exit 1
fi
if [ "$pullZac" = "true" ] && [ "$localZac" = "true" ]; then
    echoerr "Only one of -z and -l can be specified!"
    exit 1
fi
if [ "$localZac" = "true" ] && [ "$buildZac" = "true" ]; then
    echoerr "Only one of -b and -l can be specified!"
    exit 1
fi

if [ "$buildZac" = "true" ]; then
    echo "Building ZAC Docker Image ..."
    ./gradlew buildDockerImage
    export ZAC_DOCKER_IMAGE=ghcr.io/infonl/zaakafhandelcomponent:dev
fi
if [ "$localZac" = "true" ]; then
    echo "Using local ZAC Docker Image ..."
    export ZAC_DOCKER_IMAGE=ghcr.io/infonl/zaakafhandelcomponent:dev
fi
if [ "$pullZac" = "true" ]; then
    echo "Pulling latest ZAC Docker Image ..."
    docker compose pull zac
fi

# Ensure that Docker Compose volume-data directories are created with current user
mkdir -p $volumeDataFolder/openklant-database-data
mkdir -p $volumeDataFolder/openzaak-database-data
mkdir -p $volumeDataFolder/opennotificaties-database-data
mkdir -p $volumeDataFolder/zac-keycloak-database-data
mkdir -p $volumeDataFolder/solr-data
mkdir -p $volumeDataFolder/zac-database-data
mkdir -p $volumeDataFolder/zac-keycloak-database-data
mkdir -p $volumeDataFolder/zgw-referentielijsten-database-data

# Build comma separated profile list
profilesList=""
if [ ${#profiles[@]} -ne 0 ]; then
  printf -v concatenated_profiles '%s,' "${profiles[@]}"
  profilesList="${concatenated_profiles%,}"
fi

# Uses the 1Password CLI tools to set up the environment variables for running Docker Compose and ZAC in IntelliJ.
# Please see docs/INSTALL.md for details on how to use this script.
echo "Starting Docker Compose environment with profiles [$profilesList] ..."
export APP_ENV=devlocal && export COMPOSE_PROFILES=$profilesList && export OTEL_SDK_DISABLED=disableZacOpenTelemetry && op run --env-file="./.env.tpl" --no-masking -- docker compose --project-name zac up -d

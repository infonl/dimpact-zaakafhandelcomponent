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
   echo "Syntax: $0 [-d|m|t|z|b|l|o|h]"
   echo "options:"
   echo "-d     Delete local Docker volume data before starting Docker Compose."
   echo "-m     Also enable tracing and start the containers used for handling metrics and traces"
   echo "-t     Also enable containers used for integration testing"
   echo "-z     Also start last-known-good ZAC Docker container as part of the Docker Compose environment."
   echo "-b     Build and start local ZAC Docker image in the Docker Compose environment."
   echo "-l     Start local ZAC Docker image in the Docker Compose environment."
   echo "-o     Also start OpenNotificaties in the Docker Compose environment."
   echo "-h     Print this Help."
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
enableZacOpenTelemetrySampler=off
profiles=()

[ -f fix-permissions.sh ] && ./fix-permissions.sh

while getopts ':dmtzbloh' OPTION; do
  case $OPTION in
    d)
      echo "Deleting local Docker volume data folder: '$volumeDataFolder'.."
      rm -rf $volumeDataFolder
      echo "Done"
      ;;
    h)
      help
      exit;;
    t)
      echo "Also enabling containers used for integration testing"
      profiles+=("itest")
      ;;
    m)
      echo "Also enabling tracing and starting containers used for handling metrics and traces"
      profiles+=("metrics")
      enableZacOpenTelemetrySampler=on
      ;;
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
    o)
      echo "Also starting OpenNotificaties"
      profiles+=("opennotificaties")
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

# Ensure that volume-data is created with current user
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
export APP_ENV=devlocal && export COMPOSE_PROFILES=$profilesList && export SUBSYSTEM_OPENTELEMETRY__SAMPLER_TYPE=$enableZacOpenTelemetrySampler && op run --env-file="./.env.tpl" --no-masking -- docker compose --project-name zac up -d

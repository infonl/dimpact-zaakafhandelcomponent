#!/bin/bash

set -e

#
# SPDX-FileCopyrightText: 2023 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

help()
{
   echo "Starts the ZAC Podman Compose environment using the 1Password CLI tools to retrieve secrets."
   echo
   echo "Syntax: $0 [-d|e|h|z|b|l|m|t|o|n|a|f]"
   echo
   echo "General:"
   echo "   -d     Delete local Podman volume data before starting Podman Compose."
   echo "   -e     Run Podman Compose without using the 1Password CLI tools to retrieve secrets (instead, environment variables must be set manually)."
   echo "   -h     Print this Help."
   echo
   echo "ZAC options:"
   echo "   -z     Start last-known-good ZAC container image."
   echo "   -b     Build and start local ZAC container image."
   echo "   -l     Start locally built ZAC container image."
   echo
   echo "Additional components:"
   echo "   -m     Start the containers used for handling metrics and traces."
   echo "   -t     Start containers used for integration testing."
   echo "   -o     Start Objecten (required e.g. for the Dimpact productaanvraag flow)."
   echo "   -n     Start OpenNotificaties."
   echo "   -a     Start OpenArchiefbeheer."
   echo "   -f     Start Open Formulieren (also starts Objecten and OpenNotificaties automatically)."
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
disableOnePassword=false
profiles=()

[ -f fix-permissions.sh ] && ./fix-permissions.sh

while getopts ':dhzblmtonafe' OPTION; do
  case $OPTION in
    d)
      echo "Deleting local Podman volume data folder: '$volumeDataFolder'.."
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
    e)
      echo "Running without 1Password CLI tools to retrieve secrets. Make sure to set environment variables manually."
      disableOnePassword=true
      ;;
    f)
      profiles+=("openformulieren")
      # Objecten and OpenNotificaties services start automatically via their profile lists.
      # Notifications must be enabled in Open Zaak for the productaanvraag flow to work.
      export OPENZAAK_NOTIFICATIONS_DISABLED=false
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
    echo "Building ZAC container image ..."
    ./gradlew buildDockerImage
    export ZAC_DOCKER_IMAGE=ghcr.io/infonl/zaakafhandelcomponent:dev
fi
if [ "$localZac" = "true" ]; then
    echo "Using local ZAC container image ..."
    export ZAC_DOCKER_IMAGE=ghcr.io/infonl/zaakafhandelcomponent:dev
fi
if [ "$pullZac" = "true" ]; then
    echo "Pulling latest ZAC container image ..."
    podman compose pull zac
fi

# Ensure that Podman Compose volume-data directories are created with current user
mkdir -p $volumeDataFolder/openklant-database-data
mkdir -p $volumeDataFolder/openzaak-database-data
mkdir -p $volumeDataFolder/opennotificaties-database-data
mkdir -p $volumeDataFolder/openformulieren-database-data
mkdir -p $volumeDataFolder/zac-keycloak-database-data
mkdir -p $volumeDataFolder/solr-data
mkdir -p $volumeDataFolder/zac-database-data
mkdir -p $volumeDataFolder/zac-keycloak-database-data

# Build comma separated profile list
profilesList=""
if [ ${#profiles[@]} -ne 0 ]; then
  printf -v concatenated_profiles '%s,' "${profiles[@]}"
  profilesList="${concatenated_profiles%,}"
fi

# Uses the 1Password CLI tools to set up the environment variables for running Podman Compose and ZAC in IntelliJ.
# Please see docs/INSTALL.md for details on how to use this script.
echo "Starting Podman Compose environment with profiles [$profilesList] ..."
compose_files=""
if [ -n "${DOCKER_USE_ARM64_CONTAINERS:-}" ]; then
  echo "Using arm64 containers ..."
  compose_files="-f docker-compose.yaml"
  if [ -f docker-compose.override.yml ]; then
    compose_files="$compose_files -f docker-compose.override.yml"
  fi
  compose_files="$compose_files -f docker-compose.arm64-override.yaml"
fi

op_script=""
if [ "$disableOnePassword" = "false" ]; then
  if ! command -v op >/dev/null 2>&1; then
    echo "1Password CLI ('op') not found. Only using environment variables set manually."
  elif op vault get Dimpact; then
    echo "Using 1Password CLI tools to retrieve secrets from vault 'Dimpact'..."
    op_script='op run --env-file=./.env.tpl --no-masking --'
  else
    echo "No access to 1Password vault 'Dimpact'. Only using environment variables set manually."
  fi
fi

export APP_ENV=devlocal && export COMPOSE_PROFILES=$profilesList && export OTEL_SDK_DISABLED=$disableZacOpenTelemetry && $op_script podman compose $compose_files --project-name zac up -d
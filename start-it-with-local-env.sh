#!/bin/sh

#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

set -e

help() {
   echo "Starts the integration tests with a local ZAC container image."
   echo
   echo "Syntax: $0 [-b|d|s|u|h]"
   echo "options:"
   echo "-b     Build a local ZAC container image"
   echo "-d     Delete local Podman volume data before starting Podman Compose"
   echo "-c     No-op under Podman: containers are always left running after test execution,"
   echo "       since disabling the privileged Ryuk cleanup container is mandatory under rootless Podman"
   echo "-s     Do not start Podman Compose containers before test execution"
   echo "-u     Turn on debug logs"
   echo "-h     Print this Help"
   echo
}

echoerr() {
  echo 1>&2;
  echo "$@" 1>&2;
  echo 1>&2;
}

volumeDataFolder="./scripts/docker-compose/volume-data"
args=""

[ -f fix-permissions.sh ] && ./fix-permissions.sh

# Point TestContainers at the Podman socket if DOCKER_HOST isn't already set.
if [ -z "${DOCKER_HOST:-}" ] && command -v podman >/dev/null 2>&1; then
  podmanSocket=$(podman machine inspect --format '{{.ConnectionInfo.PodmanSocket.Path}}' 2>/dev/null || true)
  [ -z "$podmanSocket" ] && podmanSocket="/run/user/$(id -u)/podman/podman.sock"
  export DOCKER_HOST="unix://${podmanSocket}"
  echo "Detected Podman - setting DOCKER_HOST=${DOCKER_HOST}"
fi

# Rootless Podman cannot run the privileged Ryuk resource-reaper container, so Ryuk must always be
# disabled when running against Podman. This also means TestContainers will not clean up the Compose
# stack automatically after the tests finish - run ./stop-docker-compose.sh manually afterwards, or use
# the -c option below which does exactly the same thing under Podman.
export TESTCONTAINERS_RYUK_DISABLED=true

build=false
while getopts ':bdcsurh' OPTION; do
  case "$OPTION" in
    b)
      build=true
      ;;
    d)
      echo "Deleting local Podman volume data folder: '$volumeDataFolder'.."
      rm -rf $volumeDataFolder
      echo "Done"
      ;;
    c)
      echo "No-op: Podman Compose containers cleanup is already disabled by default under Podman."
      ;;
    s)
      echo "Disabling Podman Compose containers startup ..."
      export DO_NOT_START_DOCKER_COMPOSE=true
      ;;
    u)
      echo "Turning on debug logs ..."
      args="$args -Si -Dorg.gradle.vfs.watch=true"
      ;;
    h)
      help
      exit;;
    \?)
      echoerr "Error: Invalid option $OPTION"
      help
      exit;;
  esac
done

if [ $build = "true" ]; then
  echo "Building fresh ZAC container image ..."
  # shellcheck disable=SC2086
  ./gradlew $args clean buildDockerImage
fi

export ZAC_DOCKER_IMAGE=ghcr.io/infonl/zaakafhandelcomponent:dev
[ -f check-for-running-containers.sh ] && ./check-for-running-containers.sh

# shellcheck disable=SC2086
./gradlew $args itest

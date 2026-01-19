#!/bin/sh

#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

set -e

help() {
   echo "Starts the integration tests with a local ZAC Docker Image."
   echo
   echo "Syntax: $0 [-b|d|s|u|h]"
   echo "options:"
   echo "-b     Build a local ZAC Docker image"
   echo "-d     Delete local Docker volume data before starting Docker Compose"
   echo "-c     Keep local Docker Compose containers running after test execution"
   echo "-s     Do not start Docker Compose containers before test execution"
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

build=false
while getopts ':bdcsurh' OPTION; do
  case "$OPTION" in
    b)
      build=true
      ;;
    d)
      echo "Deleting local Docker volume data folder: '$volumeDataFolder'.."
      rm -rf $volumeDataFolder
      echo "Done"
      ;;
    c)
      echo "Disabling Docker Compose containers cleanup ..."
      export TESTCONTAINERS_RYUK_DISABLED=true
      ;;
    s)
      echo "Disabling Docker Compose containers startup ..."
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
  echo "Building fresh ZAC Docker Image ..."
  # shellcheck disable=SC2086
  ./gradlew $args clean buildDockerImage
fi

export ZAC_DOCKER_IMAGE=ghcr.io/infonl/zaakafhandelcomponent:dev
[ -f check-for-running-containers.sh ] && ./check-for-running-containers.sh

# shellcheck disable=SC2086
./gradlew $args itest

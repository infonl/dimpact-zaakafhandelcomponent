#!/bin/sh

#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

set -e

help() {
   echo "Starts the integration tests with a local ZAC container image."
   echo
   echo "Syntax: $0 [-b|d|c|s|u|h]"
   echo "options:"
   echo "-b     Build a local ZAC container image"
   echo "-d     Delete local Podman volume data before starting Podman Compose"
   echo "-c     Keep local Podman Compose containers running after test execution"
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

# DOCKER_HOST (pointed at the Podman socket) and TESTCONTAINERS_RYUK_DISABLED (rootless Podman can't run
# the privileged Ryuk resource-reaper container) are auto-detected and exported onto the itest JVM's
# environment by the `itest` Gradle task itself (see detectPodmanDockerHost() in build.gradle.kts), so
# they don't need to be set here. Compose containers are still stopped automatically after the tests
# finish (ZacItestProjectConfig.kt does this itself, independently of Ryuk) - use the -c option below to
# keep them running instead, e.g. to inspect state or reuse the stack across repeated runs.

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
      echo "Keeping Podman Compose containers running after test execution ..."
      export KEEP_ITEST_CONTAINERS_RUNNING=true
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

#!/bin/bash

#
# SPDX-FileCopyrightText: 2024, 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

set -e

help() {
   echo "Starts the integration tests with a local ZAC Docker Image, against the kind cluster of the integration tests."
   echo
   echo "Syntax: $0 [-b|c|d|s|u|h]"
   echo "options:"
   echo "-b     Build a local ZAC Docker image"
   echo "-d     Delete the kind cluster of the integration tests before the tests start"
   echo "-c     Keep the kind cluster of the integration tests after test execution"
   echo "-s     Do not start the kind cluster: run against the cluster that -c kept"
   echo "-u     Turn on debug logs"
   echo "-h     Print this Help"
   echo
   echo "Environment variables:"
   echo "   ZAC_ITEST_KIND_CLUSTER_NAME  name of the kind cluster of the integration tests (default: zac-itest)"
   echo "   KIND_PORT_OFFSET             added to every host port of the cluster (default: 0)"
}

echoerr() {
  echo 1>&2;
  echo "$@" 1>&2;
  echo 1>&2;
}

args=""

build=false
while getopts ':bdcsuh' OPTION; do
  case "$OPTION" in
    b)
      build=true
      ;;
    d)
      KIND_CLUSTER_NAME=${ZAC_ITEST_KIND_CLUSTER_NAME:-zac-itest} DELETE_CLUSTER=true ./scripts/kind/down.sh
      ;;
    c)
      echo "Keeping the kind cluster after the tests ..."
      export ZAC_ITEST_KEEP_STACK=true
      ;;
    s)
      echo "Using the running kind cluster ..."
      export ZAC_ITEST_USE_RUNNING_STACK=true
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

# shellcheck disable=SC2086
./gradlew $args itest

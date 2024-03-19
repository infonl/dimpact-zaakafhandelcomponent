#!/bin/sh

#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#

set -e

help() {
   echo "Starts the Integration Tests with local docker image."
   echo
   echo "Syntax: $0 [-l|h]"
   echo "options:"
   echo "-l     Build a local ZAC Docker image."
   echo "-h     Print this Help."
   echo
}

while getopts ':lh' OPTION; do
  case $OPTION in
    h)
      help
      exit;;
    l)
      echo "Building ZAC Docker Image ..."
      ./gradlew buildDockerImage
      ;;
    \?)
      echo "Error: Invalid option"
      help
      exit;;
  esac
done

if sudo lsof -i:8080; then
  echo "Please stop the currently running WildFly!"
  exit 1
fi

VOLUMES_DIR=scripts/docker-compose/volume-data
if [ -d $VOLUMES_DIR ] && [ "$(stat -c "%U %G" $VOLUMES_DIR)" != "$USER:$GROUP" ]; then
  sudo chown -R "$USER:$GROUP" $VOLUMES_DIR
fi

export ZAC_DOCKER_IMAGE=ghcr.io/infonl/zaakafhandelcomponent:dev
./gradlew itest

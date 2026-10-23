#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
set -euo pipefail

help() {
  echo "Starts the local ZAC development stack in a kind cluster, as an alternative to ./start-docker-compose.sh."
  echo "See docs/development/installKind.md."
  echo
  echo "Syntax: $0 [-z|-b|-l] [-d] [-h]"
  echo
  echo "Without -z, -b or -l everything but ZAC starts, so that ZAC can run from an IDE against the cluster."
  echo
  echo "   -z     Start ZAC from the image in the registry (\$ZAC_DOCKER_IMAGE, default ghcr.io/infonl/zaakafhandelcomponent:latest)."
  echo "   -b     Build the ZAC Docker image and start it."
  echo "   -l     Start the locally built ZAC Docker image (ghcr.io/infonl/zaakafhandelcomponent:dev)."
  echo "   -d     Delete the kind cluster and its data before starting, to start from the seed data again."
  echo "   -h     Print this help."
  echo
  echo "Environment variables:"
  echo "   KIND_CLUSTER_NAME    name of the kind cluster (default: zac)"
  echo "   KIND_PORT_OFFSET     added to every host port, to run next to Docker Compose (default: 0)"
  echo "   KIND_PORT_<NAME>     the host port of one service, or 0 to not publish it, for example KIND_PORT_ZAC=18080"
}

zacInCluster=""
deleteCluster=false
while getopts ':zbldh' option; do
  case $option in
    z) zacInCluster=registry ;;
    b) zacInCluster=build ;;
    l) zacInCluster=local ;;
    d) deleteCluster=true ;;
    h) help; exit ;;
    \?) echo "Invalid option: -$OPTARG" >&2; help >&2; exit 1 ;;
  esac
done

cd "$(dirname "$0")"
if [ "$deleteCluster" = true ]; then
  DELETE_CLUSTER=true ./scripts/kind/down.sh
fi
if [ "$zacInCluster" = build ]; then
  echo "Building the ZAC Docker image ..."
  ./gradlew buildDockerImage
  zacInCluster=local
fi
if [ "$zacInCluster" = local ]; then
  export ZAC_DOCKER_IMAGE=ghcr.io/infonl/zaakafhandelcomponent:dev
fi
ZAC_IN_CLUSTER=$zacInCluster exec ./scripts/kind/up.sh

#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
set -euo pipefail

help() {
  echo "Stops the local ZAC development stack in kind. ./start-kind.sh starts it again with its data."
  echo
  echo "Syntax: $0 [-d] [-h]"
  echo
  echo "   -d     Delete the kind cluster and its data instead."
  echo "   -h     Print this help."
  echo
  echo "Environment variables:"
  echo "   KIND_CLUSTER_NAME    name of the kind cluster (default: zac)"
}

deleteCluster=false
while getopts ':dh' option; do
  case $option in
    d) deleteCluster=true ;;
    h) help; exit ;;
    \?) echo "Invalid option: -$OPTARG" >&2; help >&2; exit 1 ;;
  esac
done

DELETE_CLUSTER=$deleteCluster "$(dirname "$0")/scripts/kind/down.sh"
if [ "$deleteCluster" = false ]; then
  echo "Stopped. Docker keeps the container '${KIND_CLUSTER_NAME:-zac}-control-plane' with the data of the cluster:"
  echo "./start-kind.sh starts it again, ./stop-kind.sh -d deletes it."
fi

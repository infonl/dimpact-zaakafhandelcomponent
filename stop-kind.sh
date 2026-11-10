#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
set -euo pipefail

help() {
  echo "Stops the local ZAC development stack in kind. ./start-kind.sh starts it again with its data."
  echo
  echo "Syntax: $0 [-d|D|h]"
  echo
  echo "   -d     Clear the data of the cluster (Postgres, Solr, Open Zaak documents) before stopping it."
  echo "          ./start-kind.sh then starts from the seed data again."
  echo "   -D     Delete the kind cluster and its data instead."
  echo "   -h     Print this help."
  echo
  echo "Environment variables:"
  echo "   KIND_CLUSTER_NAME    name of the kind cluster (default: zac)"
}

clearData=false
deleteCluster=false
while getopts ':dDh' option; do
  case $option in
    d) clearData=true ;;
    D) deleteCluster=true ;;
    h) help; exit ;;
    \?) echo "Invalid option: -$OPTARG" >&2; help >&2; exit 1 ;;
  esac
done

cd "$(dirname "$0")"
if [ "$deleteCluster" = false ] && [ "$clearData" = true ]; then
  ./scripts/kind/clear-data.sh
fi
DELETE_CLUSTER=$deleteCluster ./scripts/kind/down.sh
if [ "$deleteCluster" = false ]; then
  echo "Stopped. Docker keeps the container '${KIND_CLUSTER_NAME:-zac}-control-plane' with the cluster:"
  echo "./start-kind.sh starts it again, ./stop-kind.sh -D deletes it."
fi

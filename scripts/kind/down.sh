#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Stops the local ZAC development stack in kind, keeping its data, or deletes it with DELETE_CLUSTER=true.
# Usually started through ./stop-kind.sh.
set -euo pipefail

CLUSTER=${KIND_CLUSTER_NAME:-zac}

if [ "${DELETE_CLUSTER:-false}" = true ]; then
  echo "Deleting the kind cluster '$CLUSTER' and its data ..."
  docker rm --force "$CLUSTER-ports" > /dev/null 2>&1 || true
  kind delete cluster --name "$CLUSTER"
else
  echo "Stopping the kind cluster '$CLUSTER' ..."
  docker stop "$CLUSTER-ports" "$CLUSTER-control-plane" > /dev/null
fi

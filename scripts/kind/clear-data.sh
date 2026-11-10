#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Clears the data of the local ZAC development stack in kind and keeps the cluster, so that the next start of
# scripts/kind/up.sh loads the seed data again. It removes the workloads and the persistent volumes of the namespace,
# such as postgres-data, solr-data and openzaak-media; up.sh creates them again from the Helm releases.
# Usually started through ./start-kind.sh -d or ./stop-kind.sh -d.
set -euo pipefail

CLUSTER=${KIND_CLUSTER_NAME:-zac}
CONTEXT=kind-$CLUSTER
NODE=$CLUSTER-control-plane
NAMESPACE=zac

kc() { kubectl --context "$CONTEXT" --namespace "$NAMESPACE" "$@"; }

if ! kind get clusters 2> /dev/null | grep -qx "$CLUSTER"; then
  echo "The kind cluster '$CLUSTER' does not exist, so it has no data to clear."
  exit 0
fi
if [ "$(docker inspect --format '{{.State.Running}}' "$NODE")" != true ]; then
  echo "Starting the stopped cluster '$CLUSTER' to clear its data ..."
  docker start "$NODE" > /dev/null
fi
until kubectl --context "$CONTEXT" get --raw /readyz > /dev/null 2>&1; do sleep 2; done

echo "Clearing the data of the kind cluster '$CLUSTER' ..."
# a persistent volume claim is only deleted when no pod uses it any more
kc delete deployment,statefulset,job --all --wait=true --timeout=5m > /dev/null
kc delete pod --all --wait=true --timeout=5m > /dev/null
kc delete persistentvolumeclaim --all --wait=true --timeout=5m

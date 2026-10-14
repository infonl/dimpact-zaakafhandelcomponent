#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# SPIKE (PZ-13059): brings up the services that the ZAC integration tests need in a kind cluster:
# the PodiumD umbrella chart (release 'podiumd', namespace 'podiumd') with only the components ZAC needs,
# charts/zac-dev for the test doubles and seed data, and ZAC itself from charts/zac (release 'zac', namespace 'zac').
# Then run one shard against it with: ./gradlew itest -PitestPlatform=kind -PitestShard=3 -x buildDockerImage
#
# Environment:
#   ZAC_IMAGE          ZAC image to load into the cluster (default ghcr.io/infonl/zaakafhandelcomponent:kind-spike)
#   PODIUMD_CHART      chart reference (default dimpact/podiumd, version PODIUMD_VERSION, default 4.9.4)
#   ARCH_VALUES        'arm64' to use the ghcr.io/infonl arm64 images (default: arm64 on an arm64 host)
#   TIMINGS_FILE       where the step timings go (default build/reports/kind-spike/timings.tsv)
#   USE_EXISTING_CLUSTER=1  install into the existing kind-zac-spike cluster
set -euo pipefail

CLUSTER=zac-spike
CONTEXT=kind-$CLUSTER
HACK=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$HACK/../.." && pwd)
IMPORTS=$ROOT/scripts/docker-compose/imports
ZAC_IMAGE=${ZAC_IMAGE:-ghcr.io/infonl/zaakafhandelcomponent:kind-spike}
PODIUMD_CHART=${PODIUMD_CHART:-dimpact/podiumd}
PODIUMD_VERSION=${PODIUMD_VERSION:-4.9.4}
TIMINGS_FILE=${TIMINGS_FILE:-$ROOT/build/reports/kind-spike/timings.tsv}
if [ -z "${ARCH_VALUES:-}" ] && [ "$(uname -m)" = "arm64" -o "$(uname -m)" = "aarch64" ]; then
  ARCH_VALUES=arm64
fi
mkdir -p "$(dirname "$TIMINGS_FILE")"
: > "$TIMINGS_FILE"

kc() { kubectl --context "$CONTEXT" "$@"; }
START=$(date +%s)
LAST=$START
step() { printf '\n\033[1;34m==> %s\033[0m\n' "$1"; }
mark() {
  local now; now=$(date +%s)
  printf '%s\t%s\t%s\n' "$1" "$((now - LAST))" "$((now - START))" | tee -a "$TIMINGS_FILE"
  LAST=$now
}

# 1. Cluster ---------------------------------------------------------------------------------------------
if [ "${USE_EXISTING_CLUSTER:-}" != "1" ]; then
  step "kind cluster $CLUSTER"
  kind get clusters | grep -qx "$CLUSTER" || kind create cluster --name "$CLUSTER" --config "$HACK/kind-config.yaml"
  mark cluster-created
fi

# kind runs containerd with LimitNOFILE=infinity; containers inherit it and uWSGI (Open Zaak, Open Object,
# Open Klant) sizes its tables by RLIMIT_NOFILE, so one worker can take gigabytes. Cap it (trick from gpp-woo).
step "cap containerd nofile"
for node in $(kc get nodes -o jsonpath='{.items[*].metadata.name}'); do
  docker exec "$node" bash -c '
    if [ ! -f /etc/systemd/system/containerd.service.d/nofile.conf ]; then
      mkdir -p /etc/systemd/system/containerd.service.d
      printf "[Service]\nLimitNOFILE=1048576\n" > /etc/systemd/system/containerd.service.d/nofile.conf
      systemctl daemon-reload && systemctl restart containerd
    fi'
  # the kubelet pulls anonymously unless it has a Docker config with credentials; in CI that avoids the Docker
  # Hub rate limit (on Docker Desktop the credentials live in the keychain, so there is nothing to copy)
  if [ -f "$HOME/.docker/config.json" ] && grep -q '"auth"' "$HOME/.docker/config.json"; then
    docker cp "$HOME/.docker/config.json" "$node:/var/lib/kubelet/config.json"
    docker exec "$node" systemctl restart kubelet
  fi
done

# charts/zac depends on two charts that are not in git (disabled here, but Helm still wants them)
if ! ls "$ROOT"/charts/zac/charts/*.tgz > /dev/null 2>&1; then
  helm repo add opentelemetry https://open-telemetry.github.io/opentelemetry-helm-charts > /dev/null 2>&1 || true
  helm repo add solr https://solr.apache.org/charts > /dev/null 2>&1 || true
  helm dependency build "$ROOT/charts/zac" > /dev/null
fi

# ZAC calls its OPA sidecar while it boots, and Kubernetes starts the sidecar only after the ZAC container; a cold
# pull of the OPA image makes ZAC boot with errors and wait for a liveness restart. Pull the images of the ZAC pod
# up front, in the background.
step "pre-pull the ZAC pod images"
PREPULL_PIDS=()
for image in $(helm template zac "$ROOT/charts/zac" -f "$HACK/values/zac.yaml" 2> /dev/null |
    sed -nE 's/^ *image: *"?([^" ]+)"?.*/\1/p' | sort -u | grep -v zaakafhandelcomponent); do
  docker exec "$CLUSTER-control-plane" crictl pull "$image" > /dev/null &
  PREPULL_PIDS+=($!)
done

# 2. DNS: the Docker Compose host names in the ZAC test data resolve to Services in the cluster -----------
step "CoreDNS rewrites"
python3 - "$CONTEXT" <<'PY'
import subprocess, sys, tempfile
context = sys.argv[1]
kubectl = ["kubectl", "--context", context, "-n", "kube-system"]
corefile = subprocess.check_output(kubectl + ["get", "cm", "coredns", "-o", "jsonpath={.data.Corefile}"]).decode()
rules = [
    "rewrite name openzaak.local openzaak-local.podiumd.svc.cluster.local",
    "rewrite name open-object.local open-object-local.podiumd.svc.cluster.local",
    "rewrite name openklant.local openklant-local.podiumd.svc.cluster.local",
    "rewrite name bag-wiremock.local bag-wiremock.zac.svc.cluster.local",
]
missing = [rule for rule in rules if rule not in corefile]
if missing:
    corefile = corefile.replace("ready\n", "ready\n" + "".join(f"    {rule}\n" for rule in missing), 1)
    manifest = subprocess.check_output(kubectl + ["create", "cm", "coredns", f"--from-literal=Corefile={corefile}",
                                                  "--dry-run=client", "-o", "yaml"])
    subprocess.run(kubectl + ["apply", "-f", "-"], input=manifest, check=True)
    subprocess.run(kubectl + ["rollout", "restart", "deploy/coredns"], check=True)
PY

# 3. Namespaces, storage and seed data -----------------------------------------------------------------
step "namespaces, PVCs and seed ConfigMaps"
for namespace in podiumd zac; do
  kc create namespace $namespace --dry-run=client -o yaml | kc apply -f - > /dev/null
done
# PodiumD's *-storage.yaml templates create Azure Files PVs/PVCs unless a PVC with that name exists already
for claim in openzaak objecten openklant; do
  kc -n podiumd apply -f - > /dev/null <<EOF
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: $claim
spec:
  accessModes: [ReadWriteOnce]
  storageClassName: standard
  resources:
    requests:
      storage: 1Gi
EOF
done
configmap() {
  local namespace=$1 name=$2; shift 2
  kc -n "$namespace" create configmap "$name" "$@" --dry-run=client -o yaml | kc apply --server-side -f - > /dev/null
}
configmap podiumd seed-openzaak-dump --from-file="$IMPORTS/openzaak-database/00-restore-migrated-database.sql"
configmap podiumd seed-openzaak-data --from-file="$IMPORTS/openzaak-database/database"
configmap podiumd seed-openzaak-uploads --from-file="$IMPORTS/openzaak/uploads/fake-test-document.pdf"
configmap podiumd seed-openklant --from-file="$IMPORTS/openklant-database/database/1-setup-applicatie.sql"
configmap podiumd seed-objecten-fixtures --from-file="$IMPORTS/open-object/fixtures"
configmap podiumd seed-pabc-dataset --from-file="$IMPORTS/pabc-database/json-mapping/pabc-mapping-data.json"
python3 "$HACK/zac-realm-layer.py" "$IMPORTS/keycloak/realms/zaakafhandelcomponent-realm.json" "$ROOT/docker-compose.yaml" \
  > "$(dirname "$TIMINGS_FILE")/zac-realm-layer.json"
configmap podiumd seed-keycloak-zac --from-file="$(dirname "$TIMINGS_FILE")/zac-realm-layer.json"
for wiremock in smartdocuments-wiremock kvk-wiremock bag-wiremock brp-personen-wiremock; do
  configmap zac $wiremock-mappings --from-file="$IMPORTS/$wiremock/mappings"
  if [ -d "$IMPORTS/$wiremock/__files" ]; then
    configmap zac $wiremock-files --from-file="$IMPORTS/$wiremock/__files"
  fi
done
mark seed-data-created

ZAC_DEV_VALUES=()
PODIUMD_VALUES=(-f "$HACK/values/podiumd.yaml")
if [ "${ARCH_VALUES:-}" = "arm64" ]; then
  ZAC_DEV_VALUES+=(-f "$HACK/values/zac-dev-arm64.yaml")
  PODIUMD_VALUES+=(-f "$HACK/values/podiumd-arm64.yaml")
fi

# 4. Shared Postgres first, so that PodiumD finds its databases ----------------------------------------
step "zac-dev infra (Postgres)"
helm --kube-context "$CONTEXT" upgrade --install zac-dev-infra "$ROOT/charts/zac-dev" -n podiumd \
  --set part=infra "${ZAC_DEV_VALUES[@]}" > /dev/null
mark postgres-installed

# 5. PodiumD umbrella --------------------------------------------------------------------------------------
step "PodiumD $PODIUMD_VERSION"
if [ "$PODIUMD_CHART" = "dimpact/podiumd" ]; then
  helm repo add dimpact https://Dimpact-Samenwerking.github.io/helm-charts/ > /dev/null 2>&1 || true
  helm repo update dimpact > /dev/null
fi
helm --kube-context "$CONTEXT" upgrade --install podiumd "$PODIUMD_CHART" --version "$PODIUMD_VERSION" -n podiumd \
  "${PODIUMD_VALUES[@]}" > /dev/null
mark podiumd-installed

# 6. ZAC image, test doubles and ZAC ---------------------------------------------------------------------
step "load $ZAC_IMAGE"
# lets this script start while the ZAC image is still being built
until docker image inspect "$ZAC_IMAGE" > /dev/null 2>&1; do sleep 5; done
mark zac-image-available
kind load docker-image "$ZAC_IMAGE" --name "$CLUSTER" > /dev/null
mark zac-image-loaded

step "zac-dev (test doubles, seed jobs, NodePorts)"
helm --kube-context "$CONTEXT" upgrade --install zac-dev "$ROOT/charts/zac-dev" -n zac \
  --set part=zac "${ZAC_DEV_VALUES[@]}" > /dev/null
mark zac-dev-installed
# ZAC records boot errors and stays unhealthy until the liveness probe restarts it (16 x 30s) when Keycloak,
# Open Zaak or PABC are not there yet when it boots; Docker Compose has depends_on for this.
step "waiting for the ZAC dependencies"
kc -n podiumd wait --for=condition=complete job/seed-keycloak-zac --timeout=1200s > /dev/null
kc -n podiumd wait --for=condition=available deploy/openzaak deploy/openzaak-nginx deploy/pabc deploy/objecten deploy/openklant --timeout=600s > /dev/null
mark zac-dependencies-ready
wait "${PREPULL_PIDS[@]}"
mark zac-pod-images-pulled
step "ZAC"
helm --kube-context "$CONTEXT" upgrade --install zac "$ROOT/charts/zac" -n zac \
  -f "$HACK/values/zac.yaml" --set image.repository="${ZAC_IMAGE%:*}" --set image.tag="${ZAC_IMAGE##*:}" > /dev/null
mark zac-installed

# 7. Wait until everything is ready ---------------------------------------------------------------------
step "waiting for all workloads"
python3 "$HACK/wait-ready.py" "$CONTEXT" "${WAIT_TIMEOUT_SECONDS:-1500}" "$START" | tee -a "$TIMINGS_FILE"
mark all-ready

step "done in $(( $(date +%s) - START ))s — ZAC :28080, Keycloak :28081, Open Zaak :28001, GreenMail :38083"

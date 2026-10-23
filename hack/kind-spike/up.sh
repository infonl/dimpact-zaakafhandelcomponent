#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# SPIKE (PZ-13059): brings up the services that the ZAC integration tests need in a kind cluster, all in namespace
# 'zac', with docker-compose.yaml as the source of truth for the services, images, settings and seed data:
#   - Open Zaak, Open Klant and Open Object from the upstream Maykin Media charts (values in values/),
#   - Postgres, Redis, Keycloak, PABC, Solr, the test doubles and the seed jobs from charts/zac-dev,
#   - ZAC itself from charts/zac.
# Then run one shard against it with: ./gradlew itest -PitestPlatform=kind -PitestShard=3 -x buildDockerImage
#
# Environment:
#   ZAC_IMAGE          ZAC image to load into the cluster (default ghcr.io/infonl/zaakafhandelcomponent:kind-spike);
#                      the script waits until it exists, so it can start while the image is still being built
#   ARCH_VALUES        'arm64' to use the ghcr.io/infonl arm64 images (default: arm64 on an arm64 host)
#   PREPULL            '0' to leave every image pull to the kubelet instead of pulling all images up front
#   TIMINGS_FILE       where the step timings go (default build/reports/kind-spike/timings.tsv)
#   USE_EXISTING_CLUSTER=1  install into the existing kind-zac-spike cluster
set -euo pipefail

CLUSTER=zac-spike
CONTEXT=kind-$CLUSTER
NODE=$CLUSTER-control-plane
NAMESPACE=zac
HACK=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$HACK/../.." && pwd)
IMPORTS=$ROOT/scripts/docker-compose/imports
ZAC_IMAGE=${ZAC_IMAGE:-ghcr.io/infonl/zaakafhandelcomponent:kind-spike}
TIMINGS_FILE=${TIMINGS_FILE:-$ROOT/build/reports/kind-spike/timings.tsv}
REPORT_DIR=$(dirname "$TIMINGS_FILE")
WORK_DIR=$ROOT/build/kind-spike
MAYKIN_CHARTS=https://maykinmedia.github.io/charts/
OPENZAAK_CHART_VERSION=1.15.0
OPENKLANT_CHART_VERSION=1.11.0
OPENOBJECT_CHART_VERSION=1.2.0
if [ -z "${ARCH_VALUES:-}" ] && [ "$(uname -m)" = "arm64" -o "$(uname -m)" = "aarch64" ]; then
  ARCH_VALUES=arm64
fi
mkdir -p "$REPORT_DIR" "$WORK_DIR/charts" "$WORK_DIR/manifests"
: > "$TIMINGS_FILE"
: > "$REPORT_DIR/image-pulls.tsv"

kc() { kubectl --context "$CONTEXT" "$@"; }
START=$(date +%s)
LAST=$START
step() { printf '\n\033[1;34m==> %s\033[0m\n' "$1"; }
mark() {
  local now; now=$(date +%s)
  printf '%s\t%s\t%s\n' "$1" "$((now - LAST))" "$((now - START))" | tee -a "$TIMINGS_FILE"
  LAST=$now
}

# 1. Cluster, and meanwhile the charts ----------------------------------------------------------------
step "kind cluster $CLUSTER and the charts"
CLUSTER_PID=
if [ "${USE_EXISTING_CLUSTER:-}" != "1" ] && ! kind get clusters | grep -qx "$CLUSTER"; then
  kind create cluster --name "$CLUSTER" --config "$HACK/kind-config.yaml" &
  CLUSTER_PID=$!
fi
for chart in "openzaak:$OPENZAAK_CHART_VERSION" "openklant:$OPENKLANT_CHART_VERSION" "openobject:$OPENOBJECT_CHART_VERSION"; do
  if [ ! -f "$WORK_DIR/charts/${chart%%:*}-${chart##*:}.tgz" ]; then
    helm pull "${chart%%:*}" --repo "$MAYKIN_CHARTS" --version "${chart##*:}" --destination "$WORK_DIR/charts"
  fi
done
# charts/zac depends on two charts that are not in git (disabled here, but Helm still wants them)
if ! ls "$ROOT"/charts/zac/charts/*.tgz > /dev/null 2>&1; then
  helm repo add opentelemetry https://open-telemetry.github.io/opentelemetry-helm-charts > /dev/null 2>&1 || true
  helm repo add solr https://solr.apache.org/charts > /dev/null 2>&1 || true
  helm dependency build "$ROOT/charts/zac" > /dev/null
fi
[ -n "$CLUSTER_PID" ] && wait "$CLUSTER_PID"
mark cluster-created

# 2. Passwords: generated for every new cluster, never committed ---------------------------------------
kc create namespace $NAMESPACE --dry-run=client -o yaml | kc apply -f - > /dev/null
PASSWORD_KEYS="postgres keycloak keycloak-admin openzaak openklant objects pabc zac"
mkdir -p "$WORK_DIR/passwords"
for key in $PASSWORD_KEYS; do
  existing=$(kc -n $NAMESPACE get secret zac-dev-passwords -o "jsonpath={.data.$key}" 2> /dev/null | base64 -d || true)
  printf '%s' "${existing:-$(openssl rand -hex 16)}" > "$WORK_DIR/passwords/$key"
done
password() { cat "$WORK_DIR/passwords/$1"; }

# 3. Releases: rendered once, for the image list and for the install --------------------------------------
ZAC_DEV_VALUES=()
OPENZAAK_VALUES=(-f "$HACK/values/openzaak.yaml")
OPENKLANT_VALUES=(-f "$HACK/values/openklant.yaml")
OPENOBJECT_VALUES=(-f "$HACK/values/openobject.yaml")
if [ "${ARCH_VALUES:-}" = "arm64" ]; then
  ZAC_DEV_VALUES+=(-f "$HACK/values/zac-dev-arm64.yaml")
  OPENZAAK_VALUES+=(-f "$HACK/values/openzaak-arm64.yaml")
  OPENKLANT_VALUES+=(-f "$HACK/values/openklant-arm64.yaml")
  OPENOBJECT_VALUES+=(-f "$HACK/values/openobject-arm64.yaml")
fi
release_args() {
  case $1 in
    zac-dev) echo "$ROOT/charts/zac-dev" ${ZAC_DEV_VALUES[@]+"${ZAC_DEV_VALUES[@]}"} ;;
    openzaak) echo "$WORK_DIR/charts/openzaak-$OPENZAAK_CHART_VERSION.tgz" "${OPENZAAK_VALUES[@]}" \
      --set "settings.database.password=$(password openzaak)" ;;
    openklant) echo "$WORK_DIR/charts/openklant-$OPENKLANT_CHART_VERSION.tgz" "${OPENKLANT_VALUES[@]}" \
      --set "settings.database.password=$(password openklant)" ;;
    openobject) echo "$WORK_DIR/charts/openobject-$OPENOBJECT_CHART_VERSION.tgz" "${OPENOBJECT_VALUES[@]}" \
      --set "settings.database.password=$(password objects)" ;;
    zac) echo "$ROOT/charts/zac" -f "$HACK/values/zac.yaml" --set "db.password=$(password zac)" \
      --set "image.repository=${ZAC_IMAGE%:*}" --set "image.tag=${ZAC_IMAGE##*:}" ;;
  esac
}
RELEASES=(zac-dev openzaak openklant openobject zac)
for release in "${RELEASES[@]}"; do
  # shellcheck disable=SC2046 # word splitting on purpose: no argument contains a space
  helm template "$release" $(release_args "$release") -n $NAMESPACE > "$WORK_DIR/manifests/$release.yaml"
done

# 4. Image pulls ---------------------------------------------------------------------------------------
# kind runs containerd with LimitNOFILE=infinity; containers inherit it and uWSGI (Open Zaak, Open Object,
# Open Klant) sizes its tables by RLIMIT_NOFILE, so one worker can take gigabytes. Cap it (trick from gpp-woo).
# This restarts containerd, so it goes before the image pulls.
step "cap containerd nofile"
docker exec "$NODE" bash -c '
  if [ ! -f /etc/systemd/system/containerd.service.d/nofile.conf ]; then
    mkdir -p /etc/systemd/system/containerd.service.d
    printf "[Service]\nLimitNOFILE=1048576\n" > /etc/systemd/system/containerd.service.d/nofile.conf
    systemctl daemon-reload && systemctl restart containerd
  fi'
until docker exec "$NODE" crictl info > /dev/null 2>&1; do sleep 1; done
# the kubelet and crictl pull anonymously unless they get credentials; in CI those avoid the Docker Hub rate limit
# (on Docker Desktop the credentials live in the keychain, so there is nothing to copy)
DOCKER_HUB_AUTH=
if [ -f "$HOME/.docker/config.json" ] && grep -q '"auth"' "$HOME/.docker/config.json"; then
  docker cp "$HOME/.docker/config.json" "$NODE:/var/lib/kubelet/config.json"
  docker exec "$NODE" systemctl restart kubelet
  DOCKER_HUB_AUTH=$(python3 -c 'import json, os, sys
auths = json.load(open(os.path.expanduser("~/.docker/config.json"))).get("auths", {})
print(auths.get("https://index.docker.io/v1/", {}).get("auth", ""))')
fi

# The images of the stack, pulled in parallel inside the node as soon as the node exists, in two waves that share
# the bandwidth: first the images of the ZAC dependencies, then those of the ZAC pod, which starts last. Without
# this the kubelet pulls an image only once its pod is scheduled. CronJob images are left out: no CronJob runs.
images_of() {
  awk '/^---/ { kind = "" } /^kind:/ { kind = $2 }
       kind != "CronJob" && match($0, /^ *(- )?image: *"?[^" ]+/) {
         image = substr($0, RSTART, RLENGTH); sub(/^ *(- )?image: *"?/, "", image); print image }' "$@" |
    sort -u | grep -v "^${ZAC_IMAGE%:*}:"
}
pull() {
  local image=$1 auth=()
  # Docker Hub images: no registry host (a first path segment without a dot) or docker.io
  if [ -n "$DOCKER_HUB_AUTH" ] && { [[ $image == docker.io/* ]] || [[ ${image%%/*} != *.* ]] || [[ $image != */* ]]; }; then
    auth=(--auth "$DOCKER_HUB_AUTH")
  fi
  local pull_start status
  pull_start=$(date +%s)
  if docker exec "$NODE" crictl pull ${auth[@]+"${auth[@]}"} "$image" > /dev/null 2>&1; then status=ok; else status=failed; fi
  printf '%s\t%s\t%s\t%s\n' "$image" "$(($(date +%s) - pull_start))" "$(($(date +%s) - START))" "$status" \
    >> "$REPORT_DIR/image-pulls.tsv"
}
PULL_PID=
if [ "${PREPULL:-1}" != "0" ]; then
  step "pull the images in two waves"
  FIRST_WAVE=$(images_of "$WORK_DIR"/manifests/{zac-dev,openzaak,openklant,openobject}.yaml)
  SECOND_WAVE=$(images_of "$WORK_DIR/manifests/zac.yaml" | grep -vxF "$FIRST_WAVE" || true)
  (
    for image in $FIRST_WAVE; do pull "$image" & done
    wait
    for image in $SECOND_WAVE; do pull "$image" & done
    wait
  ) &
  PULL_PID=$!
fi

# ZAC itself comes from the local Docker daemon, in the background as well
(
  until docker image inspect "$ZAC_IMAGE" > /dev/null 2>&1; do sleep 5; done
  printf 'zac-image-available\t-\t%s\n' "$(($(date +%s) - START))" >> "$TIMINGS_FILE"
  kind load docker-image "$ZAC_IMAGE" --name "$CLUSTER" > /dev/null
  printf 'zac-image-loaded\t-\t%s\n' "$(($(date +%s) - START))" >> "$TIMINGS_FILE"
) &
ZAC_IMAGE_PID=$!
mark pulls-started

# 5. DNS: the Docker Compose host names in the ZAC test data resolve to Services in the cluster -----------
step "CoreDNS rewrites"
python3 - "$CONTEXT" "$NAMESPACE" <<'PY'
import subprocess, sys
context, namespace = sys.argv[1], sys.argv[2]
kubectl = ["kubectl", "--context", context, "-n", "kube-system"]
corefile = subprocess.check_output(kubectl + ["get", "cm", "coredns", "-o", "jsonpath={.data.Corefile}"]).decode()
rules = [
    f"rewrite name openzaak.local openzaak-local.{namespace}.svc.cluster.local",
    f"rewrite name open-object.local open-object-local.{namespace}.svc.cluster.local",
    f"rewrite name openklant.local openklant-local.{namespace}.svc.cluster.local",
    f"rewrite name bag-wiremock.local bag-wiremock.{namespace}.svc.cluster.local",
]
missing = [rule for rule in rules if rule not in corefile]
if missing:
    corefile = corefile.replace("ready\n", "ready\n" + "".join(f"    {rule}\n" for rule in missing), 1)
    manifest = subprocess.check_output(kubectl + ["create", "cm", "coredns", f"--from-literal=Corefile={corefile}",
                                                  "--dry-run=client", "-o", "yaml"])
    subprocess.run(kubectl + ["apply", "-f", "-"], input=manifest, check=True, stdout=subprocess.DEVNULL)
    subprocess.run(kubectl + ["rollout", "restart", "deploy/coredns"], check=True, stdout=subprocess.DEVNULL)
PY

# 6. Secrets and seed data, in one apply -------------------------------------------------------------------
step "secrets and seed ConfigMaps"
SEED=$WORK_DIR/seed.yaml
: > "$SEED"
add() { kc -n $NAMESPACE create "$@" --dry-run=client -o yaml >> "$SEED"; echo --- >> "$SEED"; }
add secret generic zac-dev-passwords --from-file="$WORK_DIR/passwords"
# a role per component, each the owner of its database; superusers, because the Open Zaak dump creates extensions
roles_sql=$WORK_DIR/00-roles-and-databases.sql
: > "$roles_sql"
for spec in keycloak:keycloak openzaak:openzaak openklant:openklant objects:objects pabc:Pabc zac:zac; do
  role=${spec%%:*} database=${spec##*:}
  printf 'CREATE ROLE "%s" LOGIN SUPERUSER PASSWORD '"'"'%s'"'"';\nCREATE DATABASE "%s" OWNER "%s";\n' \
    "$role" "$(password "$role")" "$database" "$role" >> "$roles_sql"
done
add secret generic postgres-initdb-roles --from-file="$roles_sql"
add configmap seed-openzaak-dump --from-file="$IMPORTS/openzaak-database/00-restore-migrated-database.sql"
add configmap seed-openzaak-data --from-file="$IMPORTS/openzaak-database/database"
add configmap seed-openzaak-uploads --from-file="$IMPORTS/openzaak/uploads/fake-test-document.pdf"
add configmap seed-openklant --from-file="$IMPORTS/openklant-database/database/1-setup-applicatie.sql"
add configmap seed-objecten-fixtures --from-file="$IMPORTS/open-object/fixtures"
add configmap seed-pabc-dataset --from-file="$IMPORTS/pabc-database/json-mapping/pabc-mapping-data.json"
add configmap solr-security --from-file="$IMPORTS/solr/security.json"
add configmap keycloak-realm --from-file="$IMPORTS/keycloak/realms"
# the e-mail addresses that the realm export refers to: '- ZAC_X=${DOCKER_COMPOSE_X:-default}' in docker-compose.yaml
python3 -c 'import re, sys
for name, value in re.findall(r"- (ZAC_[A-Z0-9_]+)=\$\{[A-Z0-9_]+:-([^}]*)\}", open(sys.argv[1]).read()):
    print(f"{name}={value}")' "$ROOT/docker-compose.yaml" > "$WORK_DIR/keycloak-realm.env"
add configmap keycloak-realm-env --from-env-file="$WORK_DIR/keycloak-realm.env"
for wiremock in smartdocuments-wiremock kvk-wiremock bag-wiremock brp-personen-wiremock; do
  add configmap $wiremock-mappings --from-file="$IMPORTS/$wiremock/mappings"
  if [ -d "$IMPORTS/$wiremock/__files" ]; then
    add configmap $wiremock-files --from-file="$IMPORTS/$wiremock/__files"
  fi
done
kc apply --server-side --force-conflicts -f "$SEED" > /dev/null
mark seed-data-created

# 7. Everything but ZAC, in parallel ----------------------------------------------------------------------
step "zac-dev, Open Zaak, Open Klant and Open Object"
INSTALL_PIDS=()
for release in zac-dev openzaak openklant openobject; do
  # shellcheck disable=SC2046 # word splitting on purpose: no argument contains a space
  helm --kube-context "$CONTEXT" upgrade --install "$release" $(release_args "$release") -n $NAMESPACE > /dev/null &
  INSTALL_PIDS+=($!)
done
for pid in "${INSTALL_PIDS[@]}"; do wait "$pid"; done
mark releases-installed
# records when each workload gets ready, from now on; it finishes once ZAC is ready as well
python3 "$HACK/wait-ready.py" "$CONTEXT" "${WAIT_TIMEOUT_SECONDS:-1500}" "$START" "$NAMESPACE" zac/zac \
  > "$REPORT_DIR/ready.tsv" &
WAIT_READY_PID=$!

# ZAC records boot errors and stays unhealthy until the liveness probe restarts it (16 x 30s) when Keycloak,
# Open Zaak or PABC are not there yet when it boots; Docker Compose has depends_on for this.
step "waiting for the ZAC dependencies"
kc -n $NAMESPACE wait --for=condition=available deploy/keycloak deploy/openzaak deploy/openzaak-nginx deploy/pabc-api \
  --timeout=900s > /dev/null
mark zac-dependencies-ready
wait "$ZAC_IMAGE_PID"
mark zac-image-ready
# ZAC calls its OPA sidecar while it boots, and the kubelet starts the ZAC container before it pulls the OPA image,
# so a slow pull gives ZAC a boot error that only the liveness probe resolves (16 x 30s). Have the images of the ZAC
# pod in the node first; with PREPULL they usually are already.
ZAC_POD_IMAGES=$(python3 - "$WORK_DIR/manifests/zac.yaml" <<'PY'
import re, sys
for document in open(sys.argv[1]).read().split("\n---"):
    if re.search(r"^kind: Deployment$", document, re.M) and re.search(r"^  name: zac$", document, re.M):
        print("\n".join(re.findall(r'^ *(?:- )?image: *"?([^" \n]+)', document, re.M)))
PY
)
for image in $(echo "$ZAC_POD_IMAGES" | sort -u | grep -v "^${ZAC_IMAGE%:*}:"); do
  docker exec "$NODE" crictl inspecti "$image" > /dev/null 2>&1 || pull "$image"
done
mark zac-pod-images-ready

# 8. ZAC -----------------------------------------------------------------------------------------------
step "ZAC"
# shellcheck disable=SC2046 # word splitting on purpose: no argument contains a space
helm --kube-context "$CONTEXT" upgrade --install zac $(release_args zac) -n $NAMESPACE > /dev/null
mark zac-installed

# 9. Wait until everything is ready ---------------------------------------------------------------------
step "waiting for all workloads"
wait_ready_status=0
wait "$WAIT_READY_PID" || wait_ready_status=$?
# the events tell when each image was pulled and each container started, for the report artifact
kc -n $NAMESPACE get events --sort-by=.lastTimestamp -o wide > "$REPORT_DIR/events.txt" 2>&1 || true
kc -n $NAMESPACE get pods -o json > "$REPORT_DIR/pods.json" 2>&1 || true
sort -t$'\t' -k3 -n "$REPORT_DIR/ready.tsv" | tee -a "$TIMINGS_FILE"
[ "$wait_ready_status" = 0 ] || exit "$wait_ready_status"
mark all-ready

[ -z "$PULL_PID" ] || wait "$PULL_PID" || true
if [ -s "$REPORT_DIR/image-pulls.tsv" ]; then
  printf 'images-pulled\t-\t%s\n' "$(sort -t$'\t' -k3 -n "$REPORT_DIR/image-pulls.tsv" | tail -1 | cut -f3)" | tee -a "$TIMINGS_FILE"
fi

step "done in $(($(date +%s) - START))s — ZAC :28080, Keycloak :28081, Open Zaak :28001, GreenMail :38083"

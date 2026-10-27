#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Brings up the local ZAC development stack in a kind cluster. The images, the chart versions and the settings of the
# stack are in scripts/kind (charts/zac-dev/values.yaml and values/), the seed data in scripts/kind/seed. Usually
# started through ./start-kind.sh; see docs/development/installKind.md.
#
# Environment:
#   KIND_CLUSTER_NAME            name of the kind cluster (default: zac)
#   ZAC_IN_CLUSTER               'registry' to run ZAC_DOCKER_IMAGE from its registry, 'local' to run ZAC_DOCKER_IMAGE
#                                from the local Docker daemon, or empty to run everything but ZAC
#   ZAC_DOCKER_IMAGE             the ZAC image (default: ghcr.io/infonl/zaakafhandelcomponent:latest for 'registry',
#                                ghcr.io/infonl/zaakafhandelcomponent:dev for 'local')
#   KIND_PROFILES                comma-separated optional services: objecten, itest, opennotificaties,
#                                openarchiefbeheer, openformulieren
#   DOCKER_USE_ARM64_CONTAINERS  'true' or 'false' to use the arm64 images of scripts/kind/values/arm64.yaml or not
#                                (default: 'true' on an arm64 host)
#   KIND_PORT_OFFSET             added to every host port (default: 0)
#   KIND_PORT_<NAME>             the host port of one service, or 0 to not publish it, for example KIND_PORT_ZAC=18080;
#                                see scripts/kind/ports.json for the names
#   KEYCLOAK_HOSTNAME            the URL of Keycloak in the browser and in the cluster
#                                (default: http://keycloak.localhost:<Keycloak host port>)
#   the settings of ZAC that docs/development/installKind.md lists, also from .env
set -euo pipefail

CLUSTER=${KIND_CLUSTER_NAME:-zac}
CONTEXT=kind-$CLUSTER
NODE=$CLUSTER-control-plane
PROXY=$CLUSTER-ports
NAMESPACE=zac
KIND_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$KIND_DIR/../.." && pwd)
SEED_DIR=$KIND_DIR/seed
STACK_CHART=$KIND_DIR/charts/zac-dev
WORK_DIR=$ROOT/build/kind/$CLUSTER
CHARTS_DIR=$ROOT/build/kind/charts
PROFILES=${KIND_PROFILES:-}
START=$(date +%s)
UPSTREAM_RELEASES=(openzaak openklant openobject opennotificaties openarchiefbeheer openforms)

step() { printf '\n\033[1;34m==> %s\033[0m (%ss)\n' "$1" "$(($(date +%s) - START))"; }
fail() { printf '\n\033[1;31m%s\033[0m\n' "$*" >&2; exit 1; }
kc() { kubectl --context "$CONTEXT" "$@"; }
helm_release() { helm --kube-context "$CONTEXT" upgrade --install "$@" --namespace "$NAMESPACE" > "$WORK_DIR/helm-$1.log" 2>&1 ||
  fail "Installing the Helm release '$1' failed: $(cat "$WORK_DIR/helm-$1.log")"; }
helm_uninstall() {
  if helm --kube-context "$CONTEXT" status "$1" --namespace "$NAMESPACE" > /dev/null 2>&1; then
    echo "Removing $1"
    helm --kube-context "$CONTEXT" uninstall "$1" --namespace "$NAMESPACE" --wait > /dev/null
  fi
}

case ${ZAC_IN_CLUSTER:-} in
  registry) ZAC_DOCKER_IMAGE=${ZAC_DOCKER_IMAGE:-ghcr.io/infonl/zaakafhandelcomponent:latest}; MODE=zac ;;
  local) ZAC_DOCKER_IMAGE=${ZAC_DOCKER_IMAGE:-ghcr.io/infonl/zaakafhandelcomponent:dev}; MODE=zac ;;
  "") MODE=ide ;;
  *) fail "ZAC_IN_CLUSTER is '$ZAC_IN_CLUSTER'; it must be 'registry', 'local' or empty" ;;
esac

missing=()
for tool in docker kind kubectl helm python3; do
  command -v "$tool" > /dev/null || missing+=("$tool")
done
[ ${#missing[@]} -eq 0 ] || fail "Missing tools: ${missing[*]}. See docs/development/installKind.md."
docker info > /dev/null 2>&1 || fail "Docker is not running."
docker_memory_gib=$(($(docker info --format '{{.MemTotal}}') / 1024 / 1024 / 1024))
if [ "$docker_memory_gib" -lt 12 ]; then
  echo "Warning: Docker has ${docker_memory_gib} GiB of memory; the kind stack with ZAC needs about 9 GiB." >&2
fi
if [ "$MODE" = zac ] && [ "$ZAC_IN_CLUSTER" = local ] && ! docker image inspect "$ZAC_DOCKER_IMAGE" > /dev/null 2>&1; then
  fail "The image $ZAC_DOCKER_IMAGE is not in the local Docker daemon. Build it with './gradlew buildDockerImage'."
fi
mkdir -p "$WORK_DIR/generated" "$CHARTS_DIR"
chmod 700 "$WORK_DIR/generated"

step "Reading the stack"
stack_values=(-f "$KIND_DIR/ports.json")
use_arm64=${DOCKER_USE_ARM64_CONTAINERS:-}
if [ -z "$use_arm64" ]; then
  case $(uname -m) in arm64 | aarch64) use_arm64=true ;; *) use_arm64=false ;; esac
fi
if [ "$use_arm64" != false ] && [ "$use_arm64" != 0 ]; then
  echo "Using the arm64 images of scripts/kind/values/arm64.yaml"
  stack_values+=(-f "$KIND_DIR/values/arm64.yaml")
fi
profile_values=()
for profile in objecten itest opennotificaties openarchiefbeheer openformulieren; do
  profile_values+=(--set "profiles.$profile=$([[ ,$PROFILES, == *,$profile,* ]] && echo true || echo false)")
done
helm template zac-dev "$STACK_CHART" "${stack_values[@]}" "${profile_values[@]}" --set keycloak.hostname=- \
  --show-only templates/stack.yaml | python3 -c '
import json, sys
manifest = sys.stdin.read()
print(json.dumps(json.loads(manifest.split("stack.json: |", 1)[1])))' > "$WORK_DIR/stack.json" ||
  fail "Reading the values of scripts/kind/charts/zac-dev failed"
stack() { python3 -c 'import json, sys; value = json.load(open(sys.argv[1]))
for key in sys.argv[2:]: value = value[key]
print(value if isinstance(value, str) else json.dumps(value))' "$WORK_DIR/stack.json" "$@"; }
active_releases=(openzaak openklant)
[ "$(stack objecten)" = true ] && active_releases+=(openobject)
[ "$(stack opennotificaties)" = true ] && active_releases+=(opennotificaties)
[[ ,$PROFILES, == *,openarchiefbeheer,* ]] && active_releases+=(openarchiefbeheer)
[[ ,$PROFILES, == *,openformulieren,* ]] && active_releases+=(openforms)
echo "Services: ${active_releases[*]}${PROFILES:+ (options: $PROFILES)}"

step "Checking the host ports"
python3 "$KIND_DIR/kind_stack.py" ports "$KIND_DIR/ports.json" "$MODE" "$PROFILES" "$WORK_DIR" "$NODE" "$PROXY"

step "Starting the kind cluster '$CLUSTER'"
cluster_pid=
node_restarted=false
if ! kind get clusters 2> /dev/null | grep -qx "$CLUSTER"; then
  kind create cluster --name "$CLUSTER" --config "$KIND_DIR/kind-config.yaml" > "$WORK_DIR/kind.log" 2>&1 &
  cluster_pid=$!
elif [ "$(docker inspect --format '{{.State.Running}}' "$NODE")" != true ]; then
  echo "Starting the stopped cluster"
  docker start "$NODE" > /dev/null
  node_restarted=true
fi
for release in "${active_releases[@]}"; do
  version=$(stack charts "$release" version)
  if [ ! -f "$CHARTS_DIR/$release-$version.tgz" ]; then
    helm pull "$release" --repo "$(stack charts "$release" repository)" --version "$version" --destination "$CHARTS_DIR"
  fi
done
if ! ls "$ROOT"/charts/zac/charts/*.tgz > /dev/null 2>&1; then
  helm repo add opentelemetry https://open-telemetry.github.io/opentelemetry-helm-charts > /dev/null 2>&1 || true
  helm repo add solr https://solr.apache.org/charts > /dev/null 2>&1 || true
  helm dependency build "$ROOT/charts/zac" > /dev/null
fi
if [ -n "$cluster_pid" ]; then
  wait "$cluster_pid" || fail "kind create cluster failed: $(cat "$WORK_DIR/kind.log")"
fi
until kc get --raw /readyz > /dev/null 2>&1; do sleep 2; done
if [ "$node_restarted" = true ]; then
  # ZAC would boot before the services that it needs; it starts again below, once they are ready
  kc -n "$NAMESPACE" scale deployment zac --replicas=0 > /dev/null 2>&1 || true
fi

step "Publishing the ports"
publish_arguments=()
while IFS= read -r argument; do publish_arguments+=("$argument"); done < "$WORK_DIR/publish-arguments"
docker rm --force "$PROXY" > /dev/null 2>&1 || true
docker create --name "$PROXY" --network kind --restart unless-stopped \
  ${publish_arguments[@]+"${publish_arguments[@]}"} "$(stack images nginx)" > /dev/null
docker cp "$WORK_DIR/nginx.conf" "$PROXY:/etc/nginx/nginx.conf"
docker start "$PROXY" > /dev/null

# kind runs containerd with LimitNOFILE=infinity; containers inherit it and uWSGI (Open Zaak, Open Klant, Open Object)
# sizes its tables by RLIMIT_NOFILE, so that one worker can take gigabytes. This restarts containerd, so it goes first.
docker exec "$NODE" bash -c '
  if [ ! -f /etc/systemd/system/containerd.service.d/nofile.conf ]; then
    mkdir -p /etc/systemd/system/containerd.service.d
    printf "[Service]\nLimitNOFILE=1048576\n" > /etc/systemd/system/containerd.service.d/nofile.conf
    systemctl daemon-reload && systemctl restart containerd
  fi'
until docker exec "$NODE" crictl info > /dev/null 2>&1; do sleep 1; done
# the kubelet pulls anonymously unless it gets the Docker credentials, which avoid the Docker Hub rate limit (Docker
# Desktop keeps them in the keychain instead, so there is nothing to copy there)
if [ -f "$HOME/.docker/config.json" ] && grep -q '"auth"' "$HOME/.docker/config.json" &&
  ! docker exec "$NODE" test -f /var/lib/kubelet/config.json; then
  docker cp "$HOME/.docker/config.json" "$NODE:/var/lib/kubelet/config.json"
  docker exec "$NODE" systemctl restart kubelet
fi

step "Creating the configuration, the passwords and the seed data"
kc create namespace "$NAMESPACE" --dry-run=client -o yaml | kc apply -f - > /dev/null
kubectl config set-context "$CONTEXT" --namespace "$NAMESPACE" > /dev/null
kc -n "$NAMESPACE" get secret zac-dev-generated -o json > "$WORK_DIR/generated/existing-secret.json" 2> /dev/null ||
  : > "$WORK_DIR/generated/existing-secret.json"
python3 "$KIND_DIR/kind_stack.py" values "$WORK_DIR/stack.json" "$WORK_DIR" "$MODE" "$PROFILES" \
  "$WORK_DIR/generated/existing-secret.json"
rm -f "$WORK_DIR/generated/existing-secret.json"
SEED=$WORK_DIR/seed.yaml
: > "$SEED"
add() { kc -n "$NAMESPACE" create "$@" --dry-run=client -o yaml >> "$SEED"; echo --- >> "$SEED"; }
add configmap seed-openzaak-dump --from-file="$SEED_DIR/openzaak/00-restore-migrated-database.sql"
add configmap seed-openzaak-data --from-file="$SEED_DIR/openzaak/database"
add configmap seed-openzaak-uploads --from-file="$SEED_DIR/openzaak/fake-test-document.pdf"
add configmap seed-openklant --from-file="$SEED_DIR/openklant"
add configmap seed-objecten-fixtures --from-file="$SEED_DIR/open-object/fixtures"
add configmap seed-pabc-dataset --from-file="$SEED_DIR/pabc/pabc-mapping-data.json"
add configmap seed-zac-database --from-file="$SEED_DIR/zac"
add configmap seed-openformulieren-forms --from-file="$SEED_DIR/openformulieren/forms"
add configmap keycloak-realm --from-file="$WORK_DIR/realm"
for wiremock in smartdocuments-wiremock kvk-wiremock bag-wiremock brp-personen-wiremock; do
  add configmap "$wiremock-mappings" --from-file="$SEED_DIR/$wiremock/mappings"
  if [ -d "$SEED_DIR/$wiremock/__files" ]; then
    add configmap "$wiremock-files" --from-file="$SEED_DIR/$wiremock/__files"
  fi
done
kc -n "$NAMESPACE" apply --server-side --force-conflicts -f "$SEED" \
  -f "$WORK_DIR/generated/secret.json" -f "$WORK_DIR/generated/openzaak-seed-env.json" > /dev/null

# the host names with a dot in the ZAC settings and the seed data resolve to Services in the cluster, and so does the
# host name of Keycloak in the browser (keycloak.localhost), which is in the endpoints of its OpenID configuration
python3 - "$CONTEXT" "$NAMESPACE" "$WORK_DIR/zac-dev.json" << 'PY'
import json, re, subprocess, sys, urllib.parse
context, namespace, zac_dev_values = sys.argv[1:4]
kubectl = ["kubectl", "--context", context, "-n", "kube-system"]
corefile = subprocess.check_output(kubectl + ["get", "cm", "coredns", "-o", "jsonpath={.data.Corefile}"]).decode()
rules = [
    f"rewrite name openzaak.local openzaak-local.{namespace}.svc.cluster.local",
    f"rewrite name open-object.local open-object-local.{namespace}.svc.cluster.local",
    f"rewrite name openklant.local openklant-local.{namespace}.svc.cluster.local",
    f"rewrite name bag-wiremock.local bag-wiremock.{namespace}.svc.cluster.local",
]
keycloak_service = f"keycloak.{namespace}.svc.cluster.local"
keycloak_host = urllib.parse.urlsplit(json.load(open(zac_dev_values))["keycloak"]["hostname"]).hostname
if keycloak_host and "." in keycloak_host:
    rules.append(f"rewrite name {keycloak_host} {keycloak_service}")
stale = re.compile(rf"^ *rewrite name \S+ {re.escape(keycloak_service)}\n", re.M)
cleaned = stale.sub(lambda line: line.group(0) if line.group(0).strip() in rules else "", corefile)
missing = [rule for rule in rules if rule not in cleaned]
if missing or cleaned != corefile:
    corefile = cleaned.replace("ready\n", "ready\n" + "".join(f"    {rule}\n" for rule in missing), 1)
    manifest = subprocess.check_output(kubectl + ["create", "cm", "coredns", f"--from-literal=Corefile={corefile}",
                                                  "--dry-run=client", "-o", "yaml"])
    subprocess.run(kubectl + ["replace", "-f", "-"], input=manifest, check=True, stdout=subprocess.DEVNULL)
    subprocess.run(kubectl + ["rollout", "restart", "deploy/coredns"], check=True, stdout=subprocess.DEVNULL)
PY

zac_image_pid=
if [ "${ZAC_IN_CLUSTER:-}" = local ]; then
  kind load docker-image "$ZAC_DOCKER_IMAGE" --name "$CLUSTER" > "$WORK_DIR/kind-load.log" 2>&1 &
  zac_image_pid=$!
fi

step "Installing the services: ${active_releases[*]} and those of the zac-dev chart"
helm_release zac-dev "$STACK_CHART" "${stack_values[@]}" -f "$WORK_DIR/zac-dev.json" &
install_pids=("$!")
for release in "${active_releases[@]}"; do
  helm_release "$release" "$CHARTS_DIR/$release-$(stack charts "$release" version).tgz" \
    -f "$KIND_DIR/values/$release.yaml" -f "$WORK_DIR/$release.json" &
  install_pids+=("$!")
done
for pid in "${install_pids[@]}"; do wait "$pid"; done
for release in "${UPSTREAM_RELEASES[@]}"; do
  [[ " ${active_releases[*]} " == *" $release "* ]] || helm_uninstall "$release"
done

python3 "$KIND_DIR/kind_stack.py" wait "$CONTEXT" "$NAMESPACE" 600 "$(docker inspect --format '{{.State.StartedAt}}' "$NODE")" \
  postgres > /dev/null
kc -n "$NAMESPACE" exec -i deploy/postgres -- psql --username postgres --quiet --set ON_ERROR_STOP=1 \
  < "$WORK_DIR/generated/roles.sql" > /dev/null

if [ "$MODE" = zac ]; then
  zac_values=(-f "$KIND_DIR/values/zac.yaml" -f "$WORK_DIR/zac.json"
    --set "image.repository=${ZAC_DOCKER_IMAGE%:*}" --set "image.tag=${ZAC_DOCKER_IMAGE##*:}")
  if [ "$ZAC_IN_CLUSTER" = local ]; then
    zac_values+=(--set image.pullPolicy=Never
      --set-string "podAnnotations.zac-image-id=$(docker image inspect --format '{{.Id}}' "$ZAC_DOCKER_IMAGE")")
  else
    zac_values+=(--set image.pullPolicy=Always --set-string "podAnnotations.zac-started-at=$START")
  fi

  # ZAC records boot errors and stays unready until the liveness probe restarts it when Keycloak, Open Zaak, PABC or
  # its OPA sidecar are not there yet when it boots. The kubelet starts the ZAC container before it has pulled the OPA
  # image, so that image goes into the node first.
  step "Waiting for the services that ZAC needs"
  python3 "$KIND_DIR/kind_stack.py" wait "$CONTEXT" "$NAMESPACE" 1200 \
    "$(docker inspect --format '{{.State.StartedAt}}' "$NODE")" postgres keycloak openzaak openzaak-nginx pabc-api solr
  helm template zac "$ROOT/charts/zac" --namespace "$NAMESPACE" "${zac_values[@]}" > "$WORK_DIR/zac-manifest.yaml"
  zac_pod_images=$(python3 - "$WORK_DIR/zac-manifest.yaml" << 'PY'
import re, sys
for document in open(sys.argv[1]).read().split("\n---"):
    if re.search(r"^kind: Deployment$", document, re.M) and re.search(r"^  name: zac$", document, re.M):
        print("\n".join(sorted(set(re.findall(r'^ *(?:- )?image: *"?([^" \n]+)', document, re.M)))))
PY
  )
  for image in $zac_pod_images; do
    if [ "$image" != "$ZAC_DOCKER_IMAGE" ] && ! docker exec "$NODE" crictl inspecti "$image" > /dev/null 2>&1; then
      docker exec "$NODE" crictl pull "$image" > /dev/null
    fi
  done
  if [ -n "$zac_image_pid" ]; then
    wait "$zac_image_pid" || fail "kind load docker-image failed: $(cat "$WORK_DIR/kind-load.log")"
  fi

  step "Installing ZAC ($ZAC_DOCKER_IMAGE)"
  helm_release zac "$ROOT/charts/zac" "${zac_values[@]}"
else
  helm_uninstall zac
fi

step "Waiting until every service is ready"
python3 "$KIND_DIR/kind_stack.py" wait "$CONTEXT" "$NAMESPACE" 1200 "$(docker inspect --format '{{.State.StartedAt}}' "$NODE")"
kc -n "$NAMESPACE" wait --for=condition=complete job --selector=app.kubernetes.io/component=seed --timeout=15m > /dev/null

step "The stack is ready"
while IFS='=' read -r name port; do
  printf '  %-28s localhost:%s\n' "$(echo "${name#HOST_PORT_}" | tr '[:upper:]_' '[:lower:]-')" "$port"
done < "$WORK_DIR/ports.env"
keycloak_url=$(python3 -c 'import json, sys; print(json.load(open(sys.argv[1]))["keycloak"]["hostname"])' \
  "$WORK_DIR/zac-dev.json")
echo
echo "Keycloak is at $keycloak_url, also for the pods in the cluster; its admin console is at $keycloak_url/admin."
echo "The admin user of Keycloak and of the admin pages of Open Zaak and the other components is 'admin', with the"
echo "password $(sed -n 's/^ADMIN_PASSWORD=//p' "$WORK_DIR/generated/summary.env")."
if [ "$MODE" = ide ]; then
  echo
  echo "ZAC is not running in the cluster. Run it from your IDE with the environment of .env.example."
  if grep -qv '^ADMIN_PASSWORD=' "$WORK_DIR/generated/summary.env"; then
    echo "These passwords of the stack differ from your environment; set them for ZAC, or set them in .env and run"
    echo "./start-kind.sh again:"
    grep -v '^ADMIN_PASSWORD=' "$WORK_DIR/generated/summary.env" | sed 's/^/  /'
  fi
fi

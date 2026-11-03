#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
set -euo pipefail

help() {
  echo "Starts the local ZAC development stack in a kind cluster, using the 1Password CLI tools to retrieve secrets."
  echo "See docs/development/installKind.md."
  echo
  echo "Syntax: $0 [-d|e|h|z|b|l|t|o|n|a|f|v]"
  echo
  echo "Without -z, -b or -l everything but ZAC starts, so that ZAC can run from an IDE against the cluster."
  echo "Every start makes the cluster match the options: services of options that are left out are removed."
  echo
  echo "General:"
  echo "   -d     Delete the kind cluster and its data before starting, to start from the seed data again."
  echo "   -e     Do not use the 1Password CLI tools to retrieve secrets (set the environment variables yourself)."
  echo "   -h     Print this help."
  echo
  echo "ZAC options:"
  echo "   -z     Start ZAC from the image in the registry (\$ZAC_DOCKER_IMAGE, default ghcr.io/infonl/zaakafhandelcomponent:latest)."
  echo "   -b     Build the ZAC Docker image and start it."
  echo "   -l     Start the locally built ZAC Docker image (ghcr.io/infonl/zaakafhandelcomponent:dev)."
  echo
  echo "Additional components:"
  echo "   -t     Start the services of the integration tests."
  echo "   -o     Start Open Object (required e.g. for the Dimpact productaanvraag flow)."
  echo "   -n     Start Open Notificaties."
  echo "   -a     Start Open Archiefbeheer."
  echo "   -f     Start Open Formulieren (also starts Open Object and Open Notificaties)."
  echo "   -v     Start VNG Selectielijst."
  echo
  echo "Environment variables:"
  echo "   KIND_CLUSTER_NAME    name of the kind cluster (default: zac)"
  echo "   KIND_PORT_OFFSET     added to every host port (default: 0)"
  echo "   KIND_PORT_<NAME>     the host port of one service, or 0 to not publish it, for example KIND_PORT_ZAC=18080"
}

zacInCluster=""
deleteCluster=false
disableOnePassword=false
profiles=()
while getopts ':dehzbltonafv' option; do
  case $option in
    d) deleteCluster=true ;;
    e) disableOnePassword=true ;;
    h) help; exit ;;
    z | b | l)
      if [ -n "$zacInCluster" ]; then
        echo "Only one of -z, -b and -l can be specified." >&2
        exit 1
      fi
      case $option in z) zacInCluster=registry ;; b) zacInCluster=build ;; l) zacInCluster=local ;; esac
      ;;
    t) profiles+=(itest) ;;
    o) profiles+=(objecten) ;;
    n) profiles+=(opennotificaties) ;;
    a) profiles+=(openarchiefbeheer) ;;
    f) profiles+=(openformulieren) ;;
    v) profiles+=(vngselectielijst) ;;
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

op_command=()
if [ "$disableOnePassword" = false ]; then
  if ! command -v op > /dev/null 2>&1; then
    echo "1Password CLI ('op') not found. Only using the environment variables that are set."
  elif op vault get Dimpact > /dev/null 2>&1; then
    echo "Using the 1Password CLI tools to retrieve secrets from the vault 'Dimpact' ..."
    op_command=(op run --env-file=./.env.tpl --no-masking --)
  else
    echo "No access to the 1Password vault 'Dimpact'. Only using the environment variables that are set."
  fi
fi

profilesList=$(IFS=,; echo "${profiles[*]:-}")
export APP_ENV=devlocal ZAC_IN_CLUSTER=$zacInCluster KIND_PROFILES=$profilesList
exec ${op_command[@]+"${op_command[@]}"} ./scripts/kind/up.sh

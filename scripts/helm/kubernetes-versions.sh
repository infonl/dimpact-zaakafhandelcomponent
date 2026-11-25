#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Prints the Kubernetes versions to validate the ZAC Helm chart against, as `kubernetes_versions=<JSON array>`,
# so that a GitHub workflow can append the output to $GITHUB_OUTPUT and use it as a matrix:
# - the latest stable Kubernetes release and the three minor versions before it, the releases that the Kubernetes
#   project supports;
# - the Kubernetes version of the kind node image, which follows the next Dimpact PodiumD release, also when it is
#   older than those.

set -euo pipefail

LATEST_STABLE_KUBERNETES_VERSION="v1.37.2" # latest stable Kubernetes version updated by renovate
OLDER_MINOR_VERSIONS=3

repository_root="$(cd "$(dirname "$0")/../.." && pwd)"
podiumd_version="$(grep -oE 'kindest/node:v[0-9]+\.[0-9]+\.[0-9]+' "$repository_root/scripts/kind/kind-config.yaml" | cut -d: -f2 | tr -d v)"

if [[ -z "$podiumd_version" ]]; then
  echo "Could not read the Kubernetes version from scripts/kind/kind-config.yaml" >&2
  exit 1
fi

stable_version="${LATEST_STABLE_KUBERNETES_VERSION#v}"
IFS=. read -r stable_major stable_minor _ <<< "$stable_version"
podiumd_minor="$(cut -d. -f2 <<< "$podiumd_version")"

versions=("$stable_version")
for ((minor = stable_minor - 1; minor >= stable_minor - OLDER_MINOR_VERSIONS; minor--)); do
  if [[ "$minor" -eq "$podiumd_minor" ]]; then
    versions+=("$podiumd_version")
  else
    versions+=("$stable_major.$minor.0")
  fi
done
if [[ "$podiumd_minor" -lt "$((stable_minor - OLDER_MINOR_VERSIONS))" || "$podiumd_minor" -gt "$stable_minor" ]]; then
  versions+=("$podiumd_version")
fi

printf 'kubernetes_versions=['
printf '"%s",' "${versions[@]}" | sed 's/,$//'
printf ']\n'

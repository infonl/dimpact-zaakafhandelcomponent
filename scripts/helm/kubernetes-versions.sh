#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Prints the Kubernetes versions to validate the ZAC Helm chart against, as `kubernetes_versions=<JSON array>`,
# so that a GitHub workflow can append the output to $GITHUB_OUTPUT and use it as a matrix:
# - the .0 release of the latest stable minor version and of the three minor versions before it, the releases that
#   the Kubernetes project supports. A patch release does not add or remove Kubernetes APIs, so the .0 release
#   stands for its whole minor version;
# - the exact Kubernetes version of the kind node image, which follows the next Dimpact PodiumD release.

set -euo pipefail

# Only the minor version is used. Renovate updates it to a new minor version only.
LATEST_STABLE_KUBERNETES_VERSION="v1.37.0" # latest stable Kubernetes version updated by renovate
OLDER_MINOR_VERSIONS=3

repository_root="$(cd "$(dirname "$0")/../.." && pwd)"
podiumd_version="$(grep -oE 'kindest/node:v[0-9]+\.[0-9]+\.[0-9]+' "$repository_root/scripts/kind/kind-config.yaml" | cut -d: -f2 | tr -d v)"

if [[ -z "$podiumd_version" ]]; then
  echo "Could not read the Kubernetes version from scripts/kind/kind-config.yaml" >&2
  exit 1
fi

IFS=. read -r stable_major stable_minor _ <<< "${LATEST_STABLE_KUBERNETES_VERSION#v}"

versions=()
for ((minor = stable_minor; minor >= stable_minor - OLDER_MINOR_VERSIONS; minor--)); do
  versions+=("$stable_major.$minor.0")
done
if [[ ! " ${versions[*]} " =~ " $podiumd_version " ]]; then
  versions+=("$podiumd_version")
fi

printf 'kubernetes_versions=['
printf '"%s",' "${versions[@]}" | sed 's/,$//'
printf ']\n'

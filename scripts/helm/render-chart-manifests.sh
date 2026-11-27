#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Renders the ZAC Helm chart for the given Kubernetes version into the given directory, once with its default
# values (default.yaml) and once with all optional parts turned on (all-features.yaml). The Kubernetes version
# matters, because templates such as the ingress choose their API by it.
#
# Run `helm dependency build charts/zac` first.

set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <Kubernetes version> <output directory>" >&2
  exit 1
fi

kubernetes_version="$1"
output_directory="$2"
chart="$(cd "$(dirname "$0")/../.." && pwd)/charts/zac"

mkdir -p "$output_directory"

helm template zac "$chart" --kube-version "$kubernetes_version" \
  --values "$chart/ci/zac-chart-test-values.yaml" \
  > "$output_directory/default.yaml"

helm template zac "$chart" --kube-version "$kubernetes_version" \
  --values "$chart/ci/zac-chart-test-values.yaml" \
  --values "$chart/tests/values/all-features.yaml" \
  > "$output_directory/all-features.yaml"

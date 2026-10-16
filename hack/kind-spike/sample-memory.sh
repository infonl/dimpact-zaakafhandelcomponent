#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# SPIKE (PZ-13059): samples the memory use of the kind node container every few seconds until killed.
# Usage: sample-memory.sh <output tsv> [<container>]
set -uo pipefail
OUTPUT=$1
CONTAINER=${2:-zac-spike-control-plane}
printf 'epoch\tnode_mem_usage\tnode_cpu\n' > "$OUTPUT"
while true; do
  stats=$(docker stats --no-stream --format '{{.MemUsage}}\t{{.CPUPerc}}' "$CONTAINER" 2>/dev/null | head -1)
  [ -n "$stats" ] && printf '%s\t%s\n' "$(date +%s)" "$stats" >> "$OUTPUT"
  sleep 3
done

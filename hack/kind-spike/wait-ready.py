#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# SPIKE (PZ-13059): waits until every Deployment and StatefulSet in the podiumd and zac namespaces is ready and
# every Job has completed, and prints when each of them got there (seconds after this script started).
# Usage: wait-ready.py <kube context> <timeout seconds> [<start epoch seconds>]
import json
import subprocess
import sys
import time

context, timeout = sys.argv[1], int(sys.argv[2])
namespaces = ["podiumd", "zac"]
start = float(sys.argv[3]) if len(sys.argv) > 3 else time.time()
ready_at = {}


def kubectl_json(*arguments):
    output = subprocess.run(["kubectl", "--context", context, *arguments, "-o", "json"],
                            capture_output=True, text=True, check=False).stdout
    return json.loads(output)["items"] if output else []


def workload_states():
    states = {}
    for namespace in namespaces:
        for kind in ("deployments", "statefulsets"):
            for item in kubectl_json("get", kind, "-n", namespace):
                wanted = item["spec"].get("replicas", 1)
                ready = item.get("status", {}).get("readyReplicas", 0) or 0
                states[f"{namespace}/{item['metadata']['name']}"] = ready >= wanted
        for job in kubectl_json("get", "jobs", "-n", namespace):
            if any(owner["kind"] == "CronJob" for owner in job["metadata"].get("ownerReferences", [])):
                continue
            states[f"{namespace}/job/{job['metadata']['name']}"] = (job.get("status", {}).get("succeeded") or 0) > 0
    return states


while True:
    states = workload_states()
    elapsed = int(time.time() - start)
    for name, is_ready in states.items():
        if is_ready and name not in ready_at:
            ready_at[name] = elapsed
            print(f"ready:{name}\t-\t{elapsed}", flush=True)
    # the PodiumD Keycloak StatefulSet only appears once the operator has reconciled the Keycloak CR
    keycloak_present = any(name.startswith("podiumd/keycloak") and "operator" not in name for name in states)
    if states and keycloak_present and all(states.values()):
        break
    if elapsed > timeout:
        not_ready = sorted(name for name, is_ready in states.items() if not is_ready)
        print(f"TIMEOUT after {elapsed}s; not ready: {not_ready}", flush=True)
        subprocess.run(["kubectl", "--context", context, "get", "pods", "-A", "-o", "wide"])
        sys.exit(1)
    time.sleep(2)

restarts = []
for namespace in namespaces:
    for pod in kubectl_json("get", "pods", "-n", namespace):
        for container in pod.get("status", {}).get("containerStatuses", []):
            if container.get("restartCount"):
                restarts.append(f"{namespace}/{pod['metadata']['name']}/{container['name']}={container['restartCount']}")
print(f"restarts\t-\t{' '.join(restarts) or 'none'}", flush=True)

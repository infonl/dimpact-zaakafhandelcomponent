#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# SPIKE (PZ-13059): derives the ZAC layer of the PodiumD 'podiumd' realm from the Docker Compose realm
# export: the test roles, groups and users, as a Keycloak partial import. The clients come from PodiumD.
# Usage: zac-realm-layer.py <realm export json> <docker-compose.yaml>  > zac-realm-layer.json
import json
import re
import sys

realm_file, compose_file = sys.argv[1], sys.argv[2]

# the e-mail addresses of the test users and groups: '- ZAC_X=${DOCKER_COMPOSE_X:-default}' in docker-compose.yaml
defaults = dict(re.findall(r"- (ZAC_[A-Z0-9_]+)=\$\{[A-Z0-9_]+:-([^}]*)\}", open(compose_file).read()))
text = re.sub(r"\$\{(ZAC_[A-Z0-9_]+)\}", lambda match: defaults[match.group(1)], open(realm_file).read())
realm = json.loads(text)

BUILT_IN_ROLES = {"offline_access", "uma_authorization", "default-roles-zac"}


def without_ids(value):
    if isinstance(value, dict):
        return {key: without_ids(item) for key, item in value.items() if key not in ("id", "containerId")}
    if isinstance(value, list):
        return [without_ids(item) for item in value]
    return value


roles = [role for role in realm["roles"]["realm"] if role["name"] not in BUILT_IN_ROLES]
users = []
for user in realm["users"]:
    if user["username"].startswith("service-account-"):
        continue
    user = dict(user)
    user["realmRoles"] = [
        "default-roles-podiumd" if role == "default-roles-zac" else role for role in user.get("realmRoles") or []
    ]
    # client roles of the Docker Compose 'pabc' client; the PodiumD realm has its own PABC clients
    user.pop("clientRoles", None)
    user["requiredActions"] = []
    users.append(user)

print(json.dumps({
    "ifResourceExists": "SKIP",
    "roles": {"realm": without_ids(roles)},
    "groups": without_ids(realm["groups"]),
    "users": without_ids(users),
}, indent=2))

/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { readFileSync } from "node:fs";
import path from "node:path";
import type { Account, Target } from "./target";

const REPOSITORY_ROOT = path.resolve(__dirname, "../../../..");

function zacUrlOfKindCluster(clusterName: string) {
  const portsFile = path.join(
    REPOSITORY_ROOT,
    "build/kind",
    clusterName,
    "ports.env",
  );

  let ports: string;
  try {
    ports = readFileSync(portsFile, "utf8");
  } catch {
    throw new Error(
      `The kind cluster "${clusterName}" has no ports file at ${portsFile}. ` +
        `Start the stack with "./start-kind.sh -b -t", or set ZAC_URL.`,
    );
  }

  const zacPort = /^HOST_PORT_ZAC=(\d+)$/m.exec(ports)?.[1];
  if (!zacPort) {
    throw new Error(
      `ZAC does not run in the kind cluster "${clusterName}": ${portsFile} has no HOST_PORT_ZAC. ` +
        `Start the stack with "./start-kind.sh -b -t", or set ZAC_URL.`,
    );
  }
  return `http://localhost:${zacPort}`;
}

// The Keycloak realm seed in scripts/kind/seed/keycloak gives every test user a password that equals the username.
function seededAccount(username: string, displayName: string): Account {
  return { username, password: username, displayName };
}

export const kind: Target = (environment) => ({
  zacUrl:
    environment.ZAC_URL ??
    zacUrlOfKindCluster(environment.KIND_CLUSTER_NAME ?? "zac"),
  accounts: {
    behandelaar: seededAccount("behandelaar1", "Test Behandelaar 1"),
    coordinator: seededAccount("coordinator1", "Test Coordinator 1"),
    recordmanager: seededAccount("recordmanager1", "Test Recordmanager 1"),
    beheerder: seededAccount("beheerder1", "Test Beheerder 1"),
    raadpleger: seededAccount("raadpleger1", "Test Raadpleger 1"),
  },
});

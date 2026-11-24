/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { readFileSync } from "node:fs";
import path from "node:path";
import type { Account, Target, UnknownZacUrl } from "./target";

const REPOSITORY_ROOT = path.resolve(__dirname, "../../../..");
// The host ports of greenmail-api and open-object in scripts/kind/ports.json.
const DEFAULT_GREENMAIL_API_PORT = "18083";
const DEFAULT_OPEN_OBJECT_PORT = "8010";

function portsFileOf(clusterName: string) {
  return path.join(REPOSITORY_ROOT, "build/kind", clusterName, "ports.env");
}

/** The ports that the cluster publishes on the host, or undefined when the cluster has no ports file. */
function publishedPortsOf(clusterName: string) {
  try {
    return readFileSync(portsFileOf(clusterName), "utf8");
  } catch {
    return undefined;
  }
}

function hostPort(ports: string | undefined, service: string) {
  return new RegExp(`^HOST_PORT_${service}=(\\d+)$`, "m").exec(
    ports ?? "",
  )?.[1];
}

function zacUrlOfKindCluster(clusterName: string): string | UnknownZacUrl {
  const portsFile = portsFileOf(clusterName);
  const ports = publishedPortsOf(clusterName);
  if (ports === undefined) {
    return {
      unknownBecause:
        `The kind cluster "${clusterName}" has no ports file at ${portsFile}. ` +
        `Start the stack with "./start-kind.sh -b -t -n", or set ZAC_URL.`,
    };
  }

  const zacPort = hostPort(ports, "ZAC");
  if (!zacPort) {
    return {
      unknownBecause:
        `ZAC does not run in the kind cluster "${clusterName}": ${portsFile} has no HOST_PORT_ZAC. ` +
        `Start the stack with "./start-kind.sh -b -t -n", or set ZAC_URL.`,
    };
  }
  return `http://localhost:${zacPort}`;
}

function urlOfService(
  clusterName: string,
  service: string,
  defaultPort: string,
) {
  const port = hostPort(publishedPortsOf(clusterName), service) ?? defaultPort;
  return `http://localhost:${port}`;
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
  // The realm of the kind stack has the default theme of Keycloak and no internationalisation.
  signInPage: {
    heading: "Sign in to your account",
    usernameField: "Username or email",
    passwordField: "Password",
    signInButton: "Sign In",
    invalidCredentialsMessage: "Invalid username or password.",
  },
  // scripts/kind/seed/bag-wiremock only answers this search text.
  bag: { searchText: "fake search text", adres: "Dam 1, 1012JS Amsterdam" },
  // The test doubles of './start-kind.sh -t', with the settings of scripts/kind/values/zac-e2e.yaml.
  testDoubles: {
    mail: {
      apiUrl: `${urlOfService(environment.KIND_CLUSTER_NAME ?? "zac", "GREENMAIL_API", DEFAULT_GREENMAIL_API_PORT)}/api`,
    },
    // The object type and the token are in scripts/kind/seed/open-object, the document in scripts/kind/seed/openzaak.
    objecten: {
      apiUrl: `${urlOfService(environment.KIND_CLUSTER_NAME ?? "zac", "OPEN_OBJECT", DEFAULT_OPEN_OBJECT_PORT)}/api/v2`,
      token: "fakeOpenFormulierenObjectsToken",
      productaanvraagObjecttype: "021f685e-9482-4620-b157-34cd4003da6b",
      aanvraagdocument: {
        url: "http://openzaak.local:8000/documenten/api/v1/enkelvoudiginformatieobjecten/37e16ddc-7992-418c-b5a0-54cf6680329e",
        titel: "Fake test document",
      },
    },
  },
  seed: {
    // The BRP test double knows this person, and scripts/kind/seed/openklant/1-setup-applicatie.sql gives it this
    // preferred e-mail address.
    persoon: { bsn: "999992958", emailadres: "anita.van.buren@example.com" },
  },
  runsLiveScenarios: false,
});

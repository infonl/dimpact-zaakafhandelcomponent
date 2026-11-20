/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, test } from "@playwright/test";
import { loadEnvironment } from "./env";
import type { Target } from "./targets/target";

const account = { username: "user", password: "secret", displayName: "User" };

const validTarget: Target = (environment) => ({
  zacUrl: environment.ZAC_URL ?? { unknownBecause: "no ZAC_URL" },
  accounts: { behandelaar: account },
  signInPage: {
    heading: "Inloggen",
    usernameField: "Gebruikersnaam",
    passwordField: "Wachtwoord",
    signInButton: "Inloggen",
    invalidCredentialsMessage: "Onjuiste gebruikersnaam of wachtwoord.",
  },
  runsLiveScenarios: true,
});

test("given no E2E_TARGET, when the environment loads, then it uses the profile kind", () => {
  expect(loadEnvironment({}, { kind: validTarget }).target).toBe("kind");
});

test("given an unknown E2E_TARGET, when the environment loads, then it fails with the known targets", () => {
  expect(() =>
    loadEnvironment({ E2E_TARGET: "acceptatie" }, { kind: validTarget }),
  ).toThrow('Unknown E2E_TARGET "acceptatie". Known targets: kind.');
});

test("given a profile with the settings of a target, when the environment loads, then it exposes them", () => {
  const environment = loadEnvironment(
    { ZAC_URL: "https://zac.example.com" },
    { kind: validTarget },
  );

  expect(environment).toMatchObject({
    zacUrl: "https://zac.example.com",
    accounts: { behandelaar: account },
    runsLiveScenarios: true,
    runsInCi: false,
  });
});

test("given a target that cannot tell the URL of ZAC, when the environment loads, then it keeps the reason and does not fail", () => {
  const environment = loadEnvironment({}, { kind: validTarget });

  expect(environment.zacUrl).toBeUndefined();
  expect(environment.zacUrlUnknownBecause).toBe("no ZAC_URL");
});

test("given an account without a password, when the environment loads, then it fails and names the role", () => {
  const targetWithoutPassword: Target = (environment) => ({
    ...validTarget(environment),
    accounts: { coordinator: { ...account, password: "" } },
  });

  expect(() => loadEnvironment({}, { kind: targetWithoutPassword })).toThrow(
    /accounts\.coordinator\.password/,
  );
});

test("given a CI value that is not a boolean, when the environment loads, then it fails", () => {
  expect(() =>
    loadEnvironment({ CI: "yes please" }, { kind: validTarget }),
  ).toThrow('CI has the value "yes please"');
});

test("given CI=true, when the environment loads, then the run is a CI run", () => {
  expect(loadEnvironment({ CI: "true" }, { kind: validTarget }).runsInCi).toBe(
    true,
  );
});

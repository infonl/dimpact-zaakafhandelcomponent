/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { Account, Target } from "./target";

const NO_ZAAKTYPE_OF_OUR_OWN =
  "the environment has no zaaktype for it that only the end-to-end tests use.";

function account(
  username: string | undefined,
  password: string | undefined,
  displayName: string,
): Account | undefined {
  return username && password ? { username, password, displayName } : undefined;
}

/** The TEST environment of the team, with the real BRP, KVK and BAG. */
export const dev: Target = (environment) => {
  // Both users are behandelaar and beheerder of every zaaktype. ZAC lets a beheerder do what a coordinator and a
  // recordmanager do, so these accounts do not prove the rights of those roles.
  const user1 = account(
    environment.E2E_TEST_USER_1_USERNAME,
    environment.E2E_TEST_USER_1_PASSWORD,
    "E2etest User1",
  );
  const user2 = account(
    environment.E2E_TEST_USER_2_USERNAME,
    environment.E2E_TEST_USER_2_PASSWORD,
    "E2etest User2",
  );

  return {
    zacUrl: environment.ZAC_URL ?? { unknownBecause: "Set ZAC_URL." },
    accounts: {
      ...(user1 && {
        behandelaar: user1,
        beheerder: user1,
        recordmanager: user1,
      }),
      ...(user2 && { coordinator: user2 }),
    },
    signInPage: {
      heading: "Sign in to your account",
      usernameField: "Username or email",
      passwordField: "Password",
      signInButton: "Sign In",
      invalidCredentialsMessage: "Invalid username or password.",
    },
    bag: { searchText: "Dam 1 Amsterdam", adres: "Dam 1, 1012JS Amsterdam" },
    names: {
      "Test zaaktype 2": "Zaaktype voor e2e testen",
      "Test zaaktype 3": { missingBecause: NO_ZAAKTYPE_OF_OUR_OWN },
      "Test zaaktype 4": { missingBecause: NO_ZAAKTYPE_OF_OUR_OWN },
      // "Zaaktype voor BPMN e2e testen" has zaken in another process, and ZAC then does not change its process.
      "BPMN test zaaktype 5": { missingBecause: NO_ZAAKTYPE_OF_OUR_OWN },
      "Test group behandelaars domein test 1": "Test groep A",
      "Test group behandelaars with a very long group name": "Test groep B",
      Toegekend: "Verleend",
      "Besluit aansprakelijkstelling": "Besluit na heroverweging",
      besluit: "bijlage",
      "12345678": "68750110",
      fakeNaam1: "Test BV Donald",
    },
    seed: {
      // A person of the test data of the BRP, with this preferred e-mail address in the Open Klant of the environment.
      persoon: { bsn: "999993896", emailadres: "hendrika.janse@example.com" },
    },
    runsLiveScenarios: true,
    openFormsUrl: environment.OPEN_FORMS_URL,
    // The nightly run publishes its report on GitHub Pages.
    recordsTraces: false,
    timeouts: { testMilliseconds: 300_000, expectMilliseconds: 30_000 },
  };
};

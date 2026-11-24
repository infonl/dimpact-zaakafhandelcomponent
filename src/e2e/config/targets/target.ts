/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { z } from "zod";

export const ROLES = [
  "behandelaar",
  "coordinator",
  "recordmanager",
  "beheerder",
  "raadpleger",
] as const;
export type Role = (typeof ROLES)[number];

const accountSchema = z.object({
  username: z.string().min(1),
  password: z.string().min(1),
  displayName: z.string().min(1),
});
export type Account = z.infer<typeof accountSchema>;

const unknownZacUrlSchema = z.object({ unknownBecause: z.string().min(1) });
export type UnknownZacUrl = z.infer<typeof unknownZacUrlSchema>;

const signInPageSchema = z.object({
  heading: z.string().min(1),
  usernameField: z.string().min(1),
  passwordField: z.string().min(1),
  signInButton: z.string().min(1),
  invalidCredentialsMessage: z.string().min(1),
});
export type SignInPage = z.infer<typeof signInPageSchema>;

const bagSchema = z.object({
  searchText: z.string().min(1),
  adres: z.string().min(1),
});

const nameSchema = z.union([
  z.string().min(1),
  z.object({ missingBecause: z.string().min(1) }),
]);
export type Names = Record<string, z.infer<typeof nameSchema>>;

const testDoublesSchema = z.object({
  /** The REST API of the mail server that ZAC sends its mail to. */
  mail: z.object({ apiUrl: z.url() }),
  /** The Objecten API that tells ZAC about new productaanvragen, and a document for their aanvraag. */
  objecten: z.object({
    apiUrl: z.url(),
    token: z.string().min(1),
    productaanvraagObjecttype: z.uuid(),
    aanvraagdocument: z.object({ url: z.url(), titel: z.string().min(1) }),
  }),
});
export type TestDoubles = z.infer<typeof testDoublesSchema>;

const seedSchema = z.object({
  persoon: z.object({ bsn: z.string().min(1), emailadres: z.email() }),
});
export type Seed = z.infer<typeof seedSchema>;

const timeoutsSchema = z.object({
  testMilliseconds: z.int().positive(),
  expectMilliseconds: z.int().positive(),
});

export const targetSettingsSchema = z.object({
  zacUrl: z.union([z.url(), unknownZacUrlSchema]),
  accounts: z.partialRecord(z.enum(ROLES), accountSchema),
  signInPage: signInPageSchema,
  bag: bagSchema,
  testDoubles: testDoublesSchema.optional(),
  names: z.record(z.string(), nameSchema).default({}),
  seed: seedSchema.optional(),
  runsLiveScenarios: z.boolean(),
  openFormsUrl: z.url().optional(),
  hasPublicReport: z.boolean().default(false),
  timeouts: timeoutsSchema.default({
    testMilliseconds: 120_000,
    expectMilliseconds: 10_000,
  }),
});
export type TargetSettings = z.infer<typeof targetSettingsSchema>;

/**
 * A target is the place where ZAC runs. It gets the environment variables and returns its settings,
 * which `config/env.ts` validates. A target holds no secrets: it reads them from the environment.
 *
 * - `accounts`: leave out a role whose secrets are not set; its scenarios are then skipped.
 * - `signInPage`: the texts of the Keycloak login page of the target, which depend on its theme and language.
 * - `bag`: a text to search the BAG with, and an adres that the BAG of the target then finds.
 * - `names`: how the target calls what the feature files name after the kind stack, such as a zaaktype, a groep or a
 *   bedrijf. A name that is not in it is the same in the target. A scenario that uses a name with `missingBecause` is
 *   skipped for that reason.
 * - `testDoubles`: what the test doubles of the target answer. A target without test doubles skips the steps that
 *   need them.
 * - `seed`: data that the target has before the tests run. `persoon` is a person that the BRP knows and that has
 *   a preferred e-mail address in Open Klant. A target without a seed skips the steps that need it.
 * - `runsLiveScenarios`: whether the scenarios tagged `@live-env-only`, which need real external systems, run.
 * - `openFormsUrl`: the URL of Open Formulieren. A target without it skips the scenarios that need it.
 * - `hasPublicReport`: whether anyone can read the report of a run. Such a run records everything for every scenario,
 *   except for the scenarios that sign in, because their recordings hold passwords.
 * - `timeouts`: how long a test and an assertion may take. Set them for a target that is slower than the kind stack.
 *
 * When a target cannot tell the URL of ZAC, it says why, so that tools that do not run tests, such as `bddgen`,
 * still load the configuration.
 */
export type Target = (
  environment: NodeJS.ProcessEnv,
) => z.input<typeof targetSettingsSchema>;

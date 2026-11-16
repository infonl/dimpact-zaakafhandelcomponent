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

export const targetSettingsSchema = z.object({
  zacUrl: z.union([z.url(), unknownZacUrlSchema]),
  accounts: z.partialRecord(z.enum(ROLES), accountSchema),
});
export type TargetSettings = z.infer<typeof targetSettingsSchema>;

/**
 * A target is the place where ZAC runs. It gets the environment variables and returns its settings,
 * which `config/env.ts` validates. A target holds no secrets: it reads them from the environment.
 * When a target cannot tell the URL of ZAC, it says why, so that tools that do not run tests, such as `bddgen`,
 * still load the configuration.
 */
export type Target = (environment: NodeJS.ProcessEnv) => {
  zacUrl: string | UnknownZacUrl | undefined;
  accounts: Partial<Record<Role, Partial<Account>>>;
};

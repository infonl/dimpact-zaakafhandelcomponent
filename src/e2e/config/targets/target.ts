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

export const targetSettingsSchema = z.object({
  zacUrl: z.url(),
  accounts: z.partialRecord(z.enum(ROLES), accountSchema),
});
export type TargetSettings = z.infer<typeof targetSettingsSchema>;

/**
 * A target is the place where ZAC runs. It gets the environment variables and returns its settings,
 * which `config/env.ts` validates. A target holds no secrets: it reads them from the environment.
 */
export type Target = (environment: NodeJS.ProcessEnv) => {
  zacUrl: string | undefined;
  accounts: Partial<Record<Role, Partial<Account>>>;
};

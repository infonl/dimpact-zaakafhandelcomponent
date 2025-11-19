/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import "dotenv/config";
import z from "zod";

const userSchema = z.object({
  username: z.string(),
  password: z.string(),
});
export type User = z.infer<typeof userSchema>;

const envSchema = z.object({
  businessLanguage: z.string().refine((val) => ["en"].includes(val)),
  baseUrl: z.url(),
  users: z.record(z.string(), userSchema),
});
export type Env = z.infer<typeof envSchema>;

export const DEFAULT_USER = "DEFAULT_USER";

export const ENV = envSchema.parse({
  businessLanguage: process.env.BUSINESS_LANGUAGE ?? "en",
  baseUrl: process.env.ZAC_URL,
  users: {
    [DEFAULT_USER]: {
      username: process.env.BEHEERDER1NEWIAM_USERNAME,
      password: process.env.BEHEERDER1NEWIAM_USERNAME,
    },
    beheerder1newiam: {
      username: process.env.BEHEERDER1NEWIAM_USERNAME,
      password: process.env.BEHEERDER1NEWIAM_USERNAME,
    },
    thisuserdoesnotexist: {
      username: process.env.NON_EXISTING_USER_USERNAME,
      password: process.env.NON_EXISTING_USER_PASSWORD,
    },
  },
} satisfies Env);

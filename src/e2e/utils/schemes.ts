/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  ICreateAttachment,
  ICreateLog,
} from "@cucumber/cucumber/lib/runtime/attachment_manager";
import { z } from "zod";

export const worldPossibleZacUrls = z.enum(["zac"]);
export const worldUsers = z.enum(["Bob", "Oscar"]);
export const zaakStatus = z.enum([
  "Intake",
  "Wacht op aanvullende informatie",
  "In behandeling",
  "Afgerond",
]);
export const zaakResult = z.enum(["Verleend"]);

export const worldParametersScheme = z.object({
  attach: z.any().refine((val): val is ICreateAttachment => {
    return typeof val === "function";
  }),
  log: z.custom<ICreateLog>((val) => typeof val === "function"),
  parameters: z.object({
    urls: z.object({
      zac: z.string(),
      openForms: z.string().optional(),
    }),
    users: z.object({
      [worldUsers.enum.Bob]: z.object({
        username: z.string(),
        password: z.string(),
      }),
      [worldUsers.enum.Oscar]: z.object({
        username: z.string(),
        password: z.string(),
      }),
    }),
    headless: z.boolean(),
  }),
});

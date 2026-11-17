/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import dotenv from "dotenv";
import path from "node:path";
import { z } from "zod";
import { kind } from "./targets/kind";
import { targetSettingsSchema, type Target } from "./targets/target";

const TARGETS: Record<string, Target> = { kind };
const DEFAULT_TARGET = "kind";
const ciSchema = z.stringbool().default(false);

/**
 * Combines the profile that `E2E_TARGET` selects with the environment variables, and validates the result.
 * It throws an error that names the wrong value, so a run with an invalid configuration stops before its first test.
 */
export function loadEnvironment(
  environment: NodeJS.ProcessEnv,
  targets: Record<string, Target> = TARGETS,
) {
  const targetName = environment.E2E_TARGET ?? DEFAULT_TARGET;
  const target = targets[targetName];
  if (!target) {
    throw new Error(
      `Unknown E2E_TARGET "${targetName}". Known targets: ${Object.keys(targets).join(", ")}.`,
    );
  }

  const settings = targetSettingsSchema.safeParse(target(environment));
  if (!settings.success) {
    throw new Error(
      `The configuration of the target "${targetName}" is not valid:\n${z.prettifyError(settings.error)}`,
    );
  }

  const runsInCi = ciSchema.safeParse(environment.CI || undefined);
  if (!runsInCi.success) {
    throw new Error(
      `CI has the value "${environment.CI}", which is not a boolean such as "true" or "false":\n` +
        z.prettifyError(runsInCi.error),
    );
  }

  const { zacUrl, ...settingsOfTarget } = settings.data;
  return {
    target: targetName,
    runsInCi: runsInCi.data,
    ...settingsOfTarget,
    zacUrl: typeof zacUrl === "string" ? zacUrl : undefined,
    zacUrlUnknownBecause:
      typeof zacUrl === "string" ? undefined : zacUrl.unknownBecause,
  };
}

dotenv.config({ path: path.resolve(__dirname, "../.env"), quiet: true });

export const ENV = loadEnvironment(process.env);

export function requireZacUrl() {
  if (ENV.zacUrl === undefined) {
    throw new Error(
      `The target "${ENV.target}" cannot tell the URL of ZAC. ${ENV.zacUrlUnknownBecause}`,
    );
  }
  return ENV.zacUrl;
}

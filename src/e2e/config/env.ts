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

function loadEnvironment(environment: NodeJS.ProcessEnv) {
  const targetName = environment.E2E_TARGET ?? DEFAULT_TARGET;
  const target = TARGETS[targetName];
  if (!target) {
    throw new Error(
      `Unknown E2E_TARGET "${targetName}". Known targets: ${Object.keys(TARGETS).join(", ")}.`,
    );
  }

  const settings = targetSettingsSchema.safeParse(target(environment));
  if (!settings.success) {
    throw new Error(
      `The configuration of the target "${targetName}" is not valid:\n${z.prettifyError(settings.error)}`,
    );
  }

  const { zacUrl, accounts } = settings.data;
  return {
    target: targetName,
    runsInCi: Boolean(environment.CI),
    accounts,
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

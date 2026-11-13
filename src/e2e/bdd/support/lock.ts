/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { mkdir, rm } from "node:fs/promises";
import path from "node:path";
import { setTimeout as wait } from "node:timers/promises";

const ACQUIRE_TIMEOUT_MILLISECONDS = 60_000;
const RETRY_INTERVAL_MILLISECONDS = 100;

/**
 * Runs `action` while no other worker runs an action with the same name. Workers are separate processes,
 * so the lock is a directory: creating a directory fails when it already exists.
 */
export async function withLock<Result>(
  locksDirectory: string,
  name: string,
  action: () => Promise<Result>,
) {
  const lock = path.join(locksDirectory, name.replace(/[^\w-]/g, "_"));
  await mkdir(locksDirectory, { recursive: true });

  const deadline = Date.now() + ACQUIRE_TIMEOUT_MILLISECONDS;
  for (;;) {
    try {
      await mkdir(lock);
      break;
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code !== "EEXIST") throw error;
      if (Date.now() > deadline) {
        throw new Error(
          `Another worker held the lock "${name}" for more than ${ACQUIRE_TIMEOUT_MILLISECONDS / 1000} seconds. ` +
            `Delete ${lock} when that worker no longer runs.`,
          { cause: error },
        );
      }
      await wait(RETRY_INTERVAL_MILLISECONDS);
    }
  }

  try {
    return await action();
  } finally {
    await rm(lock, { recursive: true, force: true });
  }
}

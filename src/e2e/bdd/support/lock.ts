/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { randomUUID } from "node:crypto";
import { mkdir, readFile, rename, rm, writeFile } from "node:fs/promises";
import { hostname } from "node:os";
import path from "node:path";
import { setTimeout as wait } from "node:timers/promises";

const ACQUIRE_TIMEOUT_MILLISECONDS = 60_000;
const RETRY_INTERVAL_MILLISECONDS = 100;
const OWNER_FILE = "owner.json";

type Owner = {
  token: string;
  pid: number;
  host: string;
  acquiredAt: string;
};

function errorCode(error: unknown) {
  return (error as NodeJS.ErrnoException).code;
}

function isTakenError(error: unknown) {
  return ["EEXIST", "ENOTEMPTY"].includes(errorCode(error) ?? "");
}

async function ownerOf(lock: string) {
  try {
    return JSON.parse(
      await readFile(path.join(lock, OWNER_FILE), "utf8"),
    ) as Owner;
  } catch (error) {
    if (errorCode(error) === "ENOENT") return undefined;
    throw error;
  }
}

function isGone(owner: Owner) {
  if (owner.host !== hostname()) return false;
  try {
    process.kill(owner.pid, 0);
    return false;
  } catch (error) {
    return errorCode(error) === "ESRCH";
  }
}

/**
 * Takes the lock by renaming a directory that already holds the owner file. A rename onto a directory that is not
 * empty fails, so only one worker takes the lock, and a lock never exists without its owner.
 */
async function tryToAcquire(lock: string, owner: Owner) {
  const candidate = `${lock}.candidate-${owner.token}`;
  await mkdir(candidate);
  await writeFile(path.join(candidate, OWNER_FILE), JSON.stringify(owner));
  try {
    await rename(candidate, lock);
    return true;
  } catch (error) {
    await rm(candidate, { recursive: true, force: true });
    if (isTakenError(error)) return false;
    throw error;
  }
}

/**
 * Moves away the lock of an owner whose process is gone. The destination is named after that owner and is never
 * deleted during the run, so when two workers break the same lock, the rename of the second worker fails and
 * cannot move away the lock that the first worker took after it.
 */
async function breakLockOf(lock: string, staleOwner: Owner) {
  try {
    await rename(lock, `${lock}.stale-${staleOwner.token}`);
  } catch (error) {
    if (!isTakenError(error) && errorCode(error) !== "ENOENT") throw error;
  }
}

/**
 * Moves the lock away before it deletes it. Deleting it in place empties it first, and another worker can rename its
 * candidate onto an empty directory.
 */
async function release(lock: string, owner: Owner) {
  const released = `${lock}.released-${owner.token}`;
  await rename(lock, released);
  await rm(released, { recursive: true, force: true });
}

/**
 * Runs `action` while no other worker on this machine runs an action with the same name. Workers are separate
 * processes, so the lock is a directory. When the worker that holds a lock is killed, for example because a run in
 * the Playwright UI is stopped, the next worker breaks the lock.
 */
export async function withLock<Result>(
  locksDirectory: string,
  name: string,
  action: () => Promise<Result>,
) {
  const lock = path.join(locksDirectory, name.replace(/[^\w-]/g, "_"));
  await mkdir(locksDirectory, { recursive: true });
  const owner: Owner = {
    token: randomUUID(),
    pid: process.pid,
    host: hostname(),
    acquiredAt: new Date().toISOString(),
  };

  const deadline = Date.now() + ACQUIRE_TIMEOUT_MILLISECONDS;
  while (!(await tryToAcquire(lock, owner))) {
    const currentOwner = await ownerOf(lock);
    if (currentOwner && isGone(currentOwner)) {
      await breakLockOf(lock, currentOwner);
      continue;
    }
    if (Date.now() > deadline) {
      throw new Error(
        `Another worker held the lock "${name}" for more than ${ACQUIRE_TIMEOUT_MILLISECONDS / 1000} seconds: ` +
          `${JSON.stringify(currentOwner)}. Delete ${lock} when that worker no longer runs.`,
      );
    }
    await wait(RETRY_INTERVAL_MILLISECONDS);
  }

  try {
    return await action();
  } finally {
    await release(lock, owner);
  }
}

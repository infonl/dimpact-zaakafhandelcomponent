/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, test } from "@playwright/test";
import { spawnSync } from "node:child_process";
import { mkdir, mkdtemp, readdir, writeFile } from "node:fs/promises";
import { hostname, tmpdir } from "node:os";
import path from "node:path";
import { setTimeout as wait } from "node:timers/promises";
import { withLock } from "./lock";

async function locksDirectory() {
  return mkdtemp(path.join(tmpdir(), "e2e-locks-"));
}

function signal() {
  let give = () => {};
  const given = new Promise<void>((resolve) => {
    give = resolve;
  });
  return { give, given };
}

test("given two actions with the same name, when they run at the same time, then they never overlap", async () => {
  const directory = await locksDirectory();
  let running = 0;
  let mostAtOnce = 0;
  const action = async () => {
    running += 1;
    mostAtOnce = Math.max(mostAtOnce, running);
    await wait(50);
    running -= 1;
  };

  await Promise.all([
    withLock(directory, "zaaktype", action),
    withLock(directory, "zaaktype", action),
    withLock(directory, "zaaktype", action),
  ]);

  expect(mostAtOnce).toBe(1);
});

test("given two actions with different names, when they run at the same time, then each runs while the other runs", async () => {
  const directory = await locksDirectory();
  const aRuns = signal();
  const bRuns = signal();

  // When the actions ran one after the other, the first would wait for the second forever and the test would time out.
  const both = Promise.all([
    withLock(directory, "zaaktype A", async () => {
      aRuns.give();
      await bRuns.given;
      return "A";
    }),
    withLock(directory, "zaaktype B", async () => {
      bRuns.give();
      await aRuns.given;
      return "B";
    }),
  ]);

  await expect(both).resolves.toEqual(["A", "B"]);
});

test("given an action that fails, when it ends, then the lock is free for the next action", async () => {
  const directory = await locksDirectory();

  await expect(
    withLock(directory, "zaaktype", () => Promise.reject(new Error("boom"))),
  ).rejects.toThrow("boom");

  await expect(
    withLock(directory, "zaaktype", () => Promise.resolve("next")),
  ).resolves.toBe("next");
  expect(await readdir(directory)).toEqual([]);
});

test("given a lock of a worker whose process is gone, when another worker needs it, then it breaks the lock", async () => {
  const directory = await locksDirectory();
  const goneProcess = spawnSync(process.execPath, ["-e", ""]).pid;
  const lock = path.join(directory, "zaaktype");
  await mkdir(lock);
  await writeFile(
    path.join(lock, "owner.json"),
    JSON.stringify({
      token: "gone",
      pid: goneProcess,
      host: hostname(),
      acquiredAt: new Date().toISOString(),
    }),
  );

  await expect(
    withLock(directory, "zaaktype", () => Promise.resolve("taken")),
  ).resolves.toBe("taken");
});

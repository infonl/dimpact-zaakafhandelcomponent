/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
import { appendFileSync, mkdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { expect, type Page, test } from "@playwright/test";

const evidenceDir = __dirname;
const screenshotDir = join(evidenceDir, "screenshots");
const exploreDir = join(evidenceDir, ".explore");
const watchedPages = new WeakSet<Page>();
let stepNumber = 0;
let failedRequests: string[] = [];
const exploring = process.env.SPEC === "explore.spec.ts";

/** Seed users of the local kind stack: the password equals the username. */
export const USERS = {
  beheerder: "beheerder1",
  behandelaar: "behandelaar1",
  coordinator: "coordinator1",
  recordmanager: "recordmanager1",
  raadpleger: "raadpleger1",
} as const;

/** Records every /rest request that fails or gets a status >= 400, so a failed step names the request behind it. */
function watchRequests(page: Page) {
  if (watchedPages.has(page)) return;
  watchedPages.add(page);
  page.on("response", (response) => {
    const url = new URL(response.url());
    if (response.status() >= 400 && url.pathname.startsWith("/rest/")) {
      recordFailedRequest(`${response.status()} ${response.request().method()} ${url.pathname}`);
    }
  });
  page.on("requestfailed", (request) => {
    const url = new URL(request.url());
    if (url.pathname.startsWith("/rest/")) {
      recordFailedRequest(`${request.failure()?.errorText ?? "failed"} ${request.method()} ${url.pathname}`);
    }
  });
}

function recordFailedRequest(line: string) {
  failedRequests.push(line);
  if (!exploring) {
    appendFileSync(join(evidenceDir, "failed-requests.log"), `step ${stepNumber}: ${line}\n`);
  }
}

/**
 * Signs in through Keycloak. Clears the cookies first, so a second role in the same context gets a login form
 * instead of the session of the previous one. With ZAC_LOGIN_URL set (the frontend dev server on 4200 runs
 * without auth), it signs in on that ZAC first; the session cookie is shared across localhost ports.
 */
export async function signIn(page: Page, username: string = USERS.beheerder) {
  watchRequests(page);
  await page.context().clearCookies();
  await page.goto(process.env.ZAC_LOGIN_URL ?? "/");
  await page.getByRole("textbox", { name: /Username or email|Gebruikersnaam/ }).fill(username);
  await page.getByRole("textbox", { name: /Password|Wachtwoord/ }).fill(username);
  await page.getByRole("button", { name: /Sign In|Inloggen/ }).click();
  await page.waitForURL((url) => !url.pathname.includes("/realms/"), { timeout: 60_000 });
  await expect(page.getByRole("button", { name: "Gebruikers profiel" })).toBeVisible({ timeout: 60_000 });
  if (process.env.ZAC_LOGIN_URL) {
    await page.goto("/");
    await expect(page.getByRole("button", { name: "Gebruikers profiel" })).toBeVisible({ timeout: 60_000 });
  }
  await waitForZac(page);
}

/**
 * Waits until ZAC's requests are done, so screenshots never catch a half-rendered page.
 * ZAC keeps a progressbar in the page and polls in the background, so neither can be waited on without a limit.
 */
export async function waitForZac(page: Page) {
  await page.waitForLoadState("networkidle", { timeout: 10_000 }).catch(() => undefined);
  await page.waitForTimeout(500);
}

/**
 * Writes the accessibility tree of the page to .explore/<name>.yaml (the source for role and name selectors)
 * and the /rest requests that failed so far to .explore/<name>.failed-requests.
 */
export async function dumpPage(page: Page, name: string) {
  watchRequests(page);
  await waitForZac(page);
  mkdirSync(exploreDir, { recursive: true });
  writeFileSync(join(exploreDir, `${name}.yaml`), await page.locator("body").ariaSnapshot());
  writeFileSync(join(exploreDir, `${name}.failed-requests`), failedRequests.join("\n"));
}

/**
 * One checked step of the walkthrough: runs `action`, takes a full-page screenshot and records
 * pass or fail, with the /rest requests that failed during the step, in steps.jsonl.
 */
export async function proofStep(page: Page, title: string, action: () => Promise<void>) {
  watchRequests(page);
  stepNumber += 1;
  failedRequests = [];
  const slug = title.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "").slice(0, 60);
  const screenshot = join(screenshotDir, `${String(stepNumber).padStart(2, "0")}-${slug}.png`);
  mkdirSync(screenshotDir, { recursive: true });
  const record = (status: "pass" | "fail", error?: string) =>
    appendFileSync(
      join(evidenceDir, "steps.jsonl"),
      `${JSON.stringify({ step: stepNumber, title, status, screenshot, error, failedRequests })}\n`,
    );

  await test.step(title, async () => {
    try {
      await action();
      await waitForZac(page);
      await page.screenshot({ path: screenshot, fullPage: true });
      record("pass");
    } catch (error) {
      await page.screenshot({ path: screenshot, fullPage: true }).catch(() => undefined);
      const message = error instanceof Error ? error.message : String(error);
      record("fail", message.replace(/\u001b\[[0-9;]*m/g, "").split("\n")[0]);
      throw error;
    }
  });
}

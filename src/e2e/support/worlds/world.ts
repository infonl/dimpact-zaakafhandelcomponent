/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { World, setWorldConstructor } from "@cucumber/cucumber";
import { expect } from "@playwright/test";
import fs from "fs";
import playwright from "playwright";
import { z } from "zod";
import { TestStorageService } from "../../utils/TestStorage.service";
import { worldParametersScheme } from "../../utils/schemes";

export const authFile = "user.json";

const OPEN_URL_ATTEMPTS = 3;

export class CustomWorld extends World {
  page: playwright.Page;
  expect: typeof expect = expect;
  browser: playwright.Browser;
  context: playwright.BrowserContext;
  initialized: boolean = false;
  worldParameters: z.infer<typeof worldParametersScheme>["parameters"];
  testStorage: TestStorageService;
  /** Feature and scenario currently running, so created data can name its origin. */
  testName: string = "";

  constructor(attach: unknown) {
    const res = worldParametersScheme.parse(attach);
    super({
      link(text: string): void {},
      attach: res.attach,
      parameters: res.parameters,
      log: res.log,
    });
    this.worldParameters = res.parameters;
    this.testStorage = new TestStorageService();
  }

  async init({ videoFolder = "" }: { videoFolder?: string } = {}) {
    if (!fs.existsSync(authFile)) {
      fs.writeFileSync(authFile, "{}");
    }
    this.browser = await playwright.chromium.launch({
      channel: process.env.CI ? "msedge" : undefined,
      headless: this.worldParameters.headless,
      args: ["--lang=nl-NL"],
    });
    this.context = await this.browser.newContext({
      storageState: authFile,
      locale: "nl-NL",
      recordVideo: { dir: `reports/videos/${videoFolder}` },
    });
    this.page = await this.context.newPage();
    this.initialized = true;
  }

  async stop() {
    await this.context.close();
    await this.browser.close();
    this.initialized = false;
  }

  /**
   * Opens a URL. Chromium aborts a navigation (net::ERR_ABORTED) when the app starts one of its own
   * at the same moment, for example the redirect after completing a task. Opening a page is a GET, so
   * it is safe to repeat, and the repeated navigation then starts from the settled page.
   */
  async openUrl(url: string) {
    for (let attempt = 1; attempt <= OPEN_URL_ATTEMPTS; attempt++) {
      try {
        await this.page.goto(url);
        return;
      } catch (error) {
        const isAborted =
          error instanceof Error && error.message.includes("net::ERR_ABORTED");
        if (!isAborted || attempt === OPEN_URL_ATTEMPTS) {
          throw error;
        }
        await this.page.waitForLoadState("load");
      }
    }
  }
}
setWorldConstructor(CustomWorld);

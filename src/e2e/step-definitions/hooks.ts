/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { After, AfterAll, AfterStep, Before, Status } from "@cucumber/cucumber";
import fs from "fs";
import { ONE_MINUTE_IN_MS } from "../support/time-constants";
import { CustomWorld, authFile } from "../support/worlds/world";
import { testStorageFile } from "../utils/TestStorage.service";

Before(async function (this: CustomWorld, { gherkinDocument, pickle }) {
  const escape = (s?: string) => s && encodeURIComponent(s);
  const scenario = escape(pickle.name);
  const feature = escape(gherkinDocument.feature.name);
  const videoFolder = [feature, scenario].filter(Boolean).join("/");

  this.testName = [gherkinDocument.feature?.name, pickle.name]
    .filter(Boolean)
    .join(" - ");

  await this.context?.clearCookies();

  await this.init({ videoFolder });
});

After({ timeout: ONE_MINUTE_IN_MS }, async function (this: CustomWorld) {
  await this.context.storageState({ path: authFile });
  await this.stop();
});

AfterAll(async function (this: CustomWorld) {
  fs.unlinkSync(testStorageFile);
  console.log("Deleted test storage file successfully.");
  fs.unlinkSync(authFile);
  console.log("Deleted auth file successfully.");
});

AfterStep(async function (
  this: CustomWorld,
  { result, testStepId },
): Promise<void> {
  if (result.status !== Status.FAILED) return;

  // A popup such as the SmartDocuments wizard is a page of its own, and often the one that failed.
  for (const [index, page] of this.context.pages().entries()) {
    if (page.isClosed()) continue;

    const screenshot = await page.screenshot({
      path: `./reports/screenshots/${testStepId}-${index}.png`,
    });
    this.attach(screenshot, "image/png");
  }
});

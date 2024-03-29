/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
import { Given, Then, When } from "@cucumber/cucumber";
import { CustomWorld } from "../support/worlds/world";
import { worldPossibleZacUrls } from "../utils/schemes";

const ONE_MINUTE_IN_MS = 60_000;

When(
  "Employee {string} opens zac",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user) {
    const expectedUrl =
      this.worldParameters.urls[worldPossibleZacUrls.Values.zac];
    await this.openUrl(expectedUrl);
  },
);

When(
  "Employee {string} navigates to {string}",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user, path) {
    const expectedUrl =
      this.worldParameters.urls[worldPossibleZacUrls.Values.zac] + path;

    await this.openUrl(expectedUrl);
  },
);

Given(
  "{string} navigates to {string} with path {string}",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user, urlKey, path) {
    const res = worldPossibleZacUrls.parse(urlKey);
    const expectedUrl = this.worldParameters.urls[res] + path;

    await this.openUrl(expectedUrl);
  },
);

Given(
  "{string} navigates to {string} with path {string} with delay after of {int} ms",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user, urlKey, path, delay) {
    const res = worldPossibleZacUrls.parse(urlKey);
    const expectedUrl = this.worldParameters.urls[res] + path;

    await this.openUrl(expectedUrl);

    await this.page.waitForURL(expectedUrl);

    const currentUrl = this.page.url();

    if (currentUrl !== expectedUrl) {
      throw new Error(
        `Navigation failed: Expected URL '${expectedUrl}', but found '${currentUrl}'`,
      );
    }

    await this.page.waitForTimeout(delay);
  },
);

Then(
  "{string} sees the text: {string}",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, user, text) {
    await this.page.waitForSelector(`text=${text}`, {
      timeout: ONE_MINUTE_IN_MS,
    });
  },
);

Then(
  "{string} clicks on element with accessibility label: {string}",
  async function (this: CustomWorld, user, text) {
    await this.page.getByLabel(text).click();
  },
);

Then(
  "{string} clicks on element with id: {string}",
  async function (this: CustomWorld, user, text) {
    await this.page.click("#" + text);
  },
);

Then(
  "{string} clicks on element with text: {string}",
  async function (this: CustomWorld, user, text) {
    await this.page.getByText(text).click();
  },
);

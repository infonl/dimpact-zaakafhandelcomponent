/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
import { Given, Then, When } from "@cucumber/cucumber";
import { CustomWorld } from "../support/worlds/world";
import { worldPossibleZacUrls } from "../utils/schemes";

const ONE_MINUTE_IN_MS = 60_000;
const ONE_SECOND_IN_MS = 1_000;
const TEN_SECONDS_IN_MS = ONE_SECOND_IN_MS * 10;
const FIVE_MINUTES_IN_MS = ONE_MINUTE_IN_MS * 5;

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
  "the page is done searching",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld) {
    await this.page.waitForResponse(/zoeken\/list/);
    await this.page.waitForTimeout(ONE_SECOND_IN_MS);
  },
);

Given(
  "the page is done searching and reloaded",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld) {
    await this.page.waitForTimeout(TEN_SECONDS_IN_MS);
    await this.page.reload();
    await this.page.waitForLoadState("networkidle", {
      timeout: TEN_SECONDS_IN_MS,
    });
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

Then(
  "after a while the snackbar disappears",
  { timeout: FIVE_MINUTES_IN_MS },
  async function (this: CustomWorld) {
    await this.page
      .locator("mat-snack-bar-container")
      .waitFor({ state: "hidden", timeout: FIVE_MINUTES_IN_MS });
  },
);

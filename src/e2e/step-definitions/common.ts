/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
import { Given, When, Then } from "@cucumber/cucumber";
import { CustomWorld } from "../support/worlds/world";
import { worldPossibleZacUrls } from "../utils/schemes";

Given("{string} navigates to {string} with path {string}", { timeout: 60 * 1000 }, async function (this: CustomWorld, user, urlKey, path) {
    const res = worldPossibleZacUrls.parse(urlKey)
    await this.openUrl(this.worldParameters.urls[res] + path);
});

Then("{string} sees the text: {string}", async function (this: CustomWorld, user, text) {
    await this.page.waitForSelector(`text=${text}`);
});

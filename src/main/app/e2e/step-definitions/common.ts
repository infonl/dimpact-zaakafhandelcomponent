/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
import { Given, When, Then } from "@cucumber/cucumber";
import { CustomWorld } from "../support/worlds/world";
import { waitForHealthCheck } from "../support/utils/health";


Given("Zac is live", { timeout: 5 * (60 * 1000) }, async function (this: CustomWorld) {
    await waitForHealthCheck(this)
});

Given("{string} navigates to {string}", { timeout: 60 * 1000 }, async function (this: CustomWorld, user, url) {
    await this.openUrl(url);
});

Then("{string} sees the text: {string}", async function (this: CustomWorld, user, text) {
    await this.page.waitForSelector(`text=${text}`);
});

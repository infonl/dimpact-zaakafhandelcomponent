/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
import { Given, Then } from "@cucumber/cucumber";
import { CustomWorld } from "../support/worlds/world";
import { worldPossibleZacUrls } from "../utils/schemes";

Given("{string} navigates to {string} with path {string}", { timeout: 60 * 1000 }, async function (this: CustomWorld, user, urlKey, path) {
    const res = worldPossibleZacUrls.parse(urlKey)
    await this.openUrl(this.worldParameters.urls[res] + path);
});

Then("{string} sees the text: {string}", async function (this: CustomWorld, user, text) {
    await this.page.waitForSelector(`text=${text}`);
});

Then("{string} click on element with accessabillity label: {string}" , async function (this: CustomWorld, user, text) {
    await this.page.getByLabel(text).click()
})

Then("{string} click on element with id: {string} accessabillity label", async function (this: CustomWorld, user, text) {
    //  this.page.getAttribute('id', 'admin_button').click()
})
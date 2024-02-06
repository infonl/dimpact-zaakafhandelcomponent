/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
import { Given, Then, When } from "@cucumber/cucumber";
import { CustomWorld } from "../support/worlds/world";
import { worldPossibleZacUrls, worldUsers } from "../utils/schemes";
import { login } from "./authentication";

When("Employee {string} opens zac", { timeout: 60 * 1000 }, async function (this: CustomWorld, user) {
    const expectedUrl = this.worldParameters.urls[worldPossibleZacUrls.Values.zac];
    const isLoginScreen = await this.page.getByLabel('Sign in to your account')
    await this.page.waitForTimeout(2000)

    if(await isLoginScreen.isVisible()) {
        const parsedUser = worldUsers.parse(user)
        const {username, password} = this.worldParameters.users[parsedUser]

        await login(this, username, password)
        await this.page.waitForTimeout(500)
    }
 
    await this.openUrl(expectedUrl);
})

When("Employee {string} navigates to {string}", { timeout: 60 * 1000 }, async function (this: CustomWorld, user, path) {
    const expectedUrl = this.worldParameters.urls[worldPossibleZacUrls.Values.zac] + path;
 
    await this.openUrl(expectedUrl);
 });

Given("{string} navigates to {string} with path {string}", { timeout: 60 * 1000 }, async function (this: CustomWorld, user, urlKey, path) {
   const res = worldPossibleZacUrls.parse(urlKey);
   const expectedUrl = this.worldParameters.urls[res] + path;

   await this.openUrl(expectedUrl);
});

Given("{string} navigates to {string} with path {string} with delay after of {int} ms", { timeout: 60 * 1000 }, async function (this: CustomWorld, user, urlKey, path, delay) {
    const res = worldPossibleZacUrls.parse(urlKey);
    const expectedUrl = this.worldParameters.urls[res] + path;
 
    await this.openUrl(expectedUrl);
 
    await this.page.waitForURL(expectedUrl);
 
    const currentUrl = this.page.url();
 
    if (currentUrl !== expectedUrl) {
        throw new Error(`Navigation failed: Expected URL '${expectedUrl}', but found '${currentUrl}'`);
    }

    await this.page.waitForTimeout(delay)
 });
 

Then("{string} sees the text: {string}", async function (this: CustomWorld, user, text) {
    await this.page.waitForSelector(`text=${text}`, {'timeout': 10000 });
});

Then("{string} clicks on element with accessabillity label: {string}" , async function (this: CustomWorld, user, text) {
    await this.page.getByLabel(text).click()
})

Then("{string} clicks on element with id: {string}", async function (this: CustomWorld, user, text) {
    await this.page.click('#' + text)
})

Then("{string} clicks on element with text: {string}", async function (this: CustomWorld, user, text) {
    await this.page.getByText(text).click()
})
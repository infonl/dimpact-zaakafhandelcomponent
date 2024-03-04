/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { When } from "@cucumber/cucumber";
import { CustomWorld, authFile } from "../support/worlds/world";
import { worldUsers } from "../utils/schemes";

const ONE_MINUTE_IN_MS = 60 * 1000;

export async function login(world: CustomWorld, username: string, password: string) {
    await world.page.getByLabel("Username or email").click();
    await world.page.getByLabel("Username or email").fill(username);
    await world.page.getByText("Password").click();
    await world.page.getByText("Password").fill(password);
    await world.page.getByRole("button", { name: "Sign In" }).click();
}

async function loginToZac(this: CustomWorld, user: string) {
    const parsedUser = worldUsers.parse(user)
    const {username, password} = this.worldParameters.users[parsedUser]

    await login(this, username, password);
}

When("Employee {string} logs in to zac", { timeout: ONE_MINUTE_IN_MS }, async function (this: CustomWorld, user: string) {
    await loginToZac.call(this, user);
});

When("Employee {string} logs out of zac", async function (this: CustomWorld, user: string) {
    await this.page.getByText("account_circle").first().click();
    await this.page.getByText("Uitloggen").first().click();
});

// @deprecated 
When("{string} logs in", { timeout: ONE_MINUTE_IN_MS }, async function (this: CustomWorld, user: string) {
    await loginToZac.call(this, user);
});

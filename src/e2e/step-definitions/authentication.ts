/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { When } from "@cucumber/cucumber";
import { CustomWorld } from "../support/worlds/world";
import { worldUsers } from "../utils/schemes";

When("Employee {string} logs in to zac", async function (this: CustomWorld, user) {
    const parsedUser = worldUsers.parse(user)
    const {username, password} = this.worldParameters.users[parsedUser]

    await this.page.getByLabel("Username or email").click();
    await this.page.getByLabel("Username or email").fill(username);
    await this.page.getByText("Password").click();
    await this.page.getByText("Password").fill(password);
    await this.page.getByRole("button", { name: "Sign In" }).click();
});

When("Employee {string} logs out of zac", async function (this: CustomWorld, user) {
    const parsedUser = worldUsers.parse(user)

    await this.page.getByText("account_circle").first().click();
    await this.page.getByText("Uitloggen").first().click();
    await this.removeContext();
});

// @deprecated 
When("{string} logs in", async function (this: CustomWorld, user) {
    const parsedUser = worldUsers.parse(user)
    const {username, password} = this.worldParameters.users[parsedUser]

    await this.page.getByLabel("Username or email").click();
    await this.page.getByLabel("Username or email").fill(username);
    await this.page.getByText("Password").click();
    await this.page.getByText("Password").fill(password);
    await this.page.getByRole("button", { name: "Sign In" }).click();
});

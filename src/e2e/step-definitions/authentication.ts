/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { When } from "@cucumber/cucumber";
import { CustomWorld } from "../support/worlds/world";
import { worldUsers } from "../utils/schemes";


When("{string} logs in", async function (this: CustomWorld, user) {
    const parsedUser = worldUsers.parse(user)
    const {username, password} = this.worldParameters.users[parsedUser]

    await this.page.getByLabel("Username or email").click();
    await this.page.getByLabel("Username or email").fill(username);
    await this.page.getByLabel("Password").click();
    await this.page.getByLabel("Password").fill(password);
    await this.page.getByRole("button", { name: "Sign In" }).click();
});

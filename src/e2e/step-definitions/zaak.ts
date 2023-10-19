import { When } from "@cucumber/cucumber";
import { CustomWorld } from "../support/worlds/world";

/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
When("{string} wants to create a new zaak", { timeout: 60 * 1000 }, async function (this: CustomWorld, user) {
    await this.page.getByLabel("Zaak toevoegen").click();
    await this.page.getByLabel("Casetype").click();
    await this.page.getByRole("option", { name: "Bezwaar behandelen" }).click();
    await this.page
        .locator("div")
        .filter({ hasText: /^person$/ })
        .click();
    await this.page.getByLabel("CSN").dblclick({
        button: "right",
    });
    await this.page.getByLabel("CSN").fill("999993896");
    await this.page.getByLabel("emoji_people Citizen").getByRole("button", { name: "Search" }).click();
    await this.page.getByRole("button", { name: "Select" }).click();
    await this.page
        .locator("div")
        .filter({ hasText: /^gps_fixed$/ })
        .click();
    await this.page.getByPlaceholder("Search by address, zip code or place of residence").click();
    await this.page.getByPlaceholder("Search by address, zip code or place of residence").fill("1112gv");
    await this.page.getByPlaceholder("Search by address, zip code or place of residence").press("Enter");
    await this.page
        .getByRole("row", { name: "Show related data 0384200000005901 Address Meelbeskamp 49, 1112GV Diemen Select" })
        .getByTitle("Select")
        .click();
    await this.page.locator("button").filter({ hasText: "close" }).click();
    await this.page.getByText("Communication channel- Select a communication channel -").click();
    await this.page.getByText("E-mail").click();
    await this.page.getByText("Confidentiality notice Case classified").click();
    await this.page.getByText("Public", { exact: true }).click();
    await this.page.getByLabel("Description").click();
    await this.page.getByLabel("Description").fill("E2etes1");
    await this.page.getByRole("button", { name: "Create" }).click();
});

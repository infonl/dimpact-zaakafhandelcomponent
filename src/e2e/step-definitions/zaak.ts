/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Then, When } from "@cucumber/cucumber";
import { CustomWorld } from "../support/worlds/world";

When("{string} wants to create a new zaak", { timeout: 60 * 1000 }, async function (this: CustomWorld, user) {
    await this.page.getByLabel("Zaak toevoegen").click();
    await this.page.getByLabel("Zaaktype").click();
    await this.page.getByRole("option", { name: "Melding evenement organiseren behandelen" }).click();
    await this.page
        .locator("div")
        .filter({ hasText: /^person$/ })
        .click();
    await this.page.getByLabel("BSN").click();
    await this.page.getByLabel("BSN").fill("999993896");
    await this.page.getByLabel("emoji_people Persoon").getByRole("button", { name: "Zoeken" }).click();
    await this.page.getByRole("button", { name: "Select" }).click();
    await this.page
        .locator("div")
        .filter({ hasText: /^gps_fixed$/ })
        .click();
    await this.page.getByPlaceholder("Zoeken op adres, postcode of woonplaats").click();
    await this.page.getByPlaceholder("Zoeken op adres, postcode of woonplaats").fill("1112gv");
    await this.page.getByPlaceholder("Zoeken op adres, postcode of woonplaats").press("Enter");
    await this.page
        .getByRole("row", { name: "Gerelateerde gegevens tonen 0384200000005901 Adres Meelbeskamp 49, 1112GV Diemen Selecteren" })
        .getByTitle('Selecteren')
        .click();
    await this.page.getByText('close').click();
    await this.page.getByLabel("Communicatiekanaal").click();
    await this.page.getByRole("option", { name: ' E-mail ' }).click();
    await this.page.waitForTimeout(1000)
    // Openbaar should be automatically selected on openbaar
    await this.page.getByText("Vertrouwelijkheidaanduiding- Openbaar -")
    await this.page.getByLabel("Omschrijving").click();
    await this.page.getByLabel("Omschrijving").fill("E2etest1");
    await this.page.getByRole("button", { name: "Aanmaken" }).click();

    await this.page.waitForTimeout(5000)

    const currentYear = new Date().getFullYear();

// Construct the regex pattern with the current year
    const regexPattern = new RegExp(`ZAAK-${currentYear}-\\d+`, 'g');

    await this.page.getByText(regexPattern)

        // Get the HTML content of the page
    const content = await this.page.content();

    // Find all matches
    const matches = content.match(regexPattern);

    if (matches && matches.length > 0) {
        console.log('saved case number: ', matches[0])
        this.testStorage.set('caseNumber', matches[0]);
    } else {
        throw new Error("No case number found");
    }

    await this.page.waitForTimeout(1000)
});

Then("{string} sees the created zaak", { timeout: 60 * 1000 }, async function (this: CustomWorld, user,  ) {
    const caseNumber = this.testStorage.get('caseNumber');

    await this.page.getByText(caseNumber);
});


Then("{string} sees the created zaak with a delay", { timeout: 60 * 1000 + 15000 }, async function (this: CustomWorld, user,  ) {
    // atleast a minute and 10 seconds just to be sure
    await this.page.waitForTimeout(60 * 1000 + 10000)
    const caseNumber = this.testStorage.get('caseNumber');

    await this.page.getByText(caseNumber);
});
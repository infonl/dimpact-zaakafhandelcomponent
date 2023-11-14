/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { When } from "@cucumber/cucumber";
import { CustomWorld } from "../support/worlds/world";

When("{string} wants to create a new zaak", { timeout: 60 * 1000 }, async function (this: CustomWorld, user) {
    await this.page.getByLabel("Zaak toevoegen").click();
    await this.page.getByLabel("Zaaktype").click();
    await this.page.getByRole("option", { name: "Melding evenement organiseren behandelen" }).click();
    await this.page
        .locator("div")
        .filter({ hasText: /^person$/ })
        .click();
    await this.page.getByLabel("BSN").dblclick({
        button: "right",
    });
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
    await this.page.getByText("Communicatiekanaal- Kies een communicatiekanaal -").click();
    await this.page.getByRole('option').getByText("E-mail").click();
    // Openbaar should be automatically selected on openbaar
    await this.page.getByText("Vertrouwelijkheidaanduiding- Openbaar -")
    await this.page.getByLabel("Omschrijving").click();
    await this.page.getByLabel("Omschrijving").fill("E2etest1");
    await this.page.getByRole("button", { name: "Aanmaken" }).click();
});

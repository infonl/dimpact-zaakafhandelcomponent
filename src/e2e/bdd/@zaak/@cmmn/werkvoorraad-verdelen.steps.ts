/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect } from "@playwright/test";
import { Given, Then, When } from "../../fixtures";
import {
  mijnZaken,
  takenWerkvoorraad,
  zakenWerkvoorraad,
} from "../../screens/werkvoorraad";
import { zaakPage } from "../../screens/zaak";
import { SEED } from "../seed";

const OMSCHRIJVING = "zaak zonder behandelaar";
const TAAK = "Goedkeuren";

Given(
  "{int} zaken for groep {string} without a behandelaar",
  async ({ testData }, aantal: number, groep: string) => {
    for (let zaak = 0; zaak < aantal; zaak++) {
      await testData.createZaak({
        zaaktype: SEED.cmmnZaaktype,
        groep,
        omschrijving: OMSCHRIJVING,
      });
    }
  },
);

Given(
  "a zaak for groep {string} without a behandelaar",
  async ({ testData }, groep: string) => {
    await testData.createZaak({
      zaaktype: SEED.cmmnZaaktype,
      groep,
      omschrijving: OMSCHRIJVING,
    });
  },
);

Given(
  "{int} open taken for groep {string} without a behandelaar",
  async ({ testData }, aantal: number, groep: string) => {
    const zaak = await testData.createZaak({
      zaaktype: SEED.cmmnZaaktype,
      groep,
      omschrijving: "zaak met open taken",
    });
    await testData.finishIntake(zaak);
    for (let taak = 0; taak < aantal; taak++) {
      await testData.startTaak(zaak, {
        naam: TAAK,
        groep,
        taakdata: { vraag: testData.unique("Mag dit door?") },
      });
    }
  },
);

When("I open the zaken werkvoorraad", async ({ page }) => {
  await zakenWerkvoorraad(page).open();
});

When("I open the taken werkvoorraad", async ({ page }) => {
  await takenWerkvoorraad(page).open();
});

When("I select the {int} zaken", async ({ page, testData }, aantal: number) => {
  const werkvoorraad = zakenWerkvoorraad(page);
  for (const { identificatie } of testData.zaken) {
    // From the moment its dialog opens, the werkvoorraad takes any notification about a rol of a zaak as the end of
    // its assignment, also a late one about the rollen of a new zaak.
    await testData.untilZaakHasStatus(identificatie, "Intake");
  }
  // The zaken of the scenario share the unique omschrijving, so the filter shows them and no others.
  await werkvoorraad.filterOnOmschrijving(testData.unique(OMSCHRIJVING));
  for (const { identificatie } of testData.zaken) {
    await expect(werkvoorraad.zaak(identificatie)).toBeVisible();
  }
  expect(testData.zaken).toHaveLength(aantal);
  await werkvoorraad.selectAll();
});

When("I select the {int} taken", async ({ page, testData }, aantal: number) => {
  const { identificatie } = testData.zaak;
  await testData.untilTaakIsIndexed(identificatie, TAAK, aantal);
  const werkvoorraad = takenWerkvoorraad(page);
  await werkvoorraad.filterOnZaak(identificatie);
  await expect(werkvoorraad.taken()).toHaveCount(aantal);
  await werkvoorraad.selectAll();
});

When(
  "I assign them to groep {string} and to the behandelaar",
  async ({ page, accountOfRole }, groep: string) => {
    await zakenWerkvoorraad(page).verdelen({
      groep,
      medewerker: accountOfRole("behandelaar").displayName,
    });
  },
);

When(
  "I assign them to the behandelaar",
  async ({ page, accountOfRole, testData }) => {
    const { naam } = await testData.groepOf("behandelaar");
    await takenWerkvoorraad(page).verdelen({
      groep: naam,
      medewerker: accountOfRole("behandelaar").displayName,
    });
  },
);

When(
  "I release them with the reason {string}",
  async ({ page }, reden: string) => {
    await zakenWerkvoorraad(page).vrijgeven(reden);
  },
);

Then("I see the message {string}", async ({ page }, message: string) => {
  await expect(page.getByText(message)).toBeVisible();
});

Then(
  "the {int} zaken show the behandelaar without a page reload",
  async ({ page, testData, accountOfRole }, aantal: number) => {
    expect(testData.zaken).toHaveLength(aantal);
    const werkvoorraad = zakenWerkvoorraad(page);
    await werkvoorraad.untilProcessed();
    for (const { identificatie } of testData.zaken) {
      await expect(werkvoorraad.zaak(identificatie)).toContainText(
        accountOfRole("behandelaar").displayName,
      );
    }
  },
);

Then(
  "the {int} zaken show no behandelaar without a page reload",
  async ({ page, testData, accountOfRole }, aantal: number) => {
    expect(testData.zaken).toHaveLength(aantal);
    const werkvoorraad = zakenWerkvoorraad(page);
    await werkvoorraad.untilProcessed();
    for (const { identificatie } of testData.zaken) {
      await expect(werkvoorraad.zaak(identificatie)).not.toContainText(
        accountOfRole("behandelaar").displayName,
      );
    }
  },
);

Then(
  "the {int} taken show the behandelaar without a page reload",
  async ({ page, accountOfRole }, aantal: number) => {
    const werkvoorraad = takenWerkvoorraad(page);
    await werkvoorraad.untilProcessed();
    await expect(
      werkvoorraad
        .taken()
        .filter({ hasText: accountOfRole("behandelaar").displayName }),
    ).toHaveCount(aantal);
  },
);

When(
  "I assign the zaak to myself",
  async ({ page, testData, accountOfRole }) => {
    const { identificatie } = testData.zaak;
    await testData.untilZaakIsIndexed(identificatie);
    const werkvoorraad = zakenWerkvoorraad(page);
    await werkvoorraad.filterOnZaaknummer(identificatie);
    await werkvoorraad.assignToMe(identificatie);
    // The row shows the behandelaar only after ZAC has stored the assignment, which a next page load could abort.
    await expect(werkvoorraad.zaak(identificatie)).toContainText(
      accountOfRole("behandelaar").displayName,
    );
  },
);

Then(
  "the zaak shows me as behandelaar",
  async ({ page, testData, accountOfRole }) => {
    const zaak = zaakPage(page);
    await zaak.open(testData.zaak.identificatie);
    await zaak.expectGegeven(
      "Behandelaar",
      accountOfRole("behandelaar").displayName,
    );
  },
);

Then("the zaak appears in my zaken", async ({ page, testData }) => {
  const { identificatie } = testData.zaak;
  await testData.untilZaakIsIndexed(identificatie, "van de behandelaar");
  const lijst = mijnZaken(page);
  await lijst.open();
  await lijst.expectZaak(identificatie);
});

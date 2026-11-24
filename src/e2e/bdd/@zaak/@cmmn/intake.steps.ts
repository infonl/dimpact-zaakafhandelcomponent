/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect } from "@playwright/test";
import { Then, When } from "../../fixtures";
import { confirmDialog, findDialog } from "../../screens/dialog";
import { zaakPage, type HistorieRegel } from "../../screens/zaak";
import { seededPersoonOrSkip } from "../seed";

const AANVULLENDE_INFORMATIE = "Aanvullende informatie";

function dateOf(dutchDate: string) {
  const [day, month, year] = dutchDate.split("-").map(Number);
  return Date.UTC(year, month - 1, day);
}

function daysMoved({ oudeWaarde, nieuweWaarde }: HistorieRegel) {
  return (dateOf(nieuweWaarde) - dateOf(oudeWaarde)) / 86_400_000;
}

// The historie cuts long values short, so these match the start of a value.
const EVENTS_IN_HISTORIE: Record<string, (regel: HistorieRegel) => boolean> = {
  request: ({ gegeven, nieuweWaarde }) =>
    gegeven === "Status" && nieuweWaarde.startsWith("Wacht op aanvulle"),
  suspension: ({ gegeven, reden }) =>
    gegeven === "Fatale datum" && reden.startsWith("Opschorting:"),
  resumption: ({ gegeven, reden }) =>
    gegeven === "Fatale datum" && reden.startsWith("Hervatting:"),
  intake: ({ gegeven, nieuweWaarde }) =>
    gegeven === "Status" && nieuweWaarde === "In behandeling",
  extension: ({ gegeven, reden }) =>
    gegeven === "Fatale datum" && reden.startsWith("Verlenging:"),
};

When(
  "I request aanvullende informatie from the initiator",
  async ({ page, testData, $testInfo }) => {
    const zaak = zaakPage(page);
    await zaak.chooseFromMenu(AANVULLENDE_INFORMATIE);
    await page
      .getByRole("combobox", { name: "Taak toekennen aan groep" })
      .click();
    const { naam } = await testData.groepOf("behandelaar");
    await page.getByRole("option", { name: naam }).click();
    await expect(
      page.getByRole("textbox", { name: "E-mailadres" }),
    ).toHaveValue(seededPersoonOrSkip($testInfo).emailadres);
    // Starting the task sends the mail, stores it as a document and changes the status in Open Zaak, which can take
    // longer than an expect waits, so the step waits for the answer of ZAC first.
    await Promise.all([
      page.waitForResponse(
        (response) =>
          new URL(response.url()).pathname.endsWith(
            "/rest/planitems/doHumanTaskPlanItem",
          ) && response.ok(),
      ),
      page.getByRole("button", { name: "Starten" }).click(),
    ]);
    await expect(zaak.taak(AANVULLENDE_INFORMATIE)).toBeVisible();
  },
);

When(
  "I suspend the zaak for {int} days with the reason {string}",
  async ({ page }, dagen: number, reden: string) => {
    await zaakPage(page).chooseFromMenu("Zaak opschorten");
    const dialog = findDialog(page, "Zaak opschorten");
    await dialog
      .getByRole("spinbutton", { name: "Opschortduur (kalenderdagen)" })
      .fill(String(dagen));
    await dialog.getByRole("textbox", { name: "Reden" }).fill(reden);
    await confirmDialog(dialog, "Zaak opschorten");
  },
);

Then(
  "the zaak shows the indicatie {string}",
  async ({ page }, indicatie: string) => {
    await expect(zaakPage(page).indicatie(indicatie)).toBeVisible();
  },
);

Then(
  "the zaak no longer shows the indicatie {string}",
  async ({ page }, indicatie: string) => {
    await expect(zaakPage(page).indicatie(indicatie)).toBeHidden();
  },
);

Then(
  "the uiterste einddatum moves {int} days later",
  async ({ page }, dagen: number) => {
    const zaak = zaakPage(page);
    await expect(async () => {
      const wijzigingen = (await zaak.historie()).filter(
        ({ gegeven }) => gegeven === "Fatale datum",
      );
      expect(wijzigingen.map(daysMoved)).toContain(dagen);
    }).toPass();
  },
);

When(
  "I complete the aanvullende informatie task",
  async ({ page, accountOfRole, testData }) => {
    const zaak = zaakPage(page);
    const taak = zaak.taak(AANVULLENDE_INFORMATIE);
    await taak.getByRole("button", { name: "Ken aan mij toe" }).click();
    await expect(taak).toContainText(accountOfRole("behandelaar").displayName);
    await taak.getByRole("link", { name: "Taak bekijken" }).click();
    await page.getByRole("radio", { name: "Wel geleverd en akkoord" }).check();
    // The next step resumes the zaak, with a reason.
    await page
      .getByRole("radio", { name: "Nee, de opschorting behouden" })
      .check();
    await page.getByRole("button", { name: "Opslaan & afronden" }).click();
    await zaak.expectOpened(testData.zaak.identificatie);
    await expect(taak).toBeHidden();
  },
);

When(
  "I resume the zaak with the reason {string}",
  async ({ page }, reden: string) => {
    await zaakPage(page).chooseFromMenu("Zaak hervatten");
    const dialog = findDialog(page, "Zaak hervatten");
    await dialog.getByRole("textbox", { name: "Reden" }).fill(reden);
    await confirmDialog(dialog, "Zaak hervatten");
  },
);

When("I finish the intake as ontvankelijk", async ({ page }) => {
  await zaakPage(page).chooseFromMenu("Intake afronden");
  const dialog = findDialog(page, "Intake afronden");
  await dialog
    .getByRole("radiogroup", { name: "Is deze zaak ontvankelijk?" })
    .getByRole("radio", { name: "Ja" })
    .check();
  await confirmDialog(dialog, "Intake afronden");
});

When(
  "I extend the zaak by {int} days with the reason {string}",
  async ({ page }, dagen: number, reden: string) => {
    await zaakPage(page).chooseFromMenu("Zaak verlengen");
    const dialog = findDialog(page, "Zaak verlengen");
    await dialog
      .getByRole("spinbutton", { name: "Verlengingsduur (kalenderdagen)" })
      .fill(String(dagen));
    await dialog.getByRole("textbox", { name: "Reden" }).fill(reden);
    await confirmDialog(dialog, "Zaak verlengen");
  },
);

Then(
  "the historie lists the request, the suspension, the resumption, the intake and the extension",
  async ({ page }) => {
    const zaak = zaakPage(page);
    await expect(async () => {
      const historie = await zaak.historie();
      const positions = Object.entries(EVENTS_IN_HISTORIE).map(
        ([event, matches]) => ({
          event,
          position: historie.findIndex(matches),
        }),
      );
      expect(positions.filter(({ position }) => position < 0)).toEqual([]);
      // The historie lists the newest row first.
      expect(positions.map(({ event }) => event)).toEqual(
        positions
          .toSorted((first, second) => second.position - first.position)
          .map(({ event }) => event),
      );
    }).toPass();
  },
);

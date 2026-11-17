/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect } from "@playwright/test";
import { Then, When } from "../../fixtures";
import { zaakPage, type HistorieRegel } from "../../screens/zaak";
import { SEED } from "../seed";

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
  async ({ page }) => {
    const zaak = zaakPage(page);
    await zaak.chooseFromMenu(AANVULLENDE_INFORMATIE);
    await page
      .getByRole("combobox", { name: "Taak toekennen aan groep" })
      .click();
    await page.getByRole("option", { name: SEED.groep }).click();
    await page.getByRole("button", { name: "Starten" }).click();
    await expect(zaak.taak(AANVULLENDE_INFORMATIE)).toBeVisible();
  },
);

When(
  "I suspend the zaak for {int} days with the reason {string}",
  async ({ page }, dagen: number, reden: string) => {
    await zaakPage(page).chooseFromMenu("Zaak opschorten");
    const dialog = page.getByRole("dialog", { name: "Zaak opschorten" });
    await dialog
      .getByRole("spinbutton", { name: "Opschortduur (kalenderdagen)" })
      .fill(String(dagen));
    await dialog.getByRole("textbox", { name: "Reden" }).fill(reden);
    await dialog.getByRole("button", { name: "Zaak opschorten" }).click();
    await expect(dialog).toBeHidden();
  },
);

Then("the status is {string}", async ({ page }, status: string) => {
  await zaakPage(page).expectGegeven("Status", status);
});

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
  async ({ page, accountOfRole }) => {
    const taak = zaakPage(page).taak(AANVULLENDE_INFORMATIE);
    await taak.getByRole("button", { name: "Ken aan mij toe" }).click();
    await expect(taak).toContainText(accountOfRole("behandelaar").displayName);
    await taak.getByRole("link", { name: "Taak bekijken" }).click();
    await page.getByRole("radio", { name: "Wel geleverd en akkoord" }).check();
    // The next step resumes the zaak, with a reason.
    await page
      .getByRole("radio", { name: "Nee, de opschorting behouden" })
      .check();
    await page.getByRole("button", { name: "Opslaan & afronden" }).click();
    await expect(zaakPage(page).taak(AANVULLENDE_INFORMATIE)).toBeHidden();
  },
);

When(
  "I resume the zaak with the reason {string}",
  async ({ page }, reden: string) => {
    await zaakPage(page).chooseFromMenu("Zaak hervatten");
    // This dialog has no accessible name, so it is found by its heading.
    const dialog = page
      .getByRole("dialog")
      .filter({ has: page.getByRole("heading", { name: "Zaak hervatten" }) });
    await dialog.getByRole("textbox", { name: "Reden" }).fill(reden);
    await dialog.getByRole("button", { name: "Zaak hervatten" }).click();
    await expect(dialog).toBeHidden();
  },
);

When("I finish the intake as ontvankelijk", async ({ page }) => {
  await zaakPage(page).chooseFromMenu("Intake afronden");
  const dialog = page.getByRole("dialog", { name: /^Intake afronden/ });
  await dialog
    .getByRole("radiogroup", { name: "Is deze zaak ontvankelijk?" })
    .getByRole("radio", { name: "Ja" })
    .check();
  await dialog.getByRole("button", { name: "Intake afronden" }).click();
  await expect(dialog).toBeHidden();
});

When(
  "I extend the zaak by {int} days with the reason {string}",
  async ({ page }, dagen: number, reden: string) => {
    await zaakPage(page).chooseFromMenu("Zaak verlengen");
    const dialog = page.getByRole("dialog", { name: "Zaak verlengen" });
    await dialog
      .getByRole("spinbutton", { name: "Verlengingsduur (kalenderdagen)" })
      .fill(String(dagen));
    await dialog.getByRole("textbox", { name: "Reden" }).fill(reden);
    await dialog.getByRole("button", { name: "Zaak verlengen" }).click();
    await expect(dialog).toBeHidden();
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

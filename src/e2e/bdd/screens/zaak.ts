/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, type Page } from "@playwright/test";

/** A row of the historie of a zaak. ZAC cuts long values short and ends them with "...". */
export type HistorieRegel = {
  gegeven: string;
  actie: string;
  oudeWaarde: string;
  nieuweWaarde: string;
  reden: string;
};

/** The page of one zaak. */
export function zaakPage(page: Page) {
  async function tab(name: string) {
    await page.getByRole("tab", { name }).click();
    return page.getByRole("tabpanel", { name });
  }

  return {
    async open(identificatie: string) {
      await page.goto(`/zaken/${identificatie}`);
      await this.expectOpened(identificatie);
    },

    async expectOpened(identificatie: string) {
      await expect(
        page.getByRole("heading", { name: identificatie, level: 1 }),
      ).toBeVisible();
    },

    /** Expects the field of the Gegevens tab, such as "Status" or "Groep", to have the value. */
    async expectGegeven(field: string, value: string) {
      await expect(await tab("Gegevens")).toContainText(
        new RegExp(`${field}\\s*${value}`),
      );
    },

    async expectInitiator(naam: string) {
      await expect(
        page.getByRole("button", { name: `Initiator ${naam}` }),
      ).toBeVisible();
    },

    async expectBagObject(adres: string) {
      await expect(await tab("BAG objecten")).toContainText(adres);
    },

    /** Clicks an item of the menu of the zaak, such as an action or a task to start. */
    async chooseFromMenu(item: string) {
      await page
        .getByRole("navigation")
        .getByRole("button", { name: item })
        .click();
    },

    indicatie(indicatie: string) {
      return page.getByRole("option", { name: indicatie });
    },

    /** The task in the list of tasks of the zaak. */
    taak(naam: string) {
      return page.getByRole("row", { name: new RegExp(`^${naam} `) });
    },

    /** The rows of the historie, the newest first. */
    async historie(): Promise<HistorieRegel[]> {
      const rows = (await tab("Historie")).getByRole("row");
      await expect(rows.getByRole("cell")).not.toHaveCount(0);
      const regels: HistorieRegel[] = [];
      for (const row of await rows.all()) {
        const cells = await row.getByRole("cell").allInnerTexts();
        if (cells.length < 7) continue;
        const [, , gegeven, actie, oudeWaarde, nieuweWaarde, reden] = cells;
        regels.push({ gegeven, actie, oudeWaarde, nieuweWaarde, reden });
      }
      return regels;
    },

    async expectNoOntvangstbevestiging() {
      await expect(
        page.getByRole("option", { name: "Geen bevestiging verstuurd" }),
      ).toBeVisible();
    },
  };
}

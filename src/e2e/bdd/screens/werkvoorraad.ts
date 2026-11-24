/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, type Locator, type Page } from "@playwright/test";
import type { Schemas } from "../support/zac-api";
import { confirmDialog, findDialog } from "./dialog";

/** Waits for the answer of a search. With a text, waits for the answer of the search for that text, not of another. */
function answerOfSearch(page: Page, text?: string) {
  return page.waitForResponse((response) => {
    if (!new URL(response.url()).pathname.endsWith("/rest/zoeken/list")) {
      return false;
    }
    if (!response.ok()) return false;
    if (text === undefined) return true;
    const { zoeken } = response
      .request()
      .postDataJSON() as Schemas["RestZoekParameters"];
    return Object.values(zoeken ?? {}).includes(text);
  });
}

/** Opens the list and waits for the answer of its first search, so a filter shows the text it already has. */
async function openList(page: Page, path: string) {
  await Promise.all([answerOfSearch(page), page.goto(path)]);
}

/** Fills the filter of a column, and waits for the answer of the search, so a missing row is really missing. */
async function filter(page: Page, column: string, text: string) {
  const textbox = page.getByRole("textbox", { name: column });
  // ZAC does not search again for a filter that did not change, so no answer would come.
  if ((await textbox.inputValue()) === text) return;
  await textbox.fill(text);
  await Promise.all([answerOfSearch(page, text), textbox.press("Enter")]);
}

/** Chooses the option of an autocomplete field of the dialog, such as "Groep" or "Medewerker". */
async function choose(
  page: Page,
  dialog: Locator,
  field: string,
  option: string,
) {
  await dialog.getByRole("combobox", { name: field }).fill(option);
  // The options open in an overlay outside the dialog.
  await page.getByRole("option", { name: option, exact: true }).click();
}

/**
 * The actions of a werkvoorraad on the rows that the user selected. Their dialogs are named after the kind of rows,
 * such as "3 Zaken verdelen".
 */
function bulkActions(page: Page, soort: "Zaken" | "Taken") {
  return {
    async selectAll() {
      await page.getByRole("checkbox", { name: "Alles selecteren" }).check();
    },

    /** Assigns the selected rows. The dialog offers the medewerkers only after the choice of a groep. */
    async verdelen({
      groep,
      medewerker,
    }: {
      groep: string;
      medewerker: string;
    }) {
      await page.getByRole("button", { name: "Verdelen" }).click();
      const dialog = findDialog(page, `${soort} verdelen`);
      await choose(page, dialog, "Groep", groep);
      await choose(page, dialog, "Medewerker", medewerker);
      await confirmDialog(dialog, "Verdelen");
    },

    /**
     * Waits until ZAC has processed every selected row. Until then a dialog shows the progress and hides the list. For
     * zaken, the dialog closes when Open Notificaties has told ZAC about the new rol of each zaak.
     */
    async untilProcessed() {
      await expect(
        page.getByRole("dialog").filter({ has: page.getByRole("progressbar") }),
      ).toBeHidden({ timeout: 60_000 });
    },

    async vrijgeven(reden: string) {
      await page.getByRole("button", { name: "Vrijgeven" }).click();
      const dialog = findDialog(page, `${soort} vrijgeven`);
      await dialog.getByRole("textbox", { name: "Reden" }).fill(reden);
      await confirmDialog(dialog, "Vrijgeven");
    },
  };
}

/** A list of zaken from the search index, such as the werkvoorraad. */
function zakenLijst(page: Page, path: string) {
  return {
    async open() {
      await openList(page, path);
    },

    /** The row of the zaak. The row of the filters can also hold the zaaknummer, but in a textbox, not in a cell. */
    zaak(identificatie: string) {
      return page
        .getByRole("row")
        .filter({ has: page.getByRole("cell", { name: identificatie }) });
    },

    filterOnZaaknummer(identificatie: string) {
      return filter(page, "Zaaknummer", identificatie);
    },

    filterOnOmschrijving(omschrijving: string) {
      return filter(page, "Omschrijving", omschrijving);
    },

    async expectZaak(identificatie: string) {
      await this.filterOnZaaknummer(identificatie);
      await expect(this.zaak(identificatie)).toBeVisible();
    },

    async expectNoZaak(identificatie: string) {
      await this.filterOnZaaknummer(identificatie);
      await expect(this.zaak(identificatie)).toBeHidden();
    },
  };
}

/** The werkvoorraad of zaken: the open zaken that the user may read. */
export function zakenWerkvoorraad(page: Page) {
  return {
    ...zakenLijst(page, "/zaken/werkvoorraad"),
    ...bulkActions(page, "Zaken"),

    async assignToMe(identificatie: string) {
      await this.zaak(identificatie)
        .getByRole("button", { name: "Ken aan mij toe" })
        .click();
    },
  };
}

/** The list of afgehandelde zaken. */
export function afgehandeldeZaken(page: Page) {
  return zakenLijst(page, "/zaken/afgehandeld");
}

/** The zaken that are assigned to the user. */
export function mijnZaken(page: Page) {
  return zakenLijst(page, "/zaken/mijn");
}

/** The werkvoorraad of taken: the open taken that the user may read. */
export function takenWerkvoorraad(page: Page) {
  return {
    ...bulkActions(page, "Taken"),

    async open() {
      await openList(page, "/taken/werkvoorraad");
    },

    filterOnZaak(identificatie: string) {
      return filter(page, "Hoort bij zaak", identificatie);
    },

    /** The rows of the taken in the list, which have a checkbox to select the taak, unlike the rows of the headers. */
    taken() {
      return page.getByRole("row").filter({
        has: page.getByRole("checkbox", {
          name: /^(Selecteren|Deselecteren)$/,
        }),
      });
    },
  };
}

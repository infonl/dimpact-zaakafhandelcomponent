/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, type Page } from "@playwright/test";
import { chooseOption } from "./select";

const DEFAULT_GROEP = "Standaard behandelaarsgroep";

/** The zaakafhandelparameters of one zaaktype, for a beheerder. */
export function parametersPage(page: Page) {
  const defaultGroep = page.getByRole("combobox", { name: DEFAULT_GROEP });

  return {
    defaultGroep,

    /** Opens the zaakafhandelparameters from the list of the version of the zaaktype that is valid today. */
    async open(zaaktype: string) {
      await page.goto("/admin/parameters");
      await page
        .getByRole("row")
        .filter({
          has: page.getByRole("cell", { name: zaaktype, exact: true }),
        })
        .getByRole("link", { name: "Bewerken" })
        .click();
      await expect(defaultGroep).toBeVisible();
    },

    async chooseDefaultGroep(groep: string) {
      await chooseOption(page, DEFAULT_GROEP, groep);
    },
  };
}

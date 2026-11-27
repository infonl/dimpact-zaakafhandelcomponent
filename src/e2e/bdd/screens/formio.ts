/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, type Page } from "@playwright/test";

/** The form.io form of a task of a BPMN process. */
export function formioForm(page: Page) {
  return {
    /**
     * Chooses the option of a select. Form.io loads its options again when another field changes or ZAC redraws the
     * form, and can drop the choice while it does, so it chooses again until the choice holds.
     */
    async choose(field: string, option: string) {
      const select = page.getByRole("combobox", { name: field, exact: true });
      await expect(async () => {
        await select.selectOption({ label: option }, { timeout: 2_000 });
        const chosen = await select.evaluate((element: HTMLSelectElement) =>
          element.selectedOptions.item(0)?.text.trim(),
        );
        expect(chosen).toBe(option);
      }).toPass({ timeout: 30_000 });
    },

    async expectValue(field: string, value: string) {
      await expect(
        page.getByRole("textbox", { name: field, exact: true }),
      ).toHaveValue(value);
    },

    /** Submits the form, and waits until ZAC has completed the task. */
    async submit(button: string) {
      await Promise.all([
        page.waitForResponse(
          (response) =>
            response.request().method() === "PATCH" &&
            new URL(response.url()).pathname.endsWith("/rest/taken/complete") &&
            response.ok(),
        ),
        page.getByRole("button", { name: button, exact: true }).click(),
      ]);
    },
  };
}

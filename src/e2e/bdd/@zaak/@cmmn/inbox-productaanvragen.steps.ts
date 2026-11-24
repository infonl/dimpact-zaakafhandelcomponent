/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect } from "@playwright/test";
import { Given, Then, When } from "../../fixtures";
import { inboxProductaanvragen } from "../../screens/inbox-productaanvragen";
import { zaakPage } from "../../screens/zaak";
import {
  objectenOrSkip,
  registerProductaanvraag,
} from "../../support/objecten";

Given(
  "a productaanvraag {string} of a type that ZAC does not know",
  async ({ request, testData, $testInfo }, aanvraag: string) => {
    const type = testData.unique(aanvraag);
    await registerProductaanvraag(request, objectenOrSkip($testInfo), type);
    await testData.untilProductaanvraagIsInInbox(type);
  },
);

When("I open the inbox productaanvragen", async ({ page }) => {
  await inboxProductaanvragen(page).open();
});

Then(
  "I see the productaanvraag {string}",
  async ({ page, testData }, aanvraag: string) => {
    await expect(
      inboxProductaanvragen(page).productaanvraag(testData.unique(aanvraag)),
    ).toBeVisible();
  },
);

When(
  "I start a zaak from the productaanvraag {string}",
  async ({ page, testData }, aanvraag: string) => {
    await inboxProductaanvragen(page).createZaakFrom(testData.unique(aanvraag));
    await expect(
      page.getByRole("heading", { name: "Zaak aanmaken", level: 1 }),
    ).toBeVisible();
  },
);

Then(
  "the zaak shows the document of the productaanvraag",
  async ({ page, $testInfo }) => {
    const { aanvraagdocument } = objectenOrSkip($testInfo);
    await expect(zaakPage(page).document(aanvraagdocument.titel)).toBeVisible();
  },
);

Then(
  "the inbox productaanvragen no longer shows {string}",
  async ({ page, testData }, aanvraag: string) => {
    const inbox = inboxProductaanvragen(page);
    await inbox.open();
    await expect(inbox.productaanvraag(testData.unique(aanvraag))).toBeHidden();
  },
);

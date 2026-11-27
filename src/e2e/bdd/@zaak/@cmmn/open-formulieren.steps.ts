/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, type TestInfo } from "@playwright/test";
import path from "node:path";
import { ENV } from "../../../config/env";
import { Then, When } from "../../fixtures";
import { zaakPage } from "../../screens/zaak";
import { dataOf } from "../../support/zac-api";

const FORM = "indienen-aansprakelijkstelling-door-derden-behandelen-2";
const PHOTO = path.resolve(__dirname, "../files/schade.jpg");
const FORM_TITEL = "E2E test - Indienen aansprakelijkstelling door derden";
const SETTLE_MILLISECONDS = 1_500;
const REFERENCE = /OF-[A-Z0-9]{6,}/;
const STEP_TIMEOUT_MILLISECONDS = 60_000;
const DOCUMENTS_TIMEOUT_MILLISECONDS = 120_000;

function openFormsUrlOrSkip(testInfo: TestInfo) {
  testInfo.skip(
    !ENV.openFormsUrl,
    `The target "${ENV.target}" has no Open Formulieren.`,
  );
  // skip() ends the test when the target has no Open Formulieren, so it exists from here on.
  return ENV.openFormsUrl!;
}

When(
  "a resident submits the form about aansprakelijkstelling with a photo",
  async ({ browser, testData, $testInfo }) => {
    const openFormsUrl = openFormsUrlOrSkip($testInfo);
    // A resident has no session of ZAC.
    const context = await browser.newContext({
      storageState: { cookies: [], origins: [] },
      locale: "nl-NL",
    });
    const page = await context.newPage();
    const cookies = page.getByRole("button", { name: "Alles toestaan" });
    await page.addLocatorHandler(cookies, () => cookies.click());
    // Half a second after the typing the form may check its logic on the server. When the user goes on before that
    // ended, the form shows the fields of the old step in the new one, and nothing on the page tells when it ended.
    let typedAt = 0;
    let runningChecks = 0;
    const countCheck = (url: string, change: number) => {
      if (url.includes("/_check-logic")) runningChecks += change;
    };
    page.on("request", (request) => countCheck(request.url(), 1));
    page.on("requestfinished", (request) => countCheck(request.url(), -1));
    page.on("requestfailed", (request) => countCheck(request.url(), -1));
    const isSettled = () =>
      runningChecks === 0 && Date.now() - typedAt > SETTLE_MILLISECONDS;
    const nextTo = async (step: string) => {
      await expect
        .poll(isSettled, { timeout: STEP_TIMEOUT_MILLISECONDS })
        .toBe(true);
      await page.getByRole("button", { name: "Volgende" }).click();
      await expect(
        page.getByRole("heading", { name: step, exact: true }),
      ).toBeVisible();
    };
    const fill = async (label: string, value: string) => {
      // The form has hidden copies of some fields, which show for the test environment after the real ones.
      // eslint-disable-next-line playwright/no-nth-methods -- the form is not ours, and its copies have the same label
      const field = page.getByLabel(label).first();
      await field.fill(value);
      await expect(field).not.toHaveValue("");
      typedAt = Date.now();
    };
    const check = async (label: string, exact = false) => {
      await page.getByLabel(label, { exact }).check();
      typedAt = Date.now();
    };

    await page.goto(`${openFormsUrl}/${FORM}/startpagina`);
    await page.getByRole("button", { name: "Formulier starten" }).click();

    await fill("Voornaam", testData.unique("Alice"));
    await fill("Voorletter(s)", "A");
    await fill("Tussenvoegsel(s)", "den");
    await fill("Achternaam", "Test");
    await nextTo("Voorval");

    await fill("Omschrijving van het voorval", "Gat in de weg");
    await fill("Datum en tijdstip voorval", "10-10-2024 00:00");
    await check("materiële schade aan een");
    await check("nee", true);
    await check("ja, digitaal bij deze melding");
    await fill("Straat", "teststraat");
    await fill("Nadere omschrijving van de", "Voor het stadhuis");
    await fill("U kunt hier aangeven waarom", "Achterstallig onderhoud");
    await nextTo("Nadere gegevens schade");

    await fill("Omschrijving schade voertuig", "Klapband");
    await fill("Merk voertuig", "CITROËN");
    await fill("Kenteken voertuig", "EE-RP-10");
    await fill("Bedrijfsnaam", "Verzekeraar bv");
    await fill("Polisnummer", "111111111");
    await check("ja");
    await check("AllRisk");
    await nextTo("Overig");

    await page
      .getByRole("button", { name: "Foto", exact: true })
      .setInputFiles(PHOTO);
    await expect(page.getByText("schade.jpg")).toBeVisible();
    typedAt = Date.now();
    await nextTo("Controleer en bevestig");

    await page.getByLabel(/Ja, ik heb kennis genomen van het/).check();
    await page.getByRole("button", { name: "Verzenden" }).click();
    const confirmation = page.getByText(REFERENCE);
    await expect(confirmation).toBeVisible();
    const reference = REFERENCE.exec(await confirmation.innerText())?.[0];
    if (!reference) throw new Error("The confirmation has no reference.");
    await context.close();
    await testData.untilZaakWithToelichting(reference);
  },
);

Then("the submission becomes a zaak", async ({ page, testData }) => {
  await zaakPage(page).open(testData.zaak.identificatie);
});

Then(
  "the zaak shows the PDF of the submission and the photo",
  async ({ page, apiAs, testData }) => {
    const zaak = zaakPage(page);
    const { identificatie } = testData.zaak;
    // Open Formulieren adds the documents after it registered the zaak, and the page does not always hear of that.
    await expect(async () => {
      await zaak.open(identificatie);
      await expect(zaak.document(FORM_TITEL)).toHaveCount(2);
    }).toPass({ timeout: DOCUMENTS_TIMEOUT_MILLISECONDS });
    // Both documents have the titel of the form and the list only shows icons for the formats.
    const api = await apiAs("behandelaar");
    const { uuid } = await dataOf(
      api.GET("/rest/zaken/zaak/id/{identificatie}", {
        params: { path: { identificatie } },
      }),
    );
    const documenten = await dataOf(
      api.PUT("/rest/informatieobjecten/informatieobjectenList", {
        body: { zaakUUID: uuid },
      }),
    );
    expect(documenten.map(({ formaat }) => formaat).sort()).toEqual([
      "application/pdf",
      "image/jpeg",
    ]);
  },
);

/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, type Page } from "@playwright/test";
import path from "node:path";
import { Then, When } from "../../fixtures";
import { documentPage } from "../../screens/document";
import { chooseOption } from "../../screens/select";
import { zaakPage } from "../../screens/zaak";
import { mailServerOrNote, untilMailArrives } from "../../support/mail";
import { dataOf } from "../../support/zac-api";
import { seededPersoonOrSkip } from "../seed";

const FILES_DIRECTORY = path.resolve(__dirname, "../files");
// The list shows a new document only after Open Notificaties has told ZAC about it, which takes a few seconds.
const DOCUMENT_LIST_TIMEOUT_MILLISECONDS = 30_000;

function onderwerpOfOntvangstbevestiging(identificatie: string) {
  return `Ontvangstbevestiging van zaak ${identificatie}`;
}

async function openDocument(page: Page, identificatie: string, titel: string) {
  const zaak = zaakPage(page);
  await zaak.open(identificatie);
  await zaak
    .document(titel)
    .getByRole("link", { name: "Document bekijken" })
    .click();
  const document = documentPage(page);
  await document.expectOpened(titel);
  return document;
}

When(
  "I add the document {string} with the titel {string} and the type {seeded}",
  async ({ page, testData }, bestand: string, titel: string, type: string) => {
    await zaakPage(page).chooseFromMenu("Document toevoegen");
    const bestandsnaam = page.getByRole("textbox", { name: "Bestandsnaam" });
    // The form opens no file chooser until ZAC has told which formats it allows.
    await expect(bestandsnaam).toBeEnabled();
    const [fileChooser] = await Promise.all([
      page.waitForEvent("filechooser"),
      bestandsnaam.click(),
    ]);
    await fileChooser.setFiles(path.join(FILES_DIRECTORY, bestand));
    await chooseOption(page, "Documenttype", type);
    await chooseOption(page, "Status", "Definitief");
    const uniqueTitel = testData.unique(titel);
    await page.getByRole("textbox", { name: "Titel" }).fill(uniqueTitel);
    await Promise.all([
      page.waitForResponse(
        (response) =>
          response.request().method() === "POST" &&
          new URL(response.url()).pathname.includes(
            "/rest/informatieobjecten/informatieobject/",
          ) &&
          response.ok(),
      ),
      page.getByRole("button", { name: "Toevoegen", exact: true }).click(),
    ]);
    testData.rememberDocument(titel, uniqueTitel);
  },
);

Then(
  "the document {string} is listed at the zaak",
  async ({ page, testData }, titel: string) => {
    await expect(
      zaakPage(page).document(testData.documentTitled(titel)),
    ).toBeVisible({ timeout: DOCUMENT_LIST_TIMEOUT_MILLISECONDS });
  },
);

Then(
  "the document {string} shows the titel, the taal, the auteur and the creatiedatum",
  async ({ page, testData, accountOfRole }, titel: string) => {
    const document = await openDocument(
      page,
      testData.zaak.identificatie,
      testData.documentTitled(titel),
    );
    await document.expectGegeven("Titel", testData.documentTitled(titel));
    await document.expectGegeven("Taal", "Nederlands");
    await document.expectGegeven(
      "Auteur",
      accountOfRole("behandelaar").displayName,
    );
    // ZAC shows a date with non-breaking hyphens.
    const today = new Intl.DateTimeFormat("nl-NL", {
      timeZone: "Europe/Amsterdam",
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    })
      .format(new Date())
      .replaceAll("-", "\u2011");
    await document.expectGegeven("Creatiedatum", today);
  },
);

When(
  "I send the ontvangstbevestiging to the initiator",
  async ({ page, testData, $testInfo }) => {
    const { emailadres } = seededPersoonOrSkip($testInfo);
    const zaak = zaakPage(page);
    await zaak.open(testData.zaak.identificatie);
    await zaak.chooseFromMenu("Bevestiging sturen");
    const ontvanger = page.getByRole("textbox", { name: "Ontvanger" });
    await page
      .getByRole("button", { name: "Contact-e-mailadres toevoegen" })
      .click();
    await expect(ontvanger).toHaveValue(emailadres);
    // ZAC accepts the form only after the user typed in it, and the button does not count as typing.
    await ontvanger.fill(emailadres);
    // Sending the mail also stores it as a PDF document, which can take longer than an expect waits.
    await Promise.all([
      page.waitForResponse(
        (response) =>
          new URL(response.url()).pathname.includes(
            "/rest/mail/acknowledge/",
          ) && response.ok(),
      ),
      page.getByRole("button", { name: "Verstuur", exact: true }).click(),
    ]);
  },
);

Then(
  "the initiator receives the ontvangstbevestiging",
  async ({ request, testData, $testInfo }) => {
    const mailServer = mailServerOrNote($testInfo);
    if (!mailServer) return;
    await untilMailArrives(request, mailServer, {
      ontvanger: seededPersoonOrSkip($testInfo).emailadres,
      onderwerp: onderwerpOfOntvangstbevestiging(testData.zaak.identificatie),
    });
  },
);

Then(
  "the ontvangstbevestiging is stored at the zaak as a PDF document",
  async ({ page, apiAs, testData }) => {
    const { identificatie } = testData.zaak;
    const titel = onderwerpOfOntvangstbevestiging(identificatie);
    await expect(zaakPage(page).document(titel)).toBeVisible({
      timeout: DOCUMENT_LIST_TIMEOUT_MILLISECONDS,
    });
    // The list only shows an icon for the format, so the step reads it from the REST API.
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
    expect(
      documenten.find((document) => document.titel === titel)?.formaat,
    ).toBe("application/pdf");
  },
);

Then(
  "the zaak shows that the ontvangstbevestiging is sent",
  async ({ page }) => {
    await expect(
      zaakPage(page).indicatie("Geen bevestiging verstuurd"),
    ).toBeHidden();
  },
);

When(
  "I record that the document {string} was sent by post today",
  async ({ page, testData }, titel: string) => {
    const zaak = zaakPage(page);
    await zaak.open(testData.zaak.identificatie);
    await zaak.chooseFromMenu("Document verzenden");
    // The documents of the zaak stay in the page behind the panel, inert.
    await page
      .getByRole("group")
      .filter({ has: page.getByRole("columnheader", { name: "Documenttype" }) })
      .getByRole("row", { name: testData.documentTitled(titel) })
      .getByRole("checkbox")
      .check();
    const verzenden = page.getByRole("button", {
      name: "Verzenden",
      exact: true,
    });
    await verzenden.click();
    await expect(verzenden).toBeHidden();
  },
);

Then(
  "the document {string} shows the indicatie {string}",
  async ({ page, testData }, titel: string, indicatie: string) => {
    const document = await openDocument(
      page,
      testData.zaak.identificatie,
      testData.documentTitled(titel),
    );
    await expect(document.indicatie(indicatie)).toBeVisible();
  },
);

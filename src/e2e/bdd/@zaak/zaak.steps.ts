/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Given, When } from "../fixtures";
import { zaakPage } from "../screens/zaak";
import type { TestData } from "../support/test-data";
import { SEED, seededPersoonOrSkip } from "./seed";

Given(
  "a zaak in intake with a person as initiator",
  async ({ testData, $testInfo }) => {
    const { bsn } = seededPersoonOrSkip($testInfo);
    const { naam } = await testData.groepOf("behandelaar");
    await testData.createZaak({
      zaaktype: SEED.cmmnZaaktype,
      groep: naam,
      omschrijving: "zaak in intake",
      initiator: { bsn },
    });
  },
);

async function createZaakInBehandeling(
  testData: TestData,
  zaaktype: string,
  omschrijving: string,
) {
  const { naam } = await testData.groepOf("behandelaar");
  const zaak = await testData.createZaak({
    zaaktype,
    groep: naam,
    omschrijving,
  });
  await testData.finishIntake(zaak);
  return zaak;
}

Given("a zaak in behandeling", async ({ testData }) => {
  await createZaakInBehandeling(
    testData,
    SEED.cmmnZaaktype,
    "zaak in behandeling",
  );
});

Given(
  "a zaak {string} in behandeling",
  async ({ testData }, omschrijving: string) => {
    await createZaakInBehandeling(testData, SEED.cmmnZaaktype, omschrijving);
  },
);

Given(
  "a zaak {string} in behandeling, of a zaaktype that the hoofdzaak allows as deelzaak",
  async ({ testData }, omschrijving: string) => {
    await createZaakInBehandeling(
      testData,
      SEED.cmmnDeelzaaktype,
      omschrijving,
    );
  },
);

When("I open the zaak", async ({ page, testData }) => {
  await zaakPage(page).open(testData.zaak.identificatie);
});

When(
  "I open the zaak {string}",
  async ({ page, testData }, omschrijving: string) => {
    await zaakPage(page).open(
      testData.zaakDescribed(omschrijving).identificatie,
    );
  },
);

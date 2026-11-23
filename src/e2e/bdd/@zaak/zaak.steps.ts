/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Given, When } from "../fixtures";
import { zaakPage } from "../screens/zaak";
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

Given("a zaak in behandeling", async ({ testData }) => {
  const { naam } = await testData.groepOf("behandelaar");
  const zaak = await testData.createZaak({
    zaaktype: SEED.cmmnZaaktype,
    groep: naam,
    omschrijving: "zaak in behandeling",
  });
  await testData.finishIntake(zaak);
});

When("I open the zaak", async ({ page, testData }) => {
  await zaakPage(page).open(testData.zaak.identificatie);
});

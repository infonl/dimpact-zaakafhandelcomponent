/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Given, When } from "@cucumber/cucumber";
import path from "path";
import { CustomWorld } from "support/worlds/world";
import uniqid from "uniqid";
import { z } from "zod";
import { profiles } from "../support/open-forms/profiles";
import { ONE_MINUTE_IN_MS, ONE_SECOND_IN_MS } from "../support/time-constants";

export const profilesSchema = z.enum(["Alice"]);

Given(
  "Resident {string} fills in the indienen-aansprakelijkheid-behandelen open-forms form",
  { timeout: ONE_MINUTE_IN_MS },
  async function (
    this: CustomWorld,
    profileType: z.infer<typeof profilesSchema>,
  ) {
    const parsedProfile = profilesSchema.parse(profileType);
    const profile = profiles[parsedProfile];
    const id = uniqid();
    this.testStorage.set("open-forms-testid", id);

    const firstNameInput = this.page.getByLabel("Voornaam").nth(0);
    const firstLetterInput = this.page.getByLabel("Voorletter(s)").nth(0);
    const infixInput = this.page.getByLabel("Tussenvoegsel(s)").nth(0);
    const lastNameInput = this.page.getByLabel("Achternaam").nth(0);

    await this.page.goto(
      `${this.worldParameters.urls.openForms}/indienen-aansprakelijkstelling-door-derden-behandelen-2/startpagina`,
    );

    const cookieConsentButton = this.page.getByRole("button", {
      name: "Alles toestaan",
    });
    await this.page.addLocatorHandler(cookieConsentButton, async () => {
      await cookieConsentButton.click();
    });

    await this.page.getByRole("button", { name: "Formulier starten" }).click();

    // Personal details
    await firstNameInput.fill(
      profile.personalDetails.firstName + `:e2eid=${id}`,
    );
    await firstLetterInput.fill(profile.personalDetails.initials);
    await infixInput.fill(profile.personalDetails.prefix);
    await lastNameInput.fill(profile.personalDetails.lastName);
    // wait a bit before submitting this form tab because otherwise the entered data is sometimes not submitted (timing issue in Open Forms)
    await this.page.waitForTimeout(1000);
    await this.page.getByRole("button", { name: "Volgende" }).click();

    // Incident details
    await this.page.getByLabel("Omschrijving van het voorval").click();
    await this.page
      .getByLabel("Omschrijving van het voorval")
      .fill(profile.incidentDetails.description);
    await this.page
      .getByLabel("Datum en tijdstip voorval")
      .fill(profile.incidentDetails.date);

    await this.page.getByLabel("materiële schade aan een").check();
    // "Waren er getuigen aanwezig?"
    await this.page.getByLabel("nee", { exact: true }).check();
    await this.page.getByLabel("ja, digitaal bij deze melding").check();
    await this.page.getByLabel("Straat").fill("teststraat");
    await this.page
      .getByLabel("Nadere omschrijving van de")
      .fill(profile.incidentDetails.location.furtherDescription);
    await this.page
      .getByLabel("U kunt hier aangeven waarom")
      .fill(profile.incidentDetails.reasonForMunicipalityLiability);
    await this.page.getByRole("button", { name: "Volgende" }).click();

    // Damage details
    await this.page
      .getByLabel("Omschrijving schade voertuig")
      .fill(profile.damageDetails.description);
    await this.page
      .getByLabel("Merk voertuig")
      .fill(profile.damageDetails.vehicle.make);
    await this.page
      .getByLabel("Kenteken voertuig")
      .fill(profile.damageDetails.vehicle.licensePlate);
    await this.page
      .getByLabel("Bedrijfsnaam")
      .fill(profile.damageDetails.insurance.companyName);
    await this.page
      .getByLabel("Polisnummer")
      .fill(profile.damageDetails.insurance.policyNumber);
    await this.page.getByLabel("ja").check();
    await this.page.getByLabel("AllRisk").check();
    await this.page.getByRole("button", { name: "Volgende" }).click();

    const fileInput = this.page.locator('input[type="file"]').first();
    const filePath = path.join(__dirname, profile.documents.photo);
    await fileInput.setInputFiles(filePath);
    // wait a bit until file has been uploaded
    await this.page.waitForTimeout(ONE_SECOND_IN_MS);

    await this.page.getByRole("button", { name: "Volgende" }).click();
    await this.page
      .getByLabel(
        "Ja, ik heb kennis genomen van het en geef uitdrukkelijk toestemming voor het verwerken van de door mij opgegeven gegevens.",
      )
      .check();
  },
);

When(
  "Resident {string} submits the open-forms form",
  { timeout: ONE_MINUTE_IN_MS },
  async function (
    this: CustomWorld,
    profileType: z.infer<typeof profilesSchema>,
  ) {
    profilesSchema.parse(profileType);
    await this.page.getByText("Verzenden").click();
  },
);

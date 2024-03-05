import { Given, When } from "@cucumber/cucumber";
import path from "path";
import { CustomWorld } from "support/worlds/world";
import uniqid from "uniqid";
import { z } from "zod";
import { profiles } from "../support/indienen-aansprakelijkstelling-door-derden/profiles";

export const profilesSchema = z.enum(["Alice"]);

const ONE_MINUTE_IN_MS = 60_000;
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

    await this.page.goto(
      `${this.worldParameters.urls.openForms}/indienen-aansprakelijkstelling-door-derden-behandelen-2/startpagina`,
    );
    await this.page.getByRole("button", { name: "Formulier starten" }).click();

    // Personal details
    await this.page.locator("#e67qrl9-demovoornaam").click();
    await this.page
      .locator("#e67qrl9-demovoornaam")
      .fill(profile.personalDetails.firstName + `:e2eid=${id}`);
    await this.page.locator("#e5c2pwf-demovoorletters").click();
    await this.page
      .locator("#e5c2pwf-demovoorletters")
      .fill(profile.personalDetails.initials);
    await this.page.locator("#ew8j534-demotussenvoegsels").click();
    await this.page
      .locator("#ew8j534-demotussenvoegsels")
      .fill(profile.personalDetails.prefix);
    await this.page.locator("#efjsdnh-demoachternaam").click();
    await this.page
      .locator("#efjsdnh-demoachternaam")
      .fill(profile.personalDetails.lastName);
    await this.page.getByRole("button", { name: "Volgende" }).click();

    // Incident details
    await this.page.getByLabel("Omschrijving van het voorval").click();
    await this.page
      .getByLabel("Omschrijving van het voorval")
      .fill(profile.incidentDetails.description);
    await this.page.getByRole("textbox", { name: "dd-MM-yyyy HH:mm" }).click();
    await this.page
      .getByRole("textbox", { name: "dd-MM-yyyy HH:mm" })
      .fill(profile.incidentDetails.date);
    await this.page.getByLabel("materiële schade aan een").check();
    await this.page.getByLabel("ja", { exact: true }).check();
    await this.page.waitForSelector("#ej3ph74-hoeveelGetuigen", {
      state: "attached",
    });
    await this.page.getByText("Hoeveel getuigen?", { exact: true });
    await this.page
      .locator("#ej3ph74-hoeveelGetuigen")
      .evaluate((node) => node.click());
    await this.page.getByRole("option", { name: "1" }).click();
    await this.page.getByLabel("ja, digitaal bij deze melding").check();
    await this.page.getByRole("combobox").nth(1).click();
    await this.page.getByRole("option", { name: "Enschede" }).click();
    await this.page.getByLabel("Straat").click();
    await this.page.getByLabel("Straat").fill("teststraat");
    await this.page.getByLabel("Nadere omschrijving van de").click();
    await this.page
      .getByLabel("Nadere omschrijving van de")
      .fill(profile.incidentDetails.location.furtherDescription);
    await this.page.getByLabel("U kunt hier aangeven waarom").click();
    await this.page
      .getByLabel("U kunt hier aangeven waarom")
      .fill(profile.incidentDetails.reasonForMunicipalityLiability);
    await this.page.getByRole("button", { name: "Volgende" }).click();

    // Damage details
    await this.page.getByLabel("Omschrijving schade voertuig").click();
    await this.page
      .getByLabel("Omschrijving schade voertuig")
      .fill(profile.damageDetails.description);
    await this.page.getByLabel("Merk voertuig").click();
    await this.page
      .getByLabel("Merk voertuig")
      .fill(profile.damageDetails.vehicle.make);
    await this.page.getByLabel("Kenteken voertuig").click();
    await this.page
      .getByLabel("Kenteken voertuig")
      .fill(profile.damageDetails.vehicle.licensePlate);
    await this.page.getByLabel("Bedrijfsnaam").click();
    await this.page
      .getByLabel("Bedrijfsnaam")
      .fill(profile.damageDetails.insurance.companyName);
    await this.page.getByLabel("Polisnummer").click();
    await this.page
      .getByLabel("Polisnummer")
      .fill(profile.damageDetails.insurance.policyNumber);
    await this.page.getByLabel("ja").check();
    await this.page.getByLabel("AllRisk").check();
    await this.page.getByRole("button", { name: "Volgende" }).click();

    // Witness details
    await this.page.getByLabel("Achternaam").click();
    await this.page
      .getByLabel("Achternaam")
      .fill(profile.witnessDetails[0].lastName);
    await this.page.getByLabel("Tussenvoegsels").click();
    await this.page
      .getByLabel("Tussenvoegsels")
      .fill(profile.witnessDetails[0].prefix ?? "");
    await this.page.getByLabel("Voornamen").click();
    await this.page
      .getByLabel("Voornamen")
      .fill(profile.witnessDetails[0].firstName ?? "");
    await this.page.getByPlaceholder("____ __").click();
    await this.page
      .getByPlaceholder("____ __")
      .fill(profile.witnessDetails[0].postalCode ?? "");
    await this.page.getByLabel("Huisnummer").click();
    await this.page
      .getByLabel("Huisnummer")
      .fill(profile.witnessDetails[0].houseNumber ?? "");
    await this.page.getByRole("button", { name: "Volgende" }).click();

    // Documents
    const fileChooserPromise = this.page.waitForEvent("filechooser");
    await this.page.getByRole("link", { name: "blader" }).first().click();
    const fileChooser = await fileChooserPromise;
    await fileChooser.setFiles(path.join(__dirname, profile.documents.photo));
    const loader = await this.page.getByText("Bezig met uploaden...");
    await this.expect(loader).toHaveCount(0);

    const fileChooserPromise2 = this.page.waitForEvent("filechooser");
    await this.page.getByRole("link", { name: "blader" }).first().click();
    const fileChooser2 = await fileChooserPromise2;
    await fileChooser2.setFiles(
      path.join(__dirname, profile.documents.invoice),
    );
    const loader2 = await this.page.getByText("Bezig met uploaden...");
    await this.expect(loader2).toHaveCount(0);

    await this.page.waitForTimeout(5000);

    await this.page.getByRole("button", { name: "Volgende" }).click();
    await this.page
      .getByLabel(
        "Ja, ik heb kennis genomen van het  en geef uitdrukkelijk toestemming voor het verwerken van de door mij opgegeven gegevens.",
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

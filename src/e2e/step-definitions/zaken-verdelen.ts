import { Given, Then, When } from "@cucumber/cucumber";
import { CustomWorld } from "support/worlds/world";

const ONE_MINUTE_IN_MS = 60_000;

const zaakCheckmarkTitle = "Selecteren";
let _noOfZaken = 0;

Given(
  "There at least {int} zaken",
  async function (this: CustomWorld, noOfZaken: number) {
    _noOfZaken = noOfZaken;
    const zaakCount = await this.page
      .getByLabel(zaakCheckmarkTitle, { exact: true })
      .count();
    this.expect(zaakCount).toBeGreaterThanOrEqual(noOfZaken);
  },
);

When(
  "{string} selects that number of zaken",
  async function (this: CustomWorld, s: string) {
    for (let i = 0; i < _noOfZaken; i++) {
      await this.page
        .getByLabel(zaakCheckmarkTitle, { exact: true })
        .first()
        .setChecked(true);
    }
  },
);

When(
  "{string} verdeels the zaken to group {string}",
  async function (this: CustomWorld, s: string, groep: string) {
    await this.page.getByTitle("Verdelen").click();
    const expectedLabel = "Zaak toekennen aan groep";
    await this.page.getByLabel(expectedLabel).click();
    await this.page.getByRole("option", { name: groep }).click();
    await this.page.getByLabel("Reden").fill("Dummy reason");
    await this.page.locator("#zakenVerdelen_button").click();
  },
);

Then(
  "{string} gets a message confirming that the verdelen of zaken is complete",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, s: string) {
    await this.page
      .getByText(`${_noOfZaken} zaken zijn verdeeld`)
      .waitFor({ timeout: ONE_MINUTE_IN_MS });
  },
);

import { Given, Then, When } from "@cucumber/cucumber";
import { CustomWorld } from "support/worlds/world";

const ONE_MINUTE_IN_MS = 60_000;

let _noOfTaken = 0;

Given(
  "There at least {int} taken",
  async function (this: CustomWorld, noOfTaken: number) {
    _noOfTaken = noOfTaken;
    const zaakCount = await this.page
      .getByLabel("Selecteren", { exact: true })
      .count();
    this.expect(zaakCount).toBeGreaterThanOrEqual(noOfTaken);
  },
);

When(
  "{string} selects that number of taken",
  async function (this: CustomWorld, s: string) {
    for (let i = 0; i < _noOfTaken; i++) {
      await this.page
        .getByLabel("Selecteren", { exact: true })
        .first()
        .setChecked(true);
    }
  },
);

When(
  "{string} verdeels the taken to group {string}",
  async function (this: CustomWorld, s: string, groep: string) {
    await this.page.getByTitle("Verdelen").click();
    const expectedLabel = "Taak toekennen aan groep";
    await this.page.getByLabel(expectedLabel).click();
    await this.page.getByRole("option", { name: groep }).click();
    await this.page.getByLabel("Reden").fill("Dummy reason");
    await this.page.locator("#takenVerdelen_button").click();
  },
);

Then(
  "{string} gets a message confirming that the verdelen of taken is complete",
  { timeout: ONE_MINUTE_IN_MS },
  async function (this: CustomWorld, s: string) {
    await this.page
      .getByText(`${_noOfTaken} zaken zijn verdeeld`)
      .waitFor({ timeout: ONE_MINUTE_IN_MS });
  },
);

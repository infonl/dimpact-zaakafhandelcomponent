import { test, expect } from "@playwright/test";

test("test", async ({ page }) => {
    await page.goto("http://zaakafhandelcomponent-zac-dev.westeurope.cloudapp.azure.com");
    await page.getByLabel("Username or email").click();
    await page.getByLabel("Username or email").fill("testuser1");
    await page.getByLabel("Password").click();
    await page.getByLabel("Password").fill("testuser1");
    await page.getByRole("button", { name: "Sign In" }).click();
    await page.getByLabel("Zaak toevoegen").click();
    await page.getByLabel("Casetype").click();
    await page.getByRole("option", { name: "Bezwaar behandelen" }).click();
    await page
        .locator("div")
        .filter({ hasText: /^person$/ })
        .click();
    await page.getByLabel("CSN").dblclick({
        button: "right",
    });
    await page.getByLabel("CSN").fill("999993896");
    await page.getByLabel("emoji_people Citizen").getByRole("button", { name: "Search" }).click();
    await page.getByRole("button", { name: "Select" }).click();
    await page
        .locator("div")
        .filter({ hasText: /^gps_fixed$/ })
        .click();
    await page.getByPlaceholder("Search by address, zip code or place of residence").click();
    await page.getByPlaceholder("Search by address, zip code or place of residence").fill("1112gv");
    await page.getByPlaceholder("Search by address, zip code or place of residence").press("Enter");
    await page
        .getByRole("row", { name: "Show related data 0384200000005901 Address Meelbeskamp 49, 1112GV Diemen Select" })
        .getByTitle("Select")
        .click();
    await page.locator("button").filter({ hasText: "close" }).click();
    await page.getByText("Communication channel- Select a communication channel -").click();
    await page.getByText("E-mail").click();
    await page.getByText("Confidentiality notice Case classified").click();
    await page.getByText("Public", { exact: true }).click();
    await page.getByLabel("Description").click();
    await page.getByLabel("Description").fill("E2etes1");
    await page.getByRole("button", { name: "Create" }).click();
});

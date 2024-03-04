import { After, AfterAll, AfterStep, Before, Status } from "@cucumber/cucumber";
import { CustomWorld, authFile } from "../support/worlds/world"
import fs from "fs";
import {testStorageFile} from "../utils/TestStorage.service";

const ONE_MINUTE_IN_MS = 60_000;

Before(async function (this: CustomWorld) {
    await this.init();
})

After({ timeout: ONE_MINUTE_IN_MS }, async function (this: CustomWorld) {
    await this.context.storageState({ path: authFile });
    await this.stop();
})

AfterAll(async function (this: CustomWorld) {
    fs.unlinkSync(testStorageFile);
    console.log("Deleted test storage file successfully.");
    fs.unlinkSync(authFile);
    console.log("Deleted auth file successfully.");
})

AfterStep(async function (this: CustomWorld, { result, testStepId }) {
    if (result.status === Status.FAILED) {
        const screenshot = await this.page.screenshot({ path: `./reports/screenshots/${testStepId}.png` });
        this.attach(screenshot, "image/png");
    }
})

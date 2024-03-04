import { After, AfterAll, AfterStep, Before, Status } from "@cucumber/cucumber";
import { CustomWorld, authFile } from "../support/worlds/world"
import { v4 as uuidv4 } from 'uuid';
import fs from 'fs'
import { testStorageFile } from "../utils/TestStorage.service";

Before(async function (this: CustomWorld) {
    await this.init();
})

After(async function (this: CustomWorld) {
    const storageState = await this.context.storageState();
    fs.writeFileSync(authFile, JSON.stringify(storageState));
    await this.stop();
})

AfterAll(async function (this: CustomWorld) {
    fs.unlink(testStorageFile, (err) => {
        if (err) {
            throw err;
        }

        console.log("Deleted test storage file successfully.");
    });
    fs.unlink(authFile, (err) => {
        if (err) {
            throw err;
        }

        console.log("Deleted auth file successfully.");
    });
    return
})

AfterStep(async function (this: CustomWorld, { result }) {
    if (result.status === Status.FAILED) {
        const screenshot = await this.page.screenshot({ path: `./reports/screenshots/${uuidv4()}.png` });
        this.attach(screenshot, "image/png");
    }
})

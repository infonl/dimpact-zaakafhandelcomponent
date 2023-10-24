import { After, AfterAll, AfterStep, Before, Status } from "@cucumber/cucumber";
import { CustomWorld, authFile } from "../support/worlds/world"
import { v4 as uuidv4 } from 'uuid';
import fs from 'fs'

Before(async function (this: CustomWorld) {
    await this.init();
})

After(async function (this: CustomWorld) {
    fs.writeFileSync(authFile, JSON.stringify(await this.context.storageState()));
    await this.stop();
})

AfterAll(async function (this: CustomWorld) {
    fs.unlink(authFile, (err) => {
        if (err) {
            throw err;
        }
    
        console.log("Delete File successfully.");
    });
    await this.stop();
}) 

AfterStep(async function (this: CustomWorld, { result }) {
    if (result.status === Status.FAILED) {
        const screenshot = await this.page.screenshot({ path: `./reports/screenshots/${uuidv4()}.png` });
        this.attach(screenshot, "image/png");
    }
})
import { After, AfterAll, AfterStep, Before, Status } from "@cucumber/cucumber";
import { CustomWorld } from "../support/worlds/world"
import { v4 as uuidv4 } from 'uuid';
import { afterEach } from "node:test";

Before(async function (this: CustomWorld) {
    await this.init();
})

After(async function (this: CustomWorld) {
    await this.stop();
})

AfterStep(async function (this: CustomWorld, { result }) {
    if (result.status === Status.FAILED) {
        const screenshot = await this.page.screenshot({ path: `./screenshots/${uuidv4()}.png` });
        this.attach(screenshot, "image/png");
    }
})
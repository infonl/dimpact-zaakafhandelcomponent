import { After, AfterAll, AfterStep, Before, Status } from "@cucumber/cucumber";
import { CustomWorld } from "../support/worlds/world"
import { v4 as uuidv4 } from 'uuid';

Before(async function (this: CustomWorld) {
    await this.init();
})

After(async function (this: CustomWorld) {
    await this.stop();
})

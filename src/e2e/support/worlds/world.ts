/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
import { World, setWorldConstructor } from "@cucumber/cucumber";
import playwright from "playwright";
import { worldParametersScheme } from "../../utils/schemes";
import {z} from 'zod'


export class CustomWorld extends World {
    page: playwright.Page;
    browser: playwright.Browser;
    context: playwright.BrowserContext; 
    initialized: boolean = false;
    worldParameters: z.infer<typeof worldParametersScheme>['parameters'];

    constructor(attach: unknown) {
        const res = worldParametersScheme.parse(attach)
        super({attach: res.attach, parameters: res.parameters, log: res.log });

        this.worldParameters = res.parameters;
    }

    async init() {
        this.browser = await playwright.chromium.launch({
            headless: false,
        });
        this.context = await this.browser.newContext();
        this.page = await this.context.newPage();
        this.initialized = true;
    }

    async openUrl(url: string) {
        if(!this.initialized) {
            await this.init();
        }
        await this.page.goto(url);
    }
}
setWorldConstructor(CustomWorld)
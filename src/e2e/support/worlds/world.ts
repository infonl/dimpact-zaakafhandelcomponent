/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { World, setWorldConstructor } from "@cucumber/cucumber";
import playwright from "playwright";
import { worldParametersScheme } from "../../utils/schemes";
import {z} from 'zod'
import fs from 'fs';
import { expect } from '@playwright/test';
import { TestStorageService } from "../../utils/TestStorage.service"

export const authFile = 'user.json';

export class CustomWorld extends World {
    page: playwright.Page;
    expect: typeof expect = expect;
    browser: playwright.Browser;
    context: playwright.BrowserContext; 
    initialized: boolean = false;
    worldParameters: z.infer<typeof worldParametersScheme>['parameters'];
    testStorage: TestStorageService

    constructor(attach: any) {
        const res = worldParametersScheme.parse(attach)
        super({attach: res.attach, parameters: res.parameters, log: res.log });
        this.worldParameters = res.parameters;
        this.testStorage = new TestStorageService();
    }

    async init() {
        if (!fs.existsSync(authFile)) {
            fs.writeFileSync(authFile, '{}');
        }
        this.browser = await playwright.chromium.launch({
            headless: this.worldParameters.headless,
            args: ['--lang=nl-NL'],
        });
        this.context = await this.browser.newContext({
            storageState: authFile,
            locale: 'nl-NL',
            recordVideo: { dir: 'reports/videos/' }
        });
        this.page = await this.context.newPage();
        this.initialized = true;
    }

    async stop() {
        await this.context.close();
        await this.browser.close();
        this.initialized = false;
    }

    async openUrl(url: string) {
        await this.page.goto(url);
    }
}
setWorldConstructor(CustomWorld)

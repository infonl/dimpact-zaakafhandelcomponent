/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
import { IWorldOptions, World, setWorldConstructor } from "@cucumber/cucumber";
import playwright from "playwright";
import {fetch} from 'cross-fetch'

export class CustomWorld extends World {
    page: playwright.Page;
    browser: playwright.Browser;
    context: playwright.BrowserContext; 
    initialized: boolean = false;

    async init() {
        this.browser = await playwright.chromium.launch({
            headless: true,
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
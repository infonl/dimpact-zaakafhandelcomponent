/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injector } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { provideTranslateService, TranslateService } from "@ngx-translate/core";
import { paginatorLanguageInitializerFactory } from "./paginator-language-initializer";

describe(paginatorLanguageInitializerFactory.name, () => {
  async function initializeWithBrowserLanguage(browserLanguage?: string) {
    TestBed.configureTestingModule({ providers: [provideTranslateService()] });
    const translateService = TestBed.inject(TranslateService);
    jest
      .spyOn(translateService, "getBrowserLang")
      .mockReturnValue(browserLanguage);

    await paginatorLanguageInitializerFactory(
      translateService,
      TestBed.inject(Injector),
    )();

    return translateService;
  }

  it("starts the app in English for an English browser, so no language switch follows the first render", async () => {
    const translateService = await initializeWithBrowserLanguage("en");

    expect(translateService.getCurrentLang()).toBe("en");
  });

  it("starts the app in Dutch for a browser language the app does not support", async () => {
    const translateService = await initializeWithBrowserLanguage("de");

    expect(translateService.getCurrentLang()).toBe("nl");
  });

  it("starts the app in Dutch when the browser language is unknown", async () => {
    const translateService = await initializeWithBrowserLanguage(undefined);

    expect(translateService.getCurrentLang()).toBe("nl");
  });

  it("falls back to Dutch for a text that has no translation", async () => {
    const translateService = await initializeWithBrowserLanguage("en");

    expect(translateService.getFallbackLang()).toBe("nl");
  });
});

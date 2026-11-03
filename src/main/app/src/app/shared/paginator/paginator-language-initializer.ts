/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { LOCATION_INITIALIZED } from "@angular/common";
import { Injector } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";

export function paginatorLanguageInitializerFactory(
  translateService: TranslateService,
  injector: Injector,
) {
  return () =>
    new Promise((resolve) => {
      const locationInitialized = injector.get(
        LOCATION_INITIALIZED,
        Promise.resolve(null),
      );
      locationInitialized.then(() => {
        translateService.addLangs(["nl", "en"]);
        translateService.setFallbackLang("nl");
        const browserLanguage = translateService.getBrowserLang();
        translateService
          .use(browserLanguage?.match(/nl|en/) ? browserLanguage : "nl")
          .subscribe(() => {
            resolve(null);
          });
      });
    });
}

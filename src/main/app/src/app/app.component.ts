/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject } from "@angular/core";
import { Title } from "@angular/platform-browser";
import { TranslateService } from "@ngx-translate/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { FontLoaderService } from "./core/font-loader.service";
import { FontPreloadInjectorService } from "./core/font-preload-injector.service";
import { IdentityService } from "./identity/identity.service";

@Component({
  selector: "zac-root",
  templateUrl: "./app.component.html",
  styleUrls: ["./app.component.less"],
})
export class AppComponent {
  private readonly queryClient = inject(QueryClient);
  private readonly identityService = inject(IdentityService);
  private readonly translateService = inject(TranslateService);
  private readonly titleService = inject(Title);
  private readonly fontLoaderService = inject(FontLoaderService);
  private readonly fontPreloadInjectorService = inject(
    FontPreloadInjectorService,
  );

  constructor() {
    this.titleService.setTitle("Zaakafhandelcomponent");
    this.translateService.addLangs(["nl", "en"]);
    this.translateService.setFallbackLang("nl");
    const browserLanguage = this.translateService.getBrowserLang();
    this.translateService.use(
      browserLanguage?.match(/nl|en/) ? browserLanguage : "nl",
    );

    void this.queryClient.removeQueries({
      queryKey: this.identityService.readLoggedInUser().queryKey,
    });

    // Inject font preloads and load fonts with cache busting
    this.fontPreloadInjectorService.injectFontPreloads();
    this.fontLoaderService.loadFonts();
  }
}

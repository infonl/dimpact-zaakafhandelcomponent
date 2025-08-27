/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AfterViewInit, Component, OnInit } from "@angular/core";
import { Title } from "@angular/platform-browser";
import { TranslateService } from "@ngx-translate/core";
import { UtilService } from "./core/service/util.service";
import { FontLoaderService } from "./core/font-loader.service";
import { FontPreloadInjectorService } from "./core/font-preload-injector.service";
import { IdentityService } from "./identity/identity.service";
import { SessionStorageUtil } from "./shared/storage/session-storage.util";

@Component({
  selector: "zac-root",
  templateUrl: "./app.component.html",
  styleUrls: ["./app.component.less"],
})
export class AppComponent implements OnInit, AfterViewInit {
  initialized = false;

  constructor(
    private readonly translate: TranslateService,
    private readonly titleService: Title,
    public readonly utilService: UtilService,
    private readonly fontLoaderService: FontLoaderService,
    private readonly fontPreloadInjectorService: FontPreloadInjectorService
  ) {}

  ngOnInit() {
    this.titleService.setTitle("Zaakafhandelcomponent");
    this.translate.addLangs(["nl", "en"]);
    this.translate.setFallbackLang("nl");
    const browserLanguage = this.translate.getBrowserLang();
    this.translate.use(
      browserLanguage?.match(/nl|en/) ? browserLanguage : "nl"
    );
    SessionStorageUtil.removeItem(IdentityService.LOGGED_IN_USER_KEY);

    // Inject font preloads and load fonts with cache busting
    this.fontPreloadInjectorService.injectFontPreloads();
    this.fontLoaderService.loadFonts();
  }

  ngAfterViewInit() {
    if (!this.initialized) {
      this.initialized = true;
    }
  }
}

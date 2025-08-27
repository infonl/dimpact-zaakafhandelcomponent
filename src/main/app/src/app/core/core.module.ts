/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { registerLocaleData } from "@angular/common";
import {
  provideHttpClient,
  withInterceptorsFromDi,
  HttpClient,
} from "@angular/common/http";
import localeNl from "@angular/common/locales/nl";
import { LOCALE_ID, NgModule, Optional, SkipSelf } from "@angular/core";
import { MAT_DATE_LOCALE } from "@angular/material/core";
import {
  MAT_DIALOG_DEFAULT_OPTIONS,
  MatDialogConfig,
} from "@angular/material/dialog";
import { TranslateModule, TranslateLoader } from "@ngx-translate/core";
import { SharedModule } from "../shared/shared.module";
import { EnsureModuleLoadedOnceGuard } from "./ensure-module-loaded-once.guard";
import { LoadingComponent } from "./loading/loading.component";
import { UtilService } from "./service/util.service";
import { createCacheBustingTranslateLoader } from "./translate-loader.service";

registerLocaleData(localeNl, "nl-NL");

@NgModule({
  declarations: [LoadingComponent],
  exports: [LoadingComponent],
  imports: [
    TranslateModule.forRoot({
      fallbackLang: "nl",
      loader: {
        provide: TranslateLoader,
        useFactory: createCacheBustingTranslateLoader,
        deps: [HttpClient],
      },
    }),
    SharedModule,
  ],
  providers: [
    UtilService,
    { provide: LOCALE_ID, useValue: "nl-NL" },
    { provide: MAT_DATE_LOCALE, useValue: "nl-NL" },
    {
      provide: MAT_DIALOG_DEFAULT_OPTIONS,
      useValue: {
        ...new MatDialogConfig(),
        width: "650px",
        autoFocus: "dialog",
      },
    },
    provideHttpClient(withInterceptorsFromDi()),
  ],
})
export class CoreModule extends EnsureModuleLoadedOnceGuard {
  // Ensure that CoreModule is only loaded into AppModule

  // Looks for the module in the parent injector to see if it's already been loaded (only want it loaded once)
  constructor(@Optional() @SkipSelf() parentModule: CoreModule) {
    super(parentModule);
  }
}

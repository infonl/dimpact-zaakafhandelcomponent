/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { registerLocaleData } from "@angular/common";
import {
  HttpClient,
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import localeNl from "@angular/common/locales/nl";
import {
  inject,
  Injector,
  LOCALE_ID,
  NgModule,
  Optional,
  provideAppInitializer,
  SkipSelf,
} from "@angular/core";
import { MAT_DATE_LOCALE } from "@angular/material/core";
import {
  MAT_DIALOG_DEFAULT_OPTIONS,
  MatDialogConfig,
} from "@angular/material/dialog";
import { MatPaginatorIntl } from "@angular/material/paginator";
import { MAT_SNACK_BAR_DEFAULT_OPTIONS } from "@angular/material/snack-bar";
import {
  TranslateLoader,
  TranslateModule,
  TranslateService,
} from "@ngx-translate/core";
import { provideZacDateAdapter } from "../shared/form/date/provide-zac-date-adapter";
import { paginatorLanguageInitializerFactory } from "../shared/paginator/paginator-language-initializer";
import { PaginatorTranslator } from "../shared/paginator/paginator-translator";
import { EnsureModuleLoadedOnceGuard } from "./ensure-module-loaded-once.guard";
import { LoadingComponent } from "./loading/loading.component";
import { UtilService } from "./service/util.service";
import { createCacheBustingTranslateLoader } from "./translate-loader.service";

registerLocaleData(localeNl, "nl-NL");

@NgModule({
  imports: [
    LoadingComponent,
    TranslateModule.forRoot({
      fallbackLang: "nl",
      loader: {
        provide: TranslateLoader,
        useFactory: createCacheBustingTranslateLoader,
        deps: [HttpClient],
      },
    }),
  ],
  exports: [LoadingComponent],
  providers: [
    UtilService,
    { provide: LOCALE_ID, useValue: "nl-NL" },
    { provide: MAT_DATE_LOCALE, useValue: "nl-NL" },
    provideZacDateAdapter(),
    {
      provide: MAT_DIALOG_DEFAULT_OPTIONS,
      useValue: {
        ...new MatDialogConfig(),
        width: "650px",
        autoFocus: "dialog",
      },
    },
    {
      provide: MAT_SNACK_BAR_DEFAULT_OPTIONS,
      useValue: { verticalPosition: "top" },
    },
    provideHttpClient(withInterceptorsFromDi()),
    {
      provide: MatPaginatorIntl,
      deps: [TranslateService],
      useFactory: (translateService: TranslateService) =>
        new PaginatorTranslator(translateService).getTranslatedPaginator(),
    },
    provideAppInitializer(() => {
      const initializerFn = paginatorLanguageInitializerFactory(
        inject(TranslateService),
        inject(Injector),
      );
      return initializerFn();
    }),
  ],
})
export class CoreModule extends EnsureModuleLoadedOnceGuard {
  // Ensure that CoreModule is only loaded into AppModule

  // Looks for the module in the parent injector to see if it's already been loaded (only want it loaded once)
  constructor(@Optional() @SkipSelf() parentModule: CoreModule) {
    super(parentModule);
  }
}

/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  APP_BASE_HREF,
  LocationStrategy,
  PathLocationStrategy,
} from "@angular/common";
import { HttpClient, provideHttpClient } from "@angular/common/http";
import {
  ApplicationConfig,
  inject,
  Injector,
  isDevMode,
  LOCALE_ID,
  provideAppInitializer,
  provideEnvironmentInitializer,
} from "@angular/core";
import { MAT_DATE_LOCALE } from "@angular/material/core";
import {
  MAT_DIALOG_DEFAULT_OPTIONS,
  MatDialogConfig,
} from "@angular/material/dialog";
import { MatIconRegistry } from "@angular/material/icon";
import { MatPaginatorIntl } from "@angular/material/paginator";
import { MAT_SNACK_BAR_DEFAULT_OPTIONS } from "@angular/material/snack-bar";
import { provideAnimations } from "@angular/platform-browser/animations";
import { provideRouter, RouteReuseStrategy } from "@angular/router";
import {
  provideTranslateService,
  TranslateLoader,
  TranslateService,
} from "@ngx-translate/core";
import {
  provideTanStackQuery,
  QueryClient,
} from "@tanstack/angular-query-experimental";
import { withDevtools } from "@tanstack/angular-query-experimental/devtools";
import { createAsyncStoragePersister } from "@tanstack/query-async-storage-persister";
import { persistQueryClient } from "@tanstack/query-persist-client-core";
import { APP_ROUTES } from "./app.routes";
import { UtilService } from "./core/service/util.service";
import { provideStartupPrefetch } from "./core/startup-prefetch";
import { createCacheBustingTranslateLoader } from "./core/translate-loader.service";
import { RouteReuseStrategyService } from "./informatie-objecten/route-reuse-strategy.service";
import { provideZacDateAdapter } from "./shared/form/date/provide-zac-date-adapter";
import { QUERY_CLIENT } from "./shared/http/query-client";
import { shouldDehydrateQuery } from "./shared/http/should-dehydrate-query";
import { paginatorLanguageInitializerFactory } from "./shared/paginator/paginator-language-initializer";
import { PaginatorTranslator } from "./shared/paginator/paginator-translator";

export const appConfig: ApplicationConfig = {
  providers: [
    provideAnimations(),
    provideHttpClient(),
    provideRouter(APP_ROUTES),
    provideTranslateService({
      fallbackLang: "nl",
      loader: {
        provide: TranslateLoader,
        useFactory: createCacheBustingTranslateLoader,
        deps: [HttpClient],
      },
    }),
    { provide: APP_BASE_HREF, useValue: "/" },
    { provide: LocationStrategy, useClass: PathLocationStrategy },
    { provide: RouteReuseStrategy, useClass: RouteReuseStrategyService },
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
    provideTanStackQuery(
      QUERY_CLIENT,
      withDevtools(() => ({
        loadDevtools: isDevMode(),
      })),
    ),
    provideStartupPrefetch(),
    provideEnvironmentInitializer(() => {
      inject(MatIconRegistry).setDefaultFontSetClass(
        "material-symbols-outlined",
      );

      const queryClient = inject(QueryClient);
      // https://tanstack.com/query/latest/docs/framework/angular/devtools
      // @ts-expect-error -- window object extension
      window.__TANSTACK_QUERY_CLIENT__ = queryClient;

      persistQueryClient({
        queryClient,
        persister: createAsyncStoragePersister({
          storage: window.sessionStorage,
          key: "zac:tanstack:query",
        }),
        dehydrateOptions: { shouldDehydrateQuery },
      });
    }),
  ],
};

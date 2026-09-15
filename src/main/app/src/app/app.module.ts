/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { Injector, isDevMode, NgModule } from "@angular/core";

import {
  APP_BASE_HREF,
  LocationStrategy,
  PathLocationStrategy,
} from "@angular/common";
import { MatIconRegistry } from "@angular/material/icon";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import {
  provideTanStackQuery,
  QueryClient,
} from "@tanstack/angular-query-experimental";
import { withDevtools } from "@tanstack/angular-query-experimental/devtools";
import { createAsyncStoragePersister } from "@tanstack/query-async-storage-persister";
import { persistQueryClient } from "@tanstack/query-persist-client-core";
import { RouteReuseStrategy } from "@angular/router";
import { AppRoutingModule } from "./app-routing.module";
import { AppComponent } from "./app.component";
import { CoreModule } from "./core/core.module";
import { ToolbarComponent } from "./core/toolbar/toolbar.component";
import { RouteReuseStrategyService } from "./informatie-objecten/route-reuse-strategy.service";
import { Paths } from "./shared/http/http-client";
import { QUERY_CLIENT } from "./shared/http/query-client";
import { SharedModule } from "./shared/shared.module";
import { ZoekComponent } from "./zoeken/zoek/zoek.component";

@NgModule({
  declarations: [AppComponent],
  bootstrap: [AppComponent],
  imports: [
    ToolbarComponent,
    BrowserAnimationsModule,
    CoreModule,
    SharedModule,
    ZoekComponent,
    AppRoutingModule,
  ],
  providers: [
    { provide: APP_BASE_HREF, useValue: "/" },
    { provide: LocationStrategy, useClass: PathLocationStrategy },
    { provide: RouteReuseStrategy, useClass: RouteReuseStrategyService },
    provideTanStackQuery(
      QUERY_CLIENT,
      withDevtools(() => ({
        loadDevtools: isDevMode(),
      })),
    ),
    provideHttpClient(withInterceptorsFromDi()),
  ],
})
export class AppModule {
  static injector: Injector;

  constructor(
    injector: Injector,
    iconRegistry: MatIconRegistry,
    queryClient: QueryClient,
  ) {
    AppModule.injector = injector;
    iconRegistry.setDefaultFontSetClass("material-symbols-outlined");

    // https://tanstack.com/query/latest/docs/framework/angular/devtools
    // @ts-expect-error -- window object extension
    window.__TANSTACK_QUERY_CLIENT__ = queryClient;

    persistQueryClient({
      queryClient,
      persister: createAsyncStoragePersister({
        storage: window.sessionStorage,
        key: "zac:tanstack:query",
      }),
      dehydrateOptions: {
        shouldDehydrateQuery: ({ queryKey }) => {
          const [url] = queryKey;
          if (!url) return false;

          const sessionStoragePersistedEndpoints: (keyof Paths)[] = [
            "/rest/identity/loggedInUser",
          ];
          return sessionStoragePersistedEndpoints.includes(
            String(url) as keyof Paths,
          );
        },
      },
    });
  }
}

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
import {
  provideTanStackQuery,
  QueryClient,
} from "@tanstack/angular-query-experimental";
import { withDevtools } from "@tanstack/angular-query-experimental/devtools";
import { createAsyncStoragePersister } from "@tanstack/query-async-storage-persister";
import { persistQueryClient } from "@tanstack/query-persist-client-core";
import { AdminModule } from "./admin/admin.module";
import { AppRoutingModule } from "./app-routing.module";
import { AppComponent } from "./app.component";
import { CoreModule } from "./core/core.module";
import { ToolbarComponent } from "./core/toolbar/toolbar.component";
import { DashboardModule } from "./dashboard/dashboard.module";
import { DocumentenModule } from "./documenten/documenten.module";
import { FoutAfhandelingModule } from "./fout-afhandeling/fout-afhandeling.module";
import { GebruikersvoorkeurenModule } from "./gebruikersvoorkeuren/gebruikersvoorkeuren.module";
import { IdentityModule } from "./identity/identity.module";
import { InformatieObjectenModule } from "./informatie-objecten/informatie-objecten.module";
import { MailModule } from "./mail/mail.module";
import { PlanItemsModule } from "./plan-items/plan-items.module";
import { ProductaanvragenModule } from "./productaanvragen/productaanvragen.module";
import { Paths } from "./shared/http/http-client";
import { SharedModule } from "./shared/shared.module";
import { SignaleringenModule } from "./signaleringen/signaleringen.module";
import { TakenModule } from "./taken/taken.module";
import { ZakenModule } from "./zaken/zaken.module";
import { ZoekenModule } from "./zoeken/zoeken.module";
import {bootstrapApplication} from "@angular/platform-browser";

const queryClient = new QueryClient();

// https://tanstack.com/query/latest/docs/framework/angular/devtools
// @ts-expect-error -- window object extension
window.__TANSTACK_QUERY_CLIENT__ = queryClient;

@NgModule({
  exports: [ToolbarComponent],
  imports: [
    AppComponent, ToolbarComponent,
    CoreModule,
    SharedModule,
    DashboardModule,
    FoutAfhandelingModule,
    ZakenModule,
    ZoekenModule,
    InformatieObjectenModule,
    DocumentenModule,
    MailModule,
    PlanItemsModule,
    ProductaanvragenModule,
    SignaleringenModule,
    TakenModule,
    IdentityModule,
    AdminModule,
    GebruikersvoorkeurenModule,
    AppRoutingModule,
  ],
})
export class AppModule {
  static injector: Injector;

  constructor(injector: Injector, iconRegistry: MatIconRegistry) {
    AppModule.injector = injector;
    iconRegistry.setDefaultFontSetClass("material-symbols-outlined");

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

void bootstrapApplication(AppModule, {
  providers: [
    { provide: APP_BASE_HREF, useValue: "/" },
    { provide: LocationStrategy, useClass: PathLocationStrategy },
    provideTanStackQuery(
        queryClient,
        withDevtools(() => ({
          loadDevtools: isDevMode(),
        })),
    ),
    provideHttpClient(withInterceptorsFromDi()),
  ],
})

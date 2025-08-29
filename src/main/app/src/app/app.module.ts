/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { Injector, NgModule } from "@angular/core";

import {
  APP_BASE_HREF,
  LocationStrategy,
  PathLocationStrategy,
} from "@angular/common";
import { MatIconRegistry } from "@angular/material/icon";
import {
  QueryClient,
  provideTanStackQuery,
} from "@tanstack/angular-query-experimental";
import { AdminModule } from "./admin/admin.module";
import { AppRoutingModule } from "./app-routing.module";
import { AppComponent } from "./app.component";
import { CoreModule } from "./core/core.module";
import { ToolbarComponent } from "./core/toolbar/toolbar.component";
import { DashboardModule } from "./dashboard/dashboard.module";
import { DocumentenModule } from "./documenten/documenten.module";
import { FoutAfhandelingModule } from "./fout-afhandeling/fout-afhandeling.module";
import { GebruikersvoorkeurenModule } from "./gebruikersvoorkeuren/gebruikersvoorkeuren.module";
import { InformatieObjectenModule } from "./informatie-objecten/informatie-objecten.module";
import { MailModule } from "./mail/mail.module";
import { PlanItemsModule } from "./plan-items/plan-items.module";
import { ProductaanvragenModule } from "./productaanvragen/productaanvragen.module";
import { SharedModule } from "./shared/shared.module";
import { SignaleringenModule } from "./signaleringen/signaleringen.module";
import { TakenModule } from "./taken/taken.module";
import { ZakenModule } from "./zaken/zaken.module";
import { ZoekenModule } from "./zoeken/zoeken.module";

@NgModule({
  declarations: [AppComponent, ToolbarComponent],
  exports: [ToolbarComponent],
  bootstrap: [AppComponent],
  imports: [
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
    AdminModule,
    GebruikersvoorkeurenModule,
    AppRoutingModule,
  ],
  providers: [
    { provide: APP_BASE_HREF, useValue: "/" },
    { provide: LocationStrategy, useClass: PathLocationStrategy },
    provideTanStackQuery(new QueryClient()),
    provideHttpClient(withInterceptorsFromDi()),
  ],
})
export class AppModule {
  static injector: Injector;

  constructor(injector: Injector, iconRegistry: MatIconRegistry) {
    AppModule.injector = injector;
    iconRegistry.setDefaultFontSetClass("material-symbols-outlined");
  }
}

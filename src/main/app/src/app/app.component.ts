/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ChangeDetectionStrategy, Component, inject } from "@angular/core";
import { MatProgressBar } from "@angular/material/progress-bar";
import { MatSidenavModule } from "@angular/material/sidenav";
import { RouterOutlet } from "@angular/router";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { BreadcrumbsComponent } from "./core/breadcrumbs/breadcrumbs.component";
import { LoadingComponent } from "./core/loading/loading.component";
import { ToolbarComponent } from "./core/toolbar/toolbar.component";
import { IdentityService } from "./identity/identity.service";
import { ZoekComponent } from "./zoeken/zoek/zoek.component";

@Component({
  selector: "zac-root",
  templateUrl: "./app.component.html",
  styleUrls: ["./app.component.less"],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    MatProgressBar,
    MatSidenavModule,
    ToolbarComponent,
    BreadcrumbsComponent,
    ZoekComponent,
    LoadingComponent,
    RouterOutlet,
  ],
})
export class AppComponent {
  private readonly queryClient = inject(QueryClient);
  private readonly identityService = inject(IdentityService);

  constructor() {
    void this.queryClient.removeQueries({
      queryKey: this.identityService.readLoggedInUser().queryKey,
    });
  }
}

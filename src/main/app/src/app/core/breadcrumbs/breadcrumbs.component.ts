/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject } from "@angular/core";
import { toSignal } from "@angular/core/rxjs-interop";
import { NavigationEnd, Router } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import { filter, map } from "rxjs";
import { breadcrumbTitles } from "./route-title";

@Component({
  selector: "zac-breadcrumbs",
  templateUrl: "./breadcrumbs.component.html",
  styleUrls: ["./breadcrumbs.component.less"],
  standalone: true,
  imports: [TranslatePipe],
})
export class BreadcrumbsComponent {
  private readonly router = inject(Router);

  protected readonly titles = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map(() => breadcrumbTitles(this.router.routerState.snapshot.root)),
    ),
    { initialValue: breadcrumbTitles(this.router.routerState.snapshot.root) },
  );
}

/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Location } from "@angular/common";
import { Component, inject } from "@angular/core";
import { toSignal } from "@angular/core/rxjs-interop";
import { NavigationEnd, Router, RouterLink } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import { filter, map } from "rxjs";
import { navigationTrail } from "../../shared/navigation/navigation-origin";

@Component({
  selector: "zac-breadcrumbs",
  templateUrl: "./breadcrumbs.component.html",
  styleUrls: ["./breadcrumbs.component.less"],
  standalone: true,
  imports: [TranslatePipe, RouterLink],
})
export class BreadcrumbsComponent {
  private readonly router = inject(Router);
  private readonly location = inject(Location);

  protected readonly breadcrumbs = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map(() =>
        navigationTrail(this.location).map(({ title, url }) => ({
          title,
          link: this.router.parseUrl(url),
        })),
      ),
    ),
    { initialValue: [] },
  );
}

/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Location, NgTemplateOutlet } from "@angular/common";
import { Component, DestroyRef, inject, signal } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { ActivatedRouteSnapshot, NavigationEnd, Router } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { filter } from "rxjs";
import {
  continueTrail,
  navigateToBreadcrumb,
  NavigationState,
  openedFromSearchState,
} from "../../shared/navigation/navigation-origin";
import { ZaakIdentificatieComponent } from "../../shared/zaak-identificatie/zaak-identificatie.component";
import { ZakenService } from "../../zaken/zaken.service";
import { Breadcrumb, breadcrumbKind, breadcrumbs } from "./route-title";

@Component({
  selector: "zac-breadcrumbs",
  templateUrl: "./breadcrumbs.component.html",
  styleUrls: ["./breadcrumbs.component.less"],
  standalone: true,
  imports: [NgTemplateOutlet, TranslatePipe, ZaakIdentificatieComponent],
})
export class BreadcrumbsComponent {
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly queryClient = inject(QueryClient);
  private readonly zakenService = inject(ZakenService);
  private originTrail: Breadcrumb[] | null = null;

  protected readonly breadcrumbs = signal<Breadcrumb[]>([]);

  constructor() {
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.breadcrumbs.set(this.trail()));

    const unsubscribeFromQueryCache = this.queryClient
      .getQueryCache()
      .subscribe((event) => {
        if (event.type === "updated" && event.action.type === "success")
          this.updateZaken();
      });
    inject(DestroyRef).onDestroy(unsubscribeFromQueryCache);
  }

  protected navigate(event: MouseEvent, breadcrumb: Breadcrumb) {
    if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey)
      return;
    event.preventDefault();
    void navigateToBreadcrumb(breadcrumb, this.router);
  }

  private trail() {
    const root = this.router.routerState.snapshot.root;
    const kind = breadcrumbKind(root);
    const state = this.location.getState() as NavigationState;
    const trail = this.withCurrentZaken(
      state?.navigationTrail ?? this.startTrail(root, kind, state),
      breadcrumbs(root),
    );
    this.saveTrail(trail);
    this.originTrail = kind ? trail : null;
    return trail;
  }

  private startTrail(
    root: ActivatedRouteSnapshot,
    kind: ReturnType<typeof breadcrumbKind>,
    state: NavigationState,
  ) {
    const routeTrail = breadcrumbs(root);
    const routePage = routeTrail.pop();
    if (!routePage) return [];

    const page = {
      ...routePage,
      url: this.router.url,
      key: window.navigation.currentEntry?.key,
    };
    return kind === "detail" &&
      this.originTrail &&
      state?.origin !== openedFromSearchState.origin
      ? continueTrail(this.originTrail, page)
      : [...routeTrail, page];
  }

  private updateZaken() {
    const trail = this.withCurrentZaken(this.breadcrumbs());
    if (trail.every((breadcrumb, index) => breadcrumb === this.breadcrumbs()[index]))
      return;

    this.saveTrail(trail);
    if (this.originTrail) this.originTrail = trail;
    this.breadcrumbs.set(trail);
  }

  private withCurrentZaken(trail: Breadcrumb[], resolvedTrail: Breadcrumb[] = []) {
    return trail.map((breadcrumb) => {
      const { zaakUuid } = breadcrumb;
      if (!zaakUuid) return breadcrumb;

      const zaak =
        resolvedTrail.find((resolved) => resolved.zaakUuid === zaakUuid) ??
        this.queryClient.getQueryData(
          this.zakenService.readZaakQuery(zaakUuid).queryKey,
        );
      if (
        !zaak ||
        zaak.isZaakspecifiekGeautoriseerd ===
          breadcrumb.isZaakspecifiekGeautoriseerd
      )
        return breadcrumb;

      return {
        ...breadcrumb,
        isZaakspecifiekGeautoriseerd: zaak.isZaakspecifiekGeautoriseerd,
      };
    });
  }

  // The browser history keeps the trail with its page, for a reload and for back and forward.
  private saveTrail(trail: Breadcrumb[]) {
    this.location.replaceState(this.location.path(), "", {
      ...(this.location.getState() as NavigationState),
      navigationTrail: trail,
    });
  }
}

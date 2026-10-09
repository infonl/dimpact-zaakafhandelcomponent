/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Location } from "@angular/common";
import { Injectable } from "@angular/core";
import {
  NavigationCancel,
  NavigationEnd,
  NavigationError,
  NavigationStart,
  Router,
} from "@angular/router";
import { BehaviorSubject, Observable } from "rxjs";
import { filter } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
import { SessionStorageUtil } from "../storage/session-storage.util";

export type NavigationOrigin = "zoeken";

export const navigationOriginState = (origin: NavigationOrigin) => ({
  origin,
});

type HistoryEntry = { url: string; origin?: NavigationOrigin };

const toPath = (url: string) => url.split(/[?#]/)[0];

@Injectable({
  providedIn: "root",
})
export class NavigationService {
  private static NAVIGATION_HISTORY = "navigationHistory";
  private backDisabled: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(
    false,
  );
  public backDisabled$: Observable<boolean> = this.backDisabled.asObservable();

  constructor(
    private router: Router,
    private location: Location,
    private utilService: UtilService,
  ) {
    router.events
      .pipe(
        filter(
          (e) =>
            e instanceof NavigationStart ||
            e instanceof NavigationEnd ||
            e instanceof NavigationCancel ||
            e instanceof NavigationError,
        ),
      )
      .subscribe((e) => this.handleRouterEvents(e));
  }

  private handleRouterEvents(e: unknown): void {
    switch (true) {
      case e instanceof NavigationStart:
        this.utilService.setLoading(true);
        return;

      case e instanceof NavigationError &&
        this.router.routerState.snapshot.url === "":
        // on a full browser navigation, if a route resolver throws,
        // Angular by default redirects to the root url.
        // we want to override this behaviour so the target url remains in the address bar.
        window.history.replaceState(null, "", e.url);
        break;

      case e instanceof NavigationEnd: {
        const history = this.readHistory();
        if (
          history.length === 0 ||
          history[history.length - 1].url !== e.urlAfterRedirects
        ) {
          history.push({
            url: e.urlAfterRedirects,
            origin: this.router.lastSuccessfulNavigation?.extras.state?.[
              "origin"
            ] as NavigationOrigin | undefined,
          });
        }
        this.writeHistory(history);
        break;
      }
    }

    this.utilService.setLoading(false);
  }

  back(): void {
    const history = this.readHistory();
    history.pop();
    if (history.length > 0) {
      this.location.back();
    } else {
      this.router.navigate([".."]);
    }
    this.writeHistory(history);
  }

  /**
   * Leaves the pages whose path `isLeaving` matches and goes back to the page the user opened
   * them from. The search panel overlays whatever page is open, so that page is not where
   * the user came from: pages opened from search, or opened without a page before them,
   * go to `fallbackUrl` instead.
   */
  returnToOrigin(isLeaving: (path: string) => boolean, fallbackUrl: string) {
    const history = this.readHistory();
    let openedFromSearch = false;
    while (
      history.length > 0 &&
      isLeaving(toPath(history[history.length - 1].url))
    ) {
      openedFromSearch ||= history.pop()?.origin === "zoeken";
    }
    const origin = history.at(-1);
    this.writeHistory(history);

    return this.router.navigateByUrl(
      origin && !openedFromSearch ? origin.url : fallbackUrl,
    );
  }

  private readHistory(): HistoryEntry[] {
    return SessionStorageUtil.getItem<(HistoryEntry | string)[]>(
      NavigationService.NAVIGATION_HISTORY,
      [],
    ).map((entry) => (typeof entry === "string" ? { url: entry } : entry));
  }

  private writeHistory(history: HistoryEntry[]) {
    SessionStorageUtil.setItem(NavigationService.NAVIGATION_HISTORY, history);
    this.backDisabled.next(history.length <= 1);
  }
}

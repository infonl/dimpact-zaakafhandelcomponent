/*
 * SPDX-FileCopyrightText: 2021 Atos
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
        const history = SessionStorageUtil.getItem(
          NavigationService.NAVIGATION_HISTORY,
          [],
        );
        if (
          history.length === 0 ||
          history[history.length - 1] !== e.urlAfterRedirects
        ) {
          history.push(e.urlAfterRedirects as never);
        }
        SessionStorageUtil.setItem(
          NavigationService.NAVIGATION_HISTORY,
          history,
        );
        this.backDisabled.next(history.length <= 1);
        break;
      }
    }

    this.utilService.setLoading(false);
  }

  back(): void {
    const history = SessionStorageUtil.getItem(
      NavigationService.NAVIGATION_HISTORY,
      [],
    );
    history.pop();
    if (history.length > 0) {
      this.location.back();
    } else {
      this.router.navigate([".."]);
    }
    SessionStorageUtil.setItem(NavigationService.NAVIGATION_HISTORY, history);
    this.backDisabled.next(history.length <= 1);
  }
}

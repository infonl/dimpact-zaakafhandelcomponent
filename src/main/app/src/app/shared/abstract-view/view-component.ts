/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, OnDestroy, Signal, signal } from "@angular/core";
import { MatDrawerMode, MatSidenavContainer } from "@angular/material/sidenav";
import { Subscription } from "rxjs";
import { SideNavUtil } from "../side-nav/side-nav.util";

@Component({
  template: "",
  standalone: true,
})
export abstract class ViewComponent implements OnDestroy {
  protected abstract readonly sideNavContainer: Signal<MatSidenavContainer>;
  protected subscriptions$: Subscription[] = [];
  protected readonly sideNavMode = signal<MatDrawerMode>(SideNavUtil.getMode());

  protected menuModeChanged(mode: string): void {
    this.sideNavMode.set(mode as MatDrawerMode);
    this.updateMargins();
  }

  ngOnDestroy(): void {
    this.subscriptions$.forEach((subscription$) => subscription$.unsubscribe());
  }

  protected updateMargins(): void {
    setTimeout(() => this.sideNavContainer().updateContentMargins(), 300);
  }
}

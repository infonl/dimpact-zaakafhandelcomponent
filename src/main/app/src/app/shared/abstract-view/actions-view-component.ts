/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AfterViewInit, Component, OnDestroy } from "@angular/core";
import { MatSidenav } from "@angular/material/sidenav";
import { Subject, takeUntil } from "rxjs";
import { ViewComponent } from "./view-component";

@Component({ template: "" })
export abstract class ActionsViewComponent
  extends ViewComponent
  implements AfterViewInit, OnDestroy
{
  abstract actionsSidenav: MatSidenav;
  destroy$ = new Subject<void>();

  protected constructor() {
    super();
  }

  ngAfterViewInit(): void {
    super.ngAfterViewInit();
    this.actionsSidenav.closedStart
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.sideNavContainer.hasBackdrop = false;
      });
    this.actionsSidenav.openedStart
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.sideNavContainer.hasBackdrop = true;
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}

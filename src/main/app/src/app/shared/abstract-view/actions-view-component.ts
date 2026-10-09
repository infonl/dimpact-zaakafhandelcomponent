/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AfterViewInit, Component, Signal } from "@angular/core";
import { MatDrawer } from "@angular/material/sidenav";
import { ViewComponent } from "./view-component";

@Component({
  template: "",
  standalone: true,
})
export abstract class ActionsViewComponent
  extends ViewComponent
  implements AfterViewInit
{
  abstract readonly actionsSidenav: Signal<MatDrawer>;

  protected constructor() {
    super();
  }

  ngAfterViewInit(): void {
    super.ngAfterViewInit();
    this.actionsSidenav().closedStart.subscribe(() => {
      this.sideNavContainer().hasBackdrop = false;
    });
    this.actionsSidenav().openedStart.subscribe(() => {
      this.sideNavContainer().hasBackdrop = true;
    });
  }
}

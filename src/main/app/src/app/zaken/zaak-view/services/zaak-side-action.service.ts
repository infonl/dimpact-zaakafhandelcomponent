/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable, signal } from "@angular/core";
import { MatSidenav } from "@angular/material/sidenav";
import { GeneratedType } from "../../../shared/utils/generated-types";

/**
 * Owns which side action panel the zaak view is showing and the sidenav that
 * renders it. Scoped to a single zaak view, so it is provided by the component
 * rather than in the root injector.
 */
@Injectable()
export class ZaakSideActionService {
  private sidenav?: MatSidenav;

  readonly activeAction = signal<string | null>(null);
  readonly actiefPlanItem = signal<GeneratedType<"RESTPlanItem"> | null>(null);

  register(sidenav: MatSidenav) {
    this.sidenav = sidenav;
  }

  /** Opens the sidenav, optionally switching to another panel first. */
  open(action?: string) {
    if (action !== undefined) this.activeAction.set(action);
    void this.sidenav?.open();
  }

  /** Closes the sidenav without forgetting which panel was showing. */
  close() {
    void this.sidenav?.close();
  }

  /** Forgets the active panel without touching the sidenav itself. */
  clear() {
    this.activeAction.set(null);
  }

  /** Closes the sidenav and forgets both the active panel and the plan item. */
  reset() {
    this.clear();
    this.actiefPlanItem.set(null);
    this.close();
  }
}

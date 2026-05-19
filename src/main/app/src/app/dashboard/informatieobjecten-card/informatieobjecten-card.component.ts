/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, signal } from "@angular/core";
import { finalize } from "rxjs/operators";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { SignaleringenService } from "../../signaleringen.service";
import { DashboardCardComponent } from "../dashboard-card/dashboard-card.component";

@Component({
  selector: "zac-informatieobjecten-card",
  templateUrl: "./informatieobjecten-card.component.html",
  styleUrls: [
    "../dashboard-card/dashboard-card.component.less",
    "./informatieobjecten-card.component.less",
  ],
  standalone: false,
})
export class InformatieobjectenCardComponent extends DashboardCardComponent<
  GeneratedType<"RestEnkelvoudigInformatieobject">
> {
  columns = [
    "titel",
    "registratiedatumTijd",
    "informatieobjectTypeOmschrijving",
    "auteur",
    "url",
  ] as const;

  public readonly isLoading = signal(false);

  constructor(
    private signaleringenService: SignaleringenService,
    protected identityService: IdentityService,
    protected websocketService: WebsocketService,
  ) {
    super(identityService, websocketService);
  }

  protected onLoad(): void {
    const signaleringType = this.data.signaleringType;
    if (!signaleringType) {
      this.dataSource.data = [];
      return;
    }
    this.isLoading.set(true);
    this.signaleringenService
      .listInformatieobjectenSignalering(signaleringType)
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe((informatieobjecten) => {
        this.dataSource.data = informatieobjecten ?? [];
      });
  }
}

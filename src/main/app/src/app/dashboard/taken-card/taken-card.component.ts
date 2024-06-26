/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { SignaleringTaakSummary } from "src/app/signaleringen/model/signalering-taak-summary";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { SignaleringenService } from "../../signaleringen.service";
import { DashboardCardComponent } from "../dashboard-card/dashboard-card.component";

@Component({
  selector: "zac-taken-card",
  templateUrl: "./taken-card.component.html",
  styleUrls: [
    "../dashboard-card/dashboard-card.component.less",
    "./taken-card.component.less",
  ],
})
export class TakenCardComponent extends DashboardCardComponent<SignaleringTaakSummary> {
  columns: string[] = [
    "naam",
    "creatiedatumTijd",
    "zaakIdentificatie",
    "zaaktypeOmschrijving",
    "url",
  ];

  constructor(
    private signaleringenService: SignaleringenService,
    protected identityService: IdentityService,
    protected websocketService: WebsocketService,
  ) {
    super(identityService, websocketService);
  }

  protected onLoad(afterLoad: () => void): void {
    this.signaleringenService
      .listTakenSignalering(this.data.signaleringType)
      .subscribe((taken) => {
        this.dataSource.data = taken;
        afterLoad();
      });
  }
}

/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
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
})
export class InformatieobjectenCardComponent extends DashboardCardComponent<
  GeneratedType<"RestEnkelvoudigInformatieobject">
> {
  columns: string[] = [
    "titel",
    "registratiedatumTijd",
    "informatieobjectType",
    "auteur",
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
      .listInformatieobjectenSignalering(this.data.signaleringType)
      .subscribe((informatieobjecten) => {
        this.dataSource.data = informatieobjecten;
        afterLoad();
      });
  }
}

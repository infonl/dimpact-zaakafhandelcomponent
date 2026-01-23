/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { DateConditionals } from "../../shared/utils/date-conditionals";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../../zaken/zaken.service";
import { DashboardCardComponent } from "../dashboard-card/dashboard-card.component";

@Component({
  selector: "zac-zaak-waarschuwingen-card",
  templateUrl: "./zaak-waarschuwingen-card.component.html",
  styleUrls: [
    "../dashboard-card/dashboard-card.component.less",
    "./zaak-waarschuwingen-card.component.less",
  ],
})
export class ZaakWaarschuwingenCardComponent extends DashboardCardComponent<
  GeneratedType<"RestZaakOverzicht">
> {
  columns: string[] = [
    "identificatie",
    "streefdatum",
    "dagenTotStreefdatum",
    "fataledatum",
    "dagenTotFataledatum",
    "url",
  ];

  constructor(
    private zakenService: ZakenService,
    protected identityService: IdentityService,
    protected websocketService: WebsocketService,
  ) {
    super(identityService, websocketService);
  }

  isAfterDate(datum, actual): boolean {
    return DateConditionals.isExceeded(datum, actual);
  }

  protected onLoad(afterLoad: () => void): void {
    this.zakenService.listZaakWaarschuwingen().subscribe((zaken) => {
      this.dataSource.data = zaken;
      afterLoad();
    });
  }
}

/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, signal } from "@angular/core";
import { finalize } from "rxjs/operators";
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
  standalone: false,
})
export class ZaakWaarschuwingenCardComponent extends DashboardCardComponent<
  GeneratedType<"RestZaakOverzicht">
> {
  columns = [
    "identificatie",
    "streefdatum",
    "dagenTotStreefdatum",
    "fataledatum",
    "dagenTotFataledatum",
    "url",
  ] as const;

  public readonly isLoading = signal(false);

  constructor(
    private zakenService: ZakenService,
    protected identityService: IdentityService,
    protected websocketService: WebsocketService,
  ) {
    super(identityService, websocketService);
  }

  isAfterDate(
    datum: GeneratedType<"RestZaakOverzicht">["einddatumGepland"],
    actual: GeneratedType<"RestZaakOverzicht">["einddatum"],
  ) {
    return DateConditionals.isExceeded(datum ?? null, actual);
  }

  protected onLoad() {
    this.isLoading.set(true);
    this.zakenService
      .listZaakWaarschuwingen()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe((zaken) => {
        this.dataSource.data = zaken;
      });
  }
}

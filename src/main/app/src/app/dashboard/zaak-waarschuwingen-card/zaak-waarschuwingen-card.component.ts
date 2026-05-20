/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, effect } from "@angular/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { firstValueFrom } from "rxjs";
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

  zakenQuery = injectQuery(() => ({
    queryKey: ["zaak waarschuwingen dashboard"],
    queryFn: () => firstValueFrom(this.zakenService.listZaakWaarschuwingen()),
  }));

  constructor(
    private zakenService: ZakenService,
    protected identityService: IdentityService,
    protected websocketService: WebsocketService,
  ) {
    super(identityService, websocketService);
    effect(() => {
      this.dataSource.data = this.zakenQuery.data() ?? [];
    });
  }

  isAfterDate(
    datum: GeneratedType<"RestZaakOverzicht">["einddatumGepland"],
    actual: GeneratedType<"RestZaakOverzicht">["einddatum"],
  ) {
    return DateConditionals.isExceeded(datum ?? null, actual);
  }

  protected onLoad() {
    this.zakenQuery.refetch();
  }
}

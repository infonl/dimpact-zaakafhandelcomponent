/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, computed, effect, signal } from "@angular/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { firstValueFrom } from "rxjs";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { TakenMijnDatasource } from "../../taken/taken-mijn/taken-mijn-datasource";
import { DEFAULT_ZOEK_PARAMETERS } from "../../zoeken/model/zoek-parameters";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { DashboardCardComponent } from "../dashboard-card/dashboard-card.component";

@Component({
  selector: "zac-taak-zoeken-card",
  templateUrl: "./taak-zoeken-card.component.html",
  styleUrls: [
    "../dashboard-card/dashboard-card.component.less",
    "./taak-zoeken-card.component.less",
  ],
})
export class TaakZoekenCardComponent extends DashboardCardComponent {
  columns = [
    "naam",
    "creatiedatum",
    "zaakIdentificatie",
    "zaaktypeOmschrijving",
    "url",
  ] as const;
  pageSize = 5;
  pageNumber = signal(0);

  zoekParameters = computed(() => {
    const zoekParameters = TakenMijnDatasource.mijnLopendeTaken(
      DEFAULT_ZOEK_PARAMETERS,
    );
    zoekParameters.sorteerVeld = "TAAK_FATALEDATUM";
    zoekParameters.sorteerRichting = "asc";
    zoekParameters.rows = this.pageSize;
    zoekParameters.page = this.pageNumber();
    return zoekParameters;
  });

  zoekQuery = injectQuery(() => ({
    queryKey: ["taak zoeken dashboard", this.zoekParameters()],
    queryFn: () =>
      firstValueFrom(this.zoekenService.list(this.zoekParameters())),
  }));

  constructor(
    private zoekenService: ZoekenService,
    protected identityService: IdentityService,
    protected websocketService: WebsocketService,
  ) {
    super(identityService, websocketService);
    effect(() => {
      const { resultaten = [], totaal = 0 } = this.zoekQuery.data() ?? {};
      this.dataSource.data = resultaten;
      if (this.paginator) this.paginator.length = totaal;
    });
  }

  onPageChange({ pageIndex }: { pageIndex: number }) {
    this.pageNumber.set(pageIndex);
  }

  protected onLoad(): void {
    this.zoekQuery.refetch();
  }
}

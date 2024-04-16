/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, computed, effect, signal } from "@angular/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { firstValueFrom } from "rxjs";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { ZakenMijnDatasource } from "../../zaken/zaken-mijn/zaken-mijn-datasource";
import { SorteerVeld } from "../../zoeken/model/sorteer-veld";
import { ZoekObject } from "../../zoeken/model/zoek-object";
import { ZoekParameters } from "../../zoeken/model/zoek-parameters";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { DashboardCardComponent } from "../dashboard-card/dashboard-card.component";

@Component({
  selector: "zac-zaak-zoeken-card",
  templateUrl: "./zaak-zoeken-card.component.html",
  styleUrls: [
    "../dashboard-card/dashboard-card.component.less",
    "./zaak-zoeken-card.component.less",
  ],
})
export class ZaakZoekenCardComponent extends DashboardCardComponent<ZoekObject> {
  columns: string[] = [
    "identificatie",
    "startdatum",
    "zaaktypeOmschrijving",
    "omschrijving",
    "url",
  ];
  pageSize = 5;
  pageNumber = signal(0);

  zoekParameters = computed(() => {
    const zoekParameters = ZakenMijnDatasource.mijnLopendeZaken(
      new ZoekParameters(),
    );
    zoekParameters.sorteerVeld = SorteerVeld.ZAAK_STREEFDATUM;
    zoekParameters.sorteerRichting = "asc";
    zoekParameters.rows = this.pageSize;
    zoekParameters.page = this.pageNumber();
    return zoekParameters;
  });

  zoekQuery = injectQuery(() => ({
    queryKey: ["zaak zoeken dashboard", this.zoekParameters()],
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
      this.paginator.length = totaal;
    });
  }

  onPageChange({ pageIndex }: { pageIndex: number }) {
    this.pageNumber.set(pageIndex);
  }

  protected onLoad(afterLoad: () => void): void {
    this.zoekQuery.refetch();
  }
}

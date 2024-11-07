/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, computed, effect, OnDestroy, signal } from "@angular/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { firstValueFrom } from "rxjs";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { SignaleringenService } from "../../signaleringen.service";
import { ZaakOverzicht } from "../../zaken/model/zaak-overzicht";
import { DashboardCardComponent } from "../dashboard-card/dashboard-card.component";

@Component({
  selector: "zac-zaken-card",
  templateUrl: "./zaken-card.component.html",
  styleUrls: [
    "../dashboard-card/dashboard-card.component.less",
    "./zaken-card.component.less",
  ],
})
export class ZakenCardComponent
  extends DashboardCardComponent<ZaakOverzicht>
  implements OnDestroy
{
  columns: string[] = [
    "identificatie",
    "startdatum",
    "zaaktype",
    "omschrijving",
    "url",
  ];
  pageSize = 5;
  pageNumber = signal(0);

  parameters = computed(() => {
    return { pageNumber: this.pageNumber(), pageSize: this.pageSize };
  });

  zakenQuery = injectQuery(() => ({
    queryKey: ["aan mij toegekende zaken signaleringen", this.parameters()],
    queryFn: () =>
      firstValueFrom(
        this.signaleringenService.listZakenSignalering({
          signaleringType: this.data.signaleringType,
          pageNumber: this.parameters().pageNumber,
          pageSize: this.parameters().pageSize,
        }),
      ),
  }));

  constructor(
    private signaleringenService: SignaleringenService,
    protected identityService: IdentityService,
    protected websocketService: WebsocketService,
  ) {
    super(identityService, websocketService);

    effect(() => {
      const { zaken = [], total = 0 } = this.zakenQuery.data() ?? {};

      this.dataSource.data = zaken;
      this.paginator.length = total;
    });
  }

  onPageChange({ pageIndex }: { pageIndex: number }) {
    this.pageNumber.set(pageIndex);
  }

  protected onLoad(afterLoad: () => void): void {
    this.zakenQuery.refetch();
  }
}

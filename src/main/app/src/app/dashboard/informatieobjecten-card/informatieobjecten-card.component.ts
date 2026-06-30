/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import { Component, computed, effect } from "@angular/core";
import { MatIconAnchor } from "@angular/material/button";
import { MatIcon } from "@angular/material/icon";
import { MatPaginator } from "@angular/material/paginator";
import { MatProgressSpinner } from "@angular/material/progress-spinner";
import { MatSort, MatSortHeader } from "@angular/material/sort";
import {
  MatCell,
  MatCellDef,
  MatColumnDef,
  MatHeaderCell,
  MatHeaderCellDef,
  MatHeaderRow,
  MatHeaderRowDef,
  MatNoDataRow,
  MatRow,
  MatRowDef,
  MatTable,
} from "@angular/material/table";
import { RouterLink } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { firstValueFrom } from "rxjs";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
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
  standalone: true,
  imports: [
    NgIf,
    MatTable,
    MatSort,
    MatColumnDef,
    MatHeaderCellDef,
    MatHeaderCell,
    MatSortHeader,
    MatCellDef,
    MatCell,
    MatHeaderRowDef,
    MatHeaderRow,
    MatRowDef,
    MatRow,
    MatNoDataRow,
    MatPaginator,
    MatProgressSpinner,
    MatIconAnchor,
    MatIcon,
    RouterLink,
    TranslateModule,
    DatumPipe,
    EmptyPipe,
  ],
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

  parameters = computed(() => ({
    signaleringType: this.data?.signaleringType,
  }));

  ioQuery = injectQuery(() => ({
    queryKey: ["informatieobjecten signaleringen dashboard", this.parameters()],
    enabled: !!this.parameters().signaleringType,
    queryFn: () =>
      firstValueFrom(
        this.signaleringenService.listInformatieobjectenSignalering(
          this.parameters().signaleringType!,
        ),
      ),
  }));

  constructor(
    private signaleringenService: SignaleringenService,
    protected identityService: IdentityService,
    protected websocketService: WebsocketService,
  ) {
    super(identityService, websocketService);

    effect(() => {
      this.dataSource.data = this.ioQuery.data() ?? [];
    });
  }

  protected onLoad(): void {
    if (!this.data?.signaleringType) {
      this.dataSource.data = [];
      return;
    }
    this.ioQuery.refetch();
  }
}

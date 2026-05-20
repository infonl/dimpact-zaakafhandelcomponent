/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, computed, effect } from "@angular/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { firstValueFrom } from "rxjs";
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

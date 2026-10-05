/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { PostBody } from "../shared/http/http-client";
import { mergeMutationOptions } from "../shared/http/merge-mutation-options";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";
import { TakenService } from "../taken/taken.service";

@Injectable({
  providedIn: "root",
})
export class PlanItemsService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly takenService = inject(TakenService);

  readHumanTaskPlanItem(planItemId: string) {
    return this.zacHttpClient.GET("/rest/planitems/humanTaskPlanItem/{id}", {
      path: { id: planItemId },
    });
  }

  listHumanTaskPlanItemsQuery(zaakUuid: string) {
    return this.zacQueryClient.GET(
      "/rest/planitems/zaak/{uuid}/humanTaskPlanItems",
      {
        path: { uuid: zaakUuid },
      },
    );
  }

  listUserEventListenerPlanItemsQuery(zaakUuid: string) {
    return this.zacQueryClient.GET(
      "/rest/planitems/zaak/{uuid}/userEventListenerPlanItems",
      {
        path: { uuid: zaakUuid },
      },
    );
  }

  doHumanTaskPlanItem(zaakUUID: string) {
    return mergeMutationOptions(
      this.zacQueryClient.POST("/rest/planitems/doHumanTaskPlanItem"),
      {
        onSuccess: () =>
          void this.takenService.invalidateTakenVoorZaak(zaakUUID),
      },
    );
  }

  doUserEventListenerPlanItem(
    body: PostBody<"/rest/planitems/doUserEventListenerPlanItem">,
  ) {
    return this.zacHttpClient.POST(
      "/rest/planitems/doUserEventListenerPlanItem",
      body,
    );
  }
}

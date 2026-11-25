/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class PlanItemsService {
  private readonly zacQueryClient = inject(ZacQueryClient);

  readHumanTaskPlanItem(planItemId: string) {
    return this.zacQueryClient.GET("/rest/planitems/humanTaskPlanItem/{id}", {
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

  doHumanTaskPlanItem() {
    return this.zacQueryClient.POST("/rest/planitems/doHumanTaskPlanItem");
  }

  doUserEventListenerPlanItem() {
    return this.zacQueryClient.POST(
      "/rest/planitems/doUserEventListenerPlanItem",
    );
  }
}

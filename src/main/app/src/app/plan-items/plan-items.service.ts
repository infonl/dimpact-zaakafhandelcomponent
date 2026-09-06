/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { PostBody } from "../shared/http/http-client";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class PlanItemsService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);

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

  doHumanTaskPlanItem() {
    return this.zacQueryClient.POST("/rest/planitems/doHumanTaskPlanItem");
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

/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { PostBody, ZacHttpClient } from "../shared/http/zac-http-client";

@Injectable({
  providedIn: "root",
})
export class PlanItemsService {
  private basepath = "/rest/planitems";

  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  readHumanTaskPlanItem(planItemId: string) {
    return this.zacHttpClient.GET("/rest/planitems/humanTaskPlanItem/{id}", {
      path: { id: planItemId },
    });
  }

  readProcessTaskPlanItem(planItemId: string) {
    return this.zacHttpClient.GET("/rest/planitems/processTaskPlanItem/{id}", {
      path: { id: planItemId },
    });
  }

  listHumanTaskPlanItems(zaakUuid: string) {
    return this.zacHttpClient.GET(
      "/rest/planitems/zaak/{uuid}/humanTaskPlanItems",
      {
        path: { uuid: zaakUuid },
      },
    );
  }

  listProcessTaskPlanItems(zaakUuid: string) {
    return this.zacHttpClient.GET(
      "/rest/planitems/zaak/{uuid}/processTaskPlanItems",
      {
        path: { uuid: zaakUuid },
      },
    );
  }

  listUserEventListenerPlanItems(zaakUuid: string) {
    return this.zacHttpClient.GET(
      "/rest/planitems/zaak/{uuid}/userEventListenerPlanItems",
      {
        path: { uuid: zaakUuid },
      },
    );
  }

  doHumanTaskPlanItem(
    humanTaskData: PostBody<"/rest/planitems/doHumanTaskPlanItem">,
  ) {
    if (!humanTaskData.medewerker?.id) {
      humanTaskData.medewerker = null;
    }

    return this.zacHttpClient.POST(
      "/rest/planitems/doHumanTaskPlanItem",
      humanTaskData,
    );
  }

  doProcessTaskPlanItem(
    body: PostBody<"/rest/planitems/doProcessTaskPlanItem">,
  ) {
    return this.zacHttpClient.POST(
      "/rest/planitems/doProcessTaskPlanItem",
      body,
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

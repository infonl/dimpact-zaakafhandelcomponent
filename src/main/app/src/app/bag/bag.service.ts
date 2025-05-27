/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import {
  DeleteBody,
  PostBody,
  PutBody,
  ZacHttpClient,
} from "../shared/http/zac-http-client";
import { GeneratedType } from "../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class BAGService {
  constructor(private readonly zacHtppClient: ZacHttpClient) {}

  listAdressen(body: PutBody<"/rest/bag/adres">) {
    return this.zacHtppClient.PUT("/rest/bag/adres", body, {});
  }

  create(body: PostBody<"/rest/bag">) {
    return this.zacHtppClient.POST("/rest/bag", body, {});
  }

  list(zaakUuid: string) {
    return this.zacHtppClient.GET("/rest/bag/zaak/{zaakUuid}", {
      path: { zaakUuid },
    });
  }

  delete(body: DeleteBody<"/rest/bag">) {
    return this.zacHtppClient.DELETE("/rest/bag", {}, body);
  }

  read(type: GeneratedType<"BAGObjectType">, id: string) {
    return this.zacHtppClient.GET("/rest/bag/{type}/{id}", {
      path: { type, id },
    });
  }
}

/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { PutBody } from "../shared/http/http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";
import { GeneratedType } from "../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class BAGService {
  private readonly zacQueryClient = inject(ZacQueryClient);

  listAdressen(body: PutBody<"/rest/bag/adres">) {
    return this.zacQueryClient.PUT_QUERY("/rest/bag/adres", body);
  }

  create() {
    return this.zacQueryClient.POST("/rest/bag");
  }

  list(zaakUuid: string) {
    return this.zacQueryClient.GET("/rest/bag/zaak/{zaakUuid}", {
      path: { zaakUuid },
    });
  }

  delete() {
    return this.zacQueryClient.DELETE("/rest/bag");
  }

  read(type: GeneratedType<"BagObjectType">, id: string) {
    return this.zacQueryClient.GET("/rest/bag/{type}/{id}", {
      path: { type, id },
    });
  }
}

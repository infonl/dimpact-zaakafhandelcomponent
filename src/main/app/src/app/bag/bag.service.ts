/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { PutBody } from "../shared/http/http-client";
import { mergeMutationOptions } from "../shared/http/merge-mutation-options";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";
import { GeneratedType } from "../shared/utils/generated-types";
import { ZakenService } from "../zaken/zaken.service";

@Injectable({
  providedIn: "root",
})
export class BAGService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly zakenService = inject(ZakenService);

  listAdressen(body: PutBody<"/rest/bag/adres">) {
    return this.zacQueryClient.PUT_QUERY("/rest/bag/adres", body);
  }

  create() {
    return mergeMutationOptions(this.zacQueryClient.POST("/rest/bag"), {
      onSuccess: (_data, { zaakUuid }) => {
        if (!zaakUuid) return;
        this.zakenService.invalidateHistorie(zaakUuid);
      },
    });
  }

  list(zaakUuid: string) {
    return this.zacHttpClient.GET("/rest/bag/zaak/{zaakUuid}", {
      path: { zaakUuid },
    });
  }

  delete() {
    return mergeMutationOptions(this.zacQueryClient.DELETE("/rest/bag"), {
      onSuccess: (_data, { zaakUuid }) => {
        if (!zaakUuid) return;
        this.zakenService.invalidateHistorie(zaakUuid);
      },
    });
  }

  read(type: GeneratedType<"BagObjectType">, id: string) {
    return this.zacHttpClient.GET("/rest/bag/{type}/{id}", {
      path: { type, id },
    });
  }
}

/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { PatchBody, PutBody } from "../shared/http/http-client";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class TakenService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);

  readTaak(taskId: string) {
    return this.zacHttpClient.GET("/rest/taken/{taskId}", {
      path: { taskId },
    });
  }

  listTakenVoorZaak(zaakUUID: string) {
    return this.zacHttpClient.GET("/rest/taken/zaak/{zaakUUID}", {
      path: { zaakUUID },
    });
  }

  listHistorieVoorTaak(taskId: string) {
    return this.zacHttpClient.GET("/rest/taken/{taskId}/historie", {
      path: { taskId },
    });
  }

  toekennen(body: PatchBody<"/rest/taken/toekennen">) {
    return this.zacHttpClient.PATCH("/rest/taken/toekennen", body);
  }

  toekennenAanIngelogdeMedewerker(
    body: PatchBody<"/rest/taken/lijst/toekennen/mij">,
  ) {
    return this.zacHttpClient.PATCH("/rest/taken/toekennen/mij", body);
  }

  toekennenAanIngelogdeMedewerkerVanuitLijst(
    body: PatchBody<"/rest/taken/lijst/toekennen/mij">,
  ) {
    return this.zacHttpClient.PATCH("/rest/taken/lijst/toekennen/mij", body);
  }

  updateTaakdata() {
    return this.zacQueryClient.PUT("/rest/taken/taakdata");
  }

  complete() {
    return this.zacQueryClient.PATCH("/rest/taken/complete");
  }

  verdelenVanuitLijst(body: PutBody<"/rest/taken/lijst/verdelen">) {
    return this.zacHttpClient.PUT("/rest/taken/lijst/verdelen", body);
  }

  vrijgevenVanuitLijst(body: PutBody<"/rest/taken/lijst/vrijgeven">) {
    return this.zacHttpClient.PUT("/rest/taken/lijst/vrijgeven", body);
  }
}

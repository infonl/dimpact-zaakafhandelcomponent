/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { UtilService } from "../core/service/util.service";
import { PatchBody } from "../shared/http/http-client";
import { mergeMutationOptions } from "../shared/http/merge-mutation-options";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class TakenService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly utilService = inject(UtilService);

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

  listTakenVoorZaakQuery(zaakUUID: string) {
    return this.zacQueryClient.GET("/rest/taken/zaak/{zaakUUID}", {
      path: { zaakUUID },
    });
  }

  listHistorieVoorTaak(taskId: string) {
    return this.zacHttpClient.GET("/rest/taken/{taskId}/historie", {
      path: { taskId },
    });
  }

  toekennen() {
    return this.zacQueryClient.PATCH("/rest/taken/toekennen");
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
    return mergeMutationOptions(
      this.zacQueryClient.PUT("/rest/taken/taakdata"),
      { onSuccess: () => this.utilService.openSnackbar("msg.taak.opgeslagen") },
    );
  }

  complete() {
    return mergeMutationOptions(
      this.zacQueryClient.PATCH("/rest/taken/complete"),
      { onSuccess: () => this.utilService.openSnackbar("msg.taak.afgerond") },
    );
  }

  verdelenVanuitLijst() {
    return this.zacQueryClient.PUT("/rest/taken/lijst/verdelen");
  }

  vrijgevenVanuitLijst() {
    return this.zacQueryClient.PUT("/rest/taken/lijst/vrijgeven");
  }
}

/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { Router } from "@angular/router";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { UtilService } from "../core/service/util.service";
import { mergeMutationOptions } from "../shared/http/merge-mutation-options";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";
import { NavigationService } from "../shared/navigation/navigation.service";
import { GeneratedType } from "../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class TakenService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly queryClient = inject(QueryClient);
  private readonly utilService = inject(UtilService);
  private readonly router = inject(Router);
  private readonly navigationService = inject(NavigationService);

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

  invalidateTakenVoorZaak(zaakUUID: string) {
    return this.queryClient.invalidateQueries({
      queryKey: this.listTakenVoorZaakQuery(zaakUUID).queryKey,
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

  toekennenAanIngelogdeMedewerker() {
    return mergeMutationOptions(
      this.zacQueryClient.PATCH("/rest/taken/toekennen/mij"),
      { onSuccess: (taak) => this.reportToegekend(taak) },
    );
  }

  toekennenAanIngelogdeMedewerkerVanuitLijst() {
    return mergeMutationOptions(
      this.zacQueryClient.PATCH("/rest/taken/lijst/toekennen/mij"),
      { onSuccess: (taak) => this.reportToegekend(taak) },
    );
  }

  private reportToegekend({ behandelaar }: GeneratedType<"RestTask">) {
    this.utilService.openSnackbar("msg.taak.toegekend", {
      behandelaar: behandelaar?.naam,
    });
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
      {
        onSuccess: (taak) => {
          this.utilService.openSnackbar("msg.taak.afgerond");
          void this.leaveCompletedTaak(taak);
        },
      },
    );
  }

  private leaveCompletedTaak({ id }: GeneratedType<"RestTask">) {
    if (this.router.parseUrl(this.router.url).queryParams["bron"] === "email")
      return;

    return this.navigationService.returnToOrigin(
      (path) => path === `/taken/${id}`,
      "/",
    );
  }

  verdelenVanuitLijst() {
    return this.zacQueryClient.PUT("/rest/taken/lijst/verdelen");
  }

  vrijgevenVanuitLijst() {
    return this.zacQueryClient.PUT("/rest/taken/lijst/vrijgeven");
  }
}

/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { mergeMutationOptions } from "../shared/http/merge-mutation-options";
import { ZacQueryClient } from "../shared/http/zac-query-client";
import { GeneratedType } from "../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class GebruikersvoorkeurenService {
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly queryClient = inject(QueryClient);

  listZoekOpdrachten(lijstID: GeneratedType<"Werklijst">) {
    return {
      ...this.zacQueryClient.GET(
        "/rest/gebruikersvoorkeuren/zoekopdracht/{lijstID}",
        {
          path: { lijstID },
        },
      ),
      meta: { reportFailedRefetch: true },
    };
  }

  createOrUpdateZoekOpdrachten() {
    return this.zacQueryClient.POST("/rest/gebruikersvoorkeuren/zoekopdracht");
  }

  deleteZoekOpdrachten() {
    return this.zacQueryClient.DELETE(
      "/rest/gebruikersvoorkeuren/zoekopdracht/{id}",
      (id: number) => ({ parameters: { path: { id } } }),
    );
  }

  setZoekopdrachtActief() {
    return this.zacQueryClient.PUT(
      "/rest/gebruikersvoorkeuren/zoekopdracht/actief",
    );
  }

  removeZoekopdrachtActief() {
    return this.zacQueryClient.DELETE(
      "/rest/gebruikersvoorkeuren/zoekopdracht/{werklijst}/actief",
      (werklijst: GeneratedType<"Werklijst">) => ({
        parameters: { path: { werklijst } },
      }),
    );
  }

  readTabelGegevens(werklijst: GeneratedType<"Werklijst">) {
    return {
      ...this.zacQueryClient.GET(
        "/rest/gebruikersvoorkeuren/tabel-gegevens/{werklijst}",
        {
          path: { werklijst },
        },
      ),
      meta: { reportFailedRefetch: true },
    };
  }

  updateAantalPerPagina(werklijst: GeneratedType<"Werklijst">, aantal: number) {
    return mergeMutationOptions(
      this.zacQueryClient.PUT(
        "/rest/gebruikersvoorkeuren/aantal-per-pagina/{werklijst}/{aantal}",
        {
          path: { werklijst, aantal },
        },
      ),
      {
        onSuccess: () =>
          this.queryClient.invalidateQueries({
            queryKey: this.readTabelGegevens(werklijst).queryKey,
          }),
      },
    );
  }

  listDashboardCards() {
    return this.zacQueryClient.GET(
      "/rest/gebruikersvoorkeuren/dasboardcard/actief",
    );
  }

  updateDashboardCards() {
    return mergeMutationOptions(
      this.zacQueryClient.PUT("/rest/gebruikersvoorkeuren/dasboardcard/actief"),
      { onSuccess: (instellingen) => this.cacheDashboardCards(instellingen) },
    );
  }

  addDashboardCard() {
    return mergeMutationOptions(
      this.zacQueryClient.PUT("/rest/gebruikersvoorkeuren/dasboardcard"),
      { onSuccess: (instellingen) => this.cacheDashboardCards(instellingen) },
    );
  }

  deleteDashboardCard() {
    return mergeMutationOptions(
      this.zacQueryClient.DELETE("/rest/gebruikersvoorkeuren/dasboardcard"),
      { onSuccess: (instellingen) => this.cacheDashboardCards(instellingen) },
    );
  }

  private cacheDashboardCards(
    instellingen: GeneratedType<"RestDashboardCardInstelling">[],
  ) {
    this.queryClient.setQueryData(
      this.listDashboardCards().queryKey,
      instellingen,
    );
  }
}

/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { PostBody } from "../shared/http/http-client";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";
import { GeneratedType } from "../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class GebruikersvoorkeurenService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);

  listZoekOpdrachten(lijstID: GeneratedType<"Werklijst">) {
    return this.zacHttpClient.GET(
      "/rest/gebruikersvoorkeuren/zoekopdracht/{lijstID}",
      {
        path: { lijstID },
      },
    );
  }

  createOrUpdateZoekOpdrachten(
    body: PostBody<"/rest/gebruikersvoorkeuren/zoekopdracht">,
  ) {
    return this.zacHttpClient.POST(
      "/rest/gebruikersvoorkeuren/zoekopdracht",
      body,
    );
  }

  deleteZoekOpdrachten(id: number) {
    return this.zacQueryClient.DELETE(
      "/rest/gebruikersvoorkeuren/zoekopdracht/{id}",
      {
        path: { id },
      },
    );
  }

  setZoekopdrachtActief() {
    return this.zacQueryClient.PUT(
      "/rest/gebruikersvoorkeuren/zoekopdracht/actief",
    );
  }

  removeZoekopdrachtActief(werklijst: GeneratedType<"Werklijst">) {
    return this.zacQueryClient.DELETE(
      "/rest/gebruikersvoorkeuren/zoekopdracht/{werklijst}/actief",
      {
        path: { werklijst },
      },
    );
  }

  readTabelGegevens(werklijst: GeneratedType<"Werklijst">) {
    return this.zacHttpClient.GET(
      "/rest/gebruikersvoorkeuren/tabel-gegevens/{werklijst}",
      {
        path: { werklijst },
      },
    );
  }

  updateAantalPerPagina(werklijst: GeneratedType<"Werklijst">, aantal: number) {
    return this.zacQueryClient.PUT(
      "/rest/gebruikersvoorkeuren/aantal-per-pagina/{werklijst}/{aantal}",
      {
        path: { werklijst, aantal },
      },
    );
  }

  listDashboardCards() {
    return this.zacHttpClient.GET(
      "/rest/gebruikersvoorkeuren/dasboardcard/actief",
    );
  }

  updateDashboardCards() {
    return this.zacQueryClient.PUT(
      "/rest/gebruikersvoorkeuren/dasboardcard/actief",
    );
  }

  addDashboardCard() {
    return this.zacQueryClient.PUT("/rest/gebruikersvoorkeuren/dasboardcard");
  }

  deleteDashboardCard() {
    return this.zacQueryClient.DELETE(
      "/rest/gebruikersvoorkeuren/dasboardcard",
      {},
    );
  }
}

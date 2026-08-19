/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { PostBody, PutBody } from "../shared/http/http-client";
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

  deleteZoekOpdrachten() {
    return this.zacQueryClient.DELETE(
      "/rest/gebruikersvoorkeuren/zoekopdracht/{id}",
      (id: number) => ({ parameters: { path: { id } } }),
    );
  }

  setZoekopdrachtActief(
    body: PutBody<"/rest/gebruikersvoorkeuren/zoekopdracht/actief">,
  ) {
    return this.zacHttpClient.PUT(
      "/rest/gebruikersvoorkeuren/zoekopdracht/actief",
      body,
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
    return this.zacHttpClient.GET(
      "/rest/gebruikersvoorkeuren/tabel-gegevens/{werklijst}",
      {
        path: { werklijst },
      },
    );
  }

  updateAantalPerPagina(werklijst: GeneratedType<"Werklijst">, aantal: number) {
    return this.zacHttpClient.PUT(
      "/rest/gebruikersvoorkeuren/aantal-per-pagina/{werklijst}/{aantal}",
      undefined as never,
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

  updateDashboardCards(
    body: PutBody<"/rest/gebruikersvoorkeuren/dasboardcard/actief">,
  ) {
    return this.zacHttpClient.PUT(
      "/rest/gebruikersvoorkeuren/dasboardcard/actief",
      body,
    );
  }

  addDashboardCard(body: PutBody<"/rest/gebruikersvoorkeuren/dasboardcard">) {
    return this.zacHttpClient.PUT(
      "/rest/gebruikersvoorkeuren/dasboardcard",
      body,
    );
  }

  deleteDashboardCard() {
    return this.zacQueryClient.DELETE("/rest/gebruikersvoorkeuren/dasboardcard");
  }
}

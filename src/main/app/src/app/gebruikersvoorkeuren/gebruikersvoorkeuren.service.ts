/*
 * SPDX-FileCopyrightText: 2022 Atos
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
export class GebruikersvoorkeurenService {
  constructor(private readonly zacHttpClient: ZacHttpClient) {}

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
    return this.zacHttpClient.DELETE(
      "/rest/gebruikersvoorkeuren/zoekopdracht/{id}",
      {
        path: { id },
      },
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

  removeZoekopdrachtActief(werklijst: GeneratedType<"Werklijst">) {
    return this.zacHttpClient.DELETE(
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

  deleteDashboardCard(
    body: DeleteBody<"/rest/gebruikersvoorkeuren/dasboardcard">,
  ) {
    return this.zacHttpClient.DELETE(
      "/rest/gebruikersvoorkeuren/dasboardcard",
      {},
      body,
    );
  }
}

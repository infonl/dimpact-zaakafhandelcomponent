/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { PatchBody, PostBody } from "../shared/http/http-client";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class NotitieService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);

  listNotities(uuid: string) {
    return this.zacHttpClient.GET("/rest/notities/zaken/{uuid}", {
      path: { uuid },
    });
  }

  createNotitie(body: PostBody<"/rest/notities">) {
    return this.zacHttpClient.POST("/rest/notities", body);
  }

  updateNotitie(body: PatchBody<"/rest/notities">) {
    return this.zacHttpClient.PATCH("/rest/notities", body);
  }

  deleteNotitie(id: number) {
    return this.zacQueryClient.DELETE("/rest/notities/{id}", {
      path: { id },
    });
  }
}

/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class NotitieService {
  private readonly zacQueryClient = inject(ZacQueryClient);

  listNotities(uuid: string) {
    return this.zacQueryClient.GET("/rest/notities/zaken/{uuid}", {
      path: { uuid },
    });
  }

  createNotitie() {
    return this.zacQueryClient.POST("/rest/notities");
  }

  updateNotitie() {
    return this.zacQueryClient.PATCH("/rest/notities");
  }

  deleteNotitie() {
    return this.zacQueryClient.DELETE("/rest/notities/{id}", (id: number) => ({
      parameters: { path: { id } },
    }));
  }
}

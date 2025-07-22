/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import {
  PatchBody,
  PostBody,
  ZacHttpClient,
} from "../shared/http/zac-http-client";

@Injectable({
  providedIn: "root",
})
export class NotitieService {
  constructor(private readonly zacHttpClient: ZacHttpClient) {}

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
    return this.zacHttpClient.DELETE("/rest/notities/{id}", {
      path: { id },
    });
  }
}

/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { BehaviorSubject } from "rxjs";
import { switchMap } from "rxjs/operators";
import { PutBody } from "./shared/http/http-client";
import { ZacHttpClient } from "./shared/http/zac-http-client";
import { ZacQueryClient } from "./shared/http/zac-query-client";
import { GeneratedType } from "./shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class SignaleringenService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);

  private latestSignaleringSubject = new BehaviorSubject<null>(null);
  latestSignalering$ = this.latestSignaleringSubject.pipe(
    switchMap(() => this.zacHttpClient.GET("/rest/signaleringen/latest")),
  );

  updateSignaleringen() {
    this.latestSignaleringSubject.next(null);
  }

  listDashboardSignaleringTypen() {
    return this.zacHttpClient.GET("/rest/signaleringen/typen/dashboard");
  }

  listZakenSignalering(
    type: GeneratedType<"Type">,
    body: PutBody<"/rest/signaleringen/zaken/{type}">,
  ) {
    return this.zacQueryClient.PUT_QUERY(
      "/rest/signaleringen/zaken/{type}",
      body,
      { path: { type } },
    );
  }

  listTakenSignalering(
    signaleringType: GeneratedType<"RestSignaleringInstellingen">["type"],
  ) {
    return this.zacHttpClient.GET("/rest/signaleringen/taken/{type}", {
      path: { type: signaleringType },
    });
  }

  listInformatieobjectenSignalering(
    signaleringType: GeneratedType<"RestSignaleringInstellingen">["type"],
  ) {
    return this.zacHttpClient.GET(
      "/rest/signaleringen/informatieobjecten/{type}",
      {
        path: { type: signaleringType },
      },
    );
  }
}

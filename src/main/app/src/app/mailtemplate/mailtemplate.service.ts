/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { GeneratedType } from "../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class MailtemplateService {
  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  findMailtemplate(mailtemplateEnum: GeneratedType<"Mail">, zaakUUID: string) {
    return this.zacHttpClient.GET(
      "/rest/mailtemplates/{mailtemplateEnum}/{zaakUUID}",
      {
        path: { mailtemplateEnum, zaakUUID },
      },
    );
  }
}

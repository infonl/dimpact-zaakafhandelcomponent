/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ZacQueryClient } from "../shared/http/zac-query-client";
import { GeneratedType } from "../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class MailtemplateService {
  private readonly zacQueryClient = inject(ZacQueryClient);

  findMailtemplate(mailtemplateEnum: GeneratedType<"Mail">, zaakUUID: string) {
    return this.zacQueryClient.GET(
      "/rest/mailtemplates/{mailtemplateEnum}/{zaakUUID}",
      {
        path: { mailtemplateEnum, zaakUUID },
      },
    );
  }
}

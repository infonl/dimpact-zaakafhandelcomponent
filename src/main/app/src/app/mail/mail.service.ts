/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";
import { GeneratedType } from "../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class MailService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);

  sendMail(zaakUuid: string) {
    return this.zacQueryClient.POST("/rest/mail/send/{zaakUuid}", {
      path: { zaakUuid },
    });
  }

  sendAcknowledgeReceipt(
    zaakUuid: string,
    mailGegevens: GeneratedType<"RESTMailGegevens">,
  ) {
    return this.zacHttpClient.POST(
      "/rest/mail/acknowledge/{zaakUuid}",
      mailGegevens,
      { path: { zaakUuid } },
    );
  }
}

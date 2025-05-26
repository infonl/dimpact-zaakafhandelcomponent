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
export class MailService {
  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  sendMail(zaakUuid: string, mailGegevens: GeneratedType<"RESTMailGegevens">) {
    return this.zacHttpClient.POST("/rest/mail/send/{zaakUuid}", mailGegevens, {
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

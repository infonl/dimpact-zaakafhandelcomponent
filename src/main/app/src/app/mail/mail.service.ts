/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class MailService {
  private readonly zacQueryClient = inject(ZacQueryClient);

  sendMail(zaakUuid: string) {
    return this.zacQueryClient.POST("/rest/mail/send/{zaakUuid}", {
      path: { zaakUuid },
    });
  }

  sendAcknowledgeReceipt(zaakUuid: string) {
    return this.zacQueryClient.POST("/rest/mail/acknowledge/{zaakUuid}", {
      path: { zaakUuid },
    });
  }
}

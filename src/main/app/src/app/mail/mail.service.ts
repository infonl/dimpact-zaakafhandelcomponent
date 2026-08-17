/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { UtilService } from "../core/service/util.service";
import { mergeMutationOptions } from "../shared/http/merge-mutation-options";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class MailService {
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly utilService = inject(UtilService);

  sendMail(zaakUuid: string) {
    return mergeMutationOptions(
      this.zacQueryClient.POST("/rest/mail/send/{zaakUuid}", {
        path: { zaakUuid },
      }),
      { onSuccess: () => this.utilService.openSnackbar("msg.email.verstuurd") },
    );
  }

  sendAcknowledgeReceipt(zaakUuid: string) {
    return mergeMutationOptions(
      this.zacQueryClient.POST("/rest/mail/acknowledge/{zaakUuid}", {
        path: { zaakUuid },
      }),
      { onSuccess: () => this.utilService.openSnackbar("msg.email.verstuurd") },
    );
  }
}

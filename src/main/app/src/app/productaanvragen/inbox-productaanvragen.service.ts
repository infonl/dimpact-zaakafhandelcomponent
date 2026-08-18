/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { UtilService } from "../core/service/util.service";
import { PutBody } from "../shared/http/http-client";
import { mergeMutationOptions } from "../shared/http/merge-mutation-options";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class InboxProductaanvragenService {
  private basepath = "/rest/inbox-productaanvragen";
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly utilService = inject(UtilService);

  list(body: PutBody<"/rest/inbox-productaanvragen">) {
    return this.zacQueryClient.PUT_QUERY("/rest/inbox-productaanvragen", body);
  }

  delete() {
    return mergeMutationOptions(
      this.zacQueryClient.DELETE(
        "/rest/inbox-productaanvragen/{id}",
        (id: number) => ({ parameters: { path: { id } } }),
      ),
      {
        onSuccess: () =>
          this.utilService.openSnackbar(
            "msg.inboxProductaanvraag.verwijderen.uitgevoerd",
          ),
      },
    );
  }

  pdfPreview(aanvraagdocumentUUID: string): string {
    return `${this.basepath}/${aanvraagdocumentUUID}/pdfPreview`;
  }
}

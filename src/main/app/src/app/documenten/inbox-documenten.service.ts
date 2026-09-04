/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { UtilService } from "../core/service/util.service";
import { PutBody } from "../shared/http/http-client";
import { mergeMutationOptions } from "../shared/http/merge-mutation-options";
import { ZacQueryClient } from "../shared/http/zac-query-client";
import { GeneratedType } from "../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class InboxDocumentenService {
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly utilService = inject(UtilService);

  list(body: PutBody<"/rest/inboxdocumenten">) {
    return this.zacQueryClient.PUT_QUERY("/rest/inboxdocumenten", body);
  }

  delete() {
    return mergeMutationOptions(
      this.zacQueryClient.DELETE(
        "/rest/inboxdocumenten/{id}",
        (inboxDocument: GeneratedType<"RestInboxDocument">) => ({
          parameters: { path: { id: inboxDocument.id ?? -1 } },
        }),
      ),
      {
        onSuccess: (result, inboxDocument) =>
          this.utilService.openSnackbar(
            result?.isInformatieobjectDeleted === false
              ? "msg.document.verwijderen.inbox.niet-verwijderd"
              : "msg.document.verwijderen.uitgevoerd",
            { document: inboxDocument.titel },
          ),
      },
    );
  }
}

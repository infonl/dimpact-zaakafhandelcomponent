/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { UtilService } from "../core/service/util.service";
import { PutBody } from "../shared/http/http-client";
import { mergeMutationOptions } from "../shared/http/merge-mutation-options";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";
import { GeneratedType } from "../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class InboxDocumentenService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly utilService = inject(UtilService);

  list(body: PutBody<"/rest/inboxdocumenten">) {
    return this.zacHttpClient.PUT("/rest/inboxdocumenten", body);
  }

  delete(inboxDocument: GeneratedType<"RestInboxDocument">) {
    return mergeMutationOptions(
      this.zacQueryClient.DELETE("/rest/inboxdocumenten/{id}", {
        path: { id: inboxDocument.id ?? -1 },
      }),
      {
        onSuccess: (result) =>
          this.utilService.openSnackbar(
            result?.gekoppeldAanZaak
              ? "msg.document.verwijderen.inbox.gekoppeldAanZaak"
              : "msg.document.verwijderen.uitgevoerd",
            { document: inboxDocument.titel },
          ),
      },
    );
  }
}

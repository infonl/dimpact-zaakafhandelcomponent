/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { UtilService } from "../core/service/util.service";
import { PutBody } from "../shared/http/http-client";
import { mergeMutationOptions } from "../shared/http/merge-mutation-options";
import { ZacQueryClient } from "../shared/http/zac-query-client";
import { GeneratedType } from "../shared/utils/generated-types";
import {
  OntkoppeldeDocumentenBulkFoutenDialogComponent,
  OntkoppeldeDocumentenBulkFoutenDialogData,
} from "./ontkoppelde-documenten-bulk-fouten-dialog/ontkoppelde-documenten-bulk-fouten-dialog.component";

@Injectable({
  providedIn: "root",
})
export class OntkoppeldeDocumentenService {
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly queryClient = inject(QueryClient);
  private readonly utilService = inject(UtilService);
  private readonly dialog = inject(MatDialog);

  list(body: PutBody<"/rest/ontkoppeldedocumenten">) {
    return this.zacQueryClient.PUT_QUERY("/rest/ontkoppeldedocumenten", body);
  }

  delete() {
    return mergeMutationOptions(
      this.zacQueryClient.DELETE(
        "/rest/ontkoppeldedocumenten/{id}",
        (detachedDocument: GeneratedType<"RestDetachedDocument">) => ({
          parameters: { path: { id: detachedDocument.id ?? -1 } },
        }),
      ),
      {
        onSuccess: (_data, detachedDocument) => {
          void this.queryClient.invalidateQueries({
            queryKey: ["/rest/ontkoppeldedocumenten"],
          });
          this.utilService.openSnackbar("msg.document.verwijderen.uitgevoerd", {
            document: detachedDocument.titel,
          });
        },
      },
    );
  }

  deleteList() {
    return mergeMutationOptions(
      this.zacQueryClient.PUT("/rest/ontkoppeldedocumenten/lijst/verwijderen"),
      { onSuccess: () => this.invalidateList() },
    );
  }

  linkList() {
    return mergeMutationOptions(
      this.zacQueryClient.PUT("/rest/ontkoppeldedocumenten/lijst/koppelen"),
      { onSuccess: () => this.invalidateList() },
    );
  }

  reportBulkResult(
    result: GeneratedType<"RestDetachedDocumentBulkResult">,
    actie: "verwijderen" | "koppelen",
    selectedDocuments: GeneratedType<"RestDetachedDocument">[],
    args: object = {},
  ) {
    const aantal = result.verwerkt?.length ?? 0;
    this.utilService.openSnackbar(
      `msg.ontkoppelde-documenten.bulk.${actie}.uitgevoerd${aantal === 1 ? ".enkelvoud" : ""}`,
      { aantal, ...args },
    );
    if (result.mislukt?.length) {
      this.dialog.open(OntkoppeldeDocumentenBulkFoutenDialogComponent, {
        data: {
          actie,
          mislukt: result.mislukt.map((failure) => ({
            ...failure,
            titel:
              failure.titel ??
              selectedDocuments.find(({ id }) => id === failure.id)?.titel,
          })),
        } satisfies OntkoppeldeDocumentenBulkFoutenDialogData,
      });
    }
  }

  private invalidateList() {
    void this.queryClient.invalidateQueries({
      queryKey: ["/rest/ontkoppeldedocumenten"],
    });
  }
}

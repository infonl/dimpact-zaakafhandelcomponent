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
export class OntkoppeldeDocumentenService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly utilService = inject(UtilService);

  list(body: PutBody<"/rest/ontkoppeldedocumenten">) {
    return this.zacHttpClient.PUT("/rest/ontkoppeldedocumenten", body);
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
        onSuccess: (_data, detachedDocument) =>
          this.utilService.openSnackbar("msg.document.verwijderen.uitgevoerd", {
            document: detachedDocument.titel,
          }),
      },
    );
  }
}

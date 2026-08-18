/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import {
  type CreateMutationOptions,
  QueryClient,
} from "@tanstack/angular-query-experimental";
import { UtilService } from "../core/service/util.service";
import { PostBody } from "../shared/http/http-client";
import { mergeMutationOptions } from "../shared/http/merge-mutation-options";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";
import { GeneratedType } from "../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class MailtemplateBeheerService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly utilService = inject(UtilService);
  private readonly queryClient = inject(QueryClient, { optional: true });

  readMailtemplateQuery(id: number) {
    return this.zacQueryClient.GET("/rest/beheer/mailtemplates/{id}", {
      path: { id },
    });
  }

  listMailtemplates() {
    return this.zacHttpClient.GET("/rest/beheer/mailtemplates");
  }

  listKoppelbareMailtemplates() {
    return this.zacHttpClient.GET("/rest/beheer/mailtemplates/koppelbaar");
  }

  deleteMailtemplate(id: number) {
    return mergeMutationOptions(
      this.zacQueryClient.DELETE("/rest/beheer/mailtemplates/{id}", {
        path: { id },
      }),
      {
        onSuccess: () =>
          this.utilService.openSnackbar(
            "msg.mailtemplate.verwijderen.uitgevoerd",
          ),
      },
    );
  }

  saveMailtemplate(id: number | null | undefined) {
    // The create endpoint declares no response schema, so it is generated as `{}`
    // where the update returns the saved template. Neither caller reads the
    // response, so both are typed by the little they have in common.
    const save = (
      id == null
        ? this.zacQueryClient.POST("/rest/beheer/mailtemplates")
        : this.zacQueryClient.PUT("/rest/beheer/mailtemplates/{id}", {
            path: { id },
          })
    ) as CreateMutationOptions<
      unknown,
      HttpErrorResponse,
      PostBody<"/rest/beheer/mailtemplates">,
      void
    >;

    return mergeMutationOptions(save, {
      onSuccess: () => {
        if (id != null) {
          void this.queryClient?.invalidateQueries({
            queryKey: this.readMailtemplateQuery(id).queryKey,
          });
        }
        this.utilService.openSnackbar("msg.mailtemplate.opgeslagen");
      },
    });
  }

  ophalenVariabelenVoorMail(mail: GeneratedType<"Mail">) {
    return this.zacHttpClient.GET(
      "/rest/beheer/mailtemplates/variabelen/{mail}",
      {
        path: { mail },
      },
    );
  }
}

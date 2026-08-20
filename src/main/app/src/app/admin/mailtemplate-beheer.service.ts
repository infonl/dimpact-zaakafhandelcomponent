/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { lastValueFrom } from "rxjs";
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

  deleteMailtemplate() {
    return mergeMutationOptions(
      this.zacQueryClient.DELETE(
        "/rest/beheer/mailtemplates/{id}",
        (id: number) => ({ parameters: { path: { id } } }),
      ),
      {
        onSuccess: () =>
          this.utilService.openSnackbar(
            "msg.mailtemplate.verwijderen.uitgevoerd",
          ),
      },
    );
  }

  saveMailtemplate(
    id: number | null | undefined,
    body: PostBody<"/rest/beheer/mailtemplates">,
  ) {
    return lastValueFrom(
      id == null
        ? this.zacHttpClient.POST("/rest/beheer/mailtemplates", body)
        : this.zacHttpClient.PUT("/rest/beheer/mailtemplates/{id}", body, {
            path: { id },
          }),
    );
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

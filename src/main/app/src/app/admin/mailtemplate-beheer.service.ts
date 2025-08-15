/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import {PostBody, PutBody, ZacHttpClient} from "../shared/http/zac-http-client";
import { GeneratedType } from "../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class MailtemplateBeheerService {
  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  readMailtemplate(id: number) {
    return this.zacHttpClient.GET("/rest/beheer/mailtemplates/{id}", {
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
    return this.zacHttpClient.DELETE("/rest/beheer/mailtemplates/{id}", {
      path: { id },
    });
  }

  createMailtemplate(body: PostBody<"/rest/beheer/mailtemplates">) {
    return this.zacHttpClient.POST("/rest/beheer/mailtemplates", body);
  }

  updateMailtemplate(id: number, body: PutBody<"/rest/beheer/mailtemplates/{id}"> ) {
    return this.zacHttpClient.PUT("/rest/beheer/mailtemplates/{id}", body, {
      path: { id },
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

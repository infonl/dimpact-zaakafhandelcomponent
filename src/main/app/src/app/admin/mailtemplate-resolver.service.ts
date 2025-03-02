/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { Observable } from "rxjs";
import { MailtemplateBeheerService } from "./mailtemplate-beheer.service";
import { Mailtemplate } from "./model/mailtemplate";

@Injectable({
  providedIn: "root",
})
export class MailtemplateResolver {
  constructor(private service: MailtemplateBeheerService) {}

  resolve(route: ActivatedRouteSnapshot): Observable<Mailtemplate> {
    const id = route.paramMap.get("id");

    if (!id) {
      throw new Error(
        `${MailtemplateResolver.name}: no 'id' parameter found in route`,
      );
    }

    return this.service.readMailtemplate(id);
  }
}

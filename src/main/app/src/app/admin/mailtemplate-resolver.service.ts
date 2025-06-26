/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { MailtemplateBeheerService } from "./mailtemplate-beheer.service";

@Injectable({
  providedIn: "root",
})
export class MailtemplateResolver {
  constructor(private service: MailtemplateBeheerService) {}

  resolve(route: ActivatedRouteSnapshot) {
    const id = route.paramMap.get("id");

    if (!id) {
      throw new Error(
        `${MailtemplateResolver.name}: no 'id' parameter found in route`,
      );
    }

    return this.service.readMailtemplate(Number(id));
  }
}

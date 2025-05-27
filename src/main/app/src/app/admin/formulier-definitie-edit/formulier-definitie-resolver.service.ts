/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { of } from "rxjs";
import { FormulierDefinitieService } from "../formulier-defintie.service";
import { FormulierDefinitie } from "../model/formulieren/formulier-definitie";

@Injectable({
  providedIn: "root",
})
export class FormulierDefinitieResolverService {
  constructor(private service: FormulierDefinitieService) {}

  resolve(route: ActivatedRouteSnapshot) {
    const id = route.paramMap.get("id");

    if (!id) {
      throw new Error(
        `${FormulierDefinitieResolverService.name}: no 'id' parameter found in route`,
      );
    }
    if (id === "add") {
      return of(new FormulierDefinitie());
    }

    return this.service.read(id);
  }
}

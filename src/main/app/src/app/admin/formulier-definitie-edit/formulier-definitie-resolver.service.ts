/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { of } from "rxjs";
import { GeneratedType } from "../../shared/utils/generated-types";
import { FormulierDefinitieService } from "../formulier-defintie.service";

@Injectable({
  providedIn: "root",
})
export class FormulierDefinitieResolverService {
  constructor(private service: FormulierDefinitieService) {}

  resolve(route: ActivatedRouteSnapshot) {
    const id = route.paramMap.get("id");

    if (!id) {
      throw new Error(
        `${FormulierDefinitieResolverService.name}: 'id' in not defined in route`,
      );
    }

    if (id === "add") {
      return of({
        veldDefinities: [],
      } satisfies GeneratedType<"RESTFormulierDefinitie">);
    }
    const idAsNumber = Number(id);
    if (isNaN(idAsNumber)) {
      throw new Error(
        `${FormulierDefinitieResolverService.name}: 'id' in route not a valid number`,
      );
    }

    return this.service.read(idAsNumber);
  }
}

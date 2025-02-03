/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { FormulierDefinitieID } from "../../admin/model/formulier-definitie";
import { ProcessFormulierenBuilder } from "./process-formulieren-builder";

@Injectable({
  providedIn: "root",
})
export class ProcessFormulierenService {
  constructor() {}

  public getFormulierBuilder(
    formulierDefinitie: FormulierDefinitieID,
  ): ProcessFormulierenBuilder {
    return new ProcessFormulierenBuilder();
  }
}

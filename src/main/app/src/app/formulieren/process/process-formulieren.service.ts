/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ProcessFormulierenBuilder } from "./process-formulieren-builder";

@Injectable({
  providedIn: "root",
})
export class ProcessFormulierenService {
  constructor() {}

  public getFormulierBuilder(): ProcessFormulierenBuilder {
    return new ProcessFormulierenBuilder();
  }
}

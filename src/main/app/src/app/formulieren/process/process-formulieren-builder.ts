/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { PlanItem } from "../../plan-items/model/plan-item";
import { Zaak } from "../../zaken/model/zaak";
import { AbstractProcessFormulier } from "./abstract-process-formulier";

export class ProcessFormulierenBuilder {
  protected readonly _formulier: AbstractProcessFormulier;

  form(planItem: PlanItem, zaak: Zaak): ProcessFormulierenBuilder {
    return this;
  }

  build(): AbstractProcessFormulier {
    return this._formulier;
  }
}

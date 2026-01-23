/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SortDirection } from "@angular/material/sort";
import { ListParameters } from "../../shared/model/list-parameters";
import { ToggleSwitchOptions } from "../../shared/table-zoek-filters/toggle-filter/toggle-switch-options";
import { Zaaktype } from "../../zaken/model/zaaktype";
import { DatumRange } from "../../zoeken/model/datum-range";
import { CaseDefinition } from "../model/case-definition";

export class ZaakafhandelParametersListParameters extends ListParameters {
  valide: ToggleSwitchOptions = ToggleSwitchOptions.INDETERMINATE;
  geldig: ToggleSwitchOptions = ToggleSwitchOptions.INDETERMINATE;
  zaaktype: Partial<Zaaktype> | null = null;
  caseDefinition: Partial<CaseDefinition> | null = null;
  beginGeldigheid = new DatumRange();
  eindeGeldigheid = new DatumRange();

  constructor(sort: string, order: SortDirection) {
    super(sort, order);
  }
}

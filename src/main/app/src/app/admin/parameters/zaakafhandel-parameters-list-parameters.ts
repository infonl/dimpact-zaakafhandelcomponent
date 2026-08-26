/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SortDirection } from "@angular/material/sort";
import { ListParameters } from "../../shared/model/list-parameters";
import { ToggleSwitchOptions } from "../../shared/table-zoek-filters/toggle-filter/toggle-switch-options";
import { GeneratedType } from "../../shared/utils/generated-types";

export class ZaakafhandelParametersListParameters extends ListParameters {
  valide: ToggleSwitchOptions = ToggleSwitchOptions.CHECKED;
  geldig: ToggleSwitchOptions = ToggleSwitchOptions.CHECKED;
  zaaktype: Partial<GeneratedType<"RestZaaktype">> | null = null;
  caseDefinition: Partial<GeneratedType<"RESTCaseDefinition">> | null = null;
  beginGeldigheid: GeneratedType<"RestDatumRange"> = { van: null, tot: null };
  eindeGeldigheid: GeneratedType<"RestDatumRange"> = { van: null, tot: null };

  constructor(sort: string, order: SortDirection) {
    super(sort, order);
  }
}

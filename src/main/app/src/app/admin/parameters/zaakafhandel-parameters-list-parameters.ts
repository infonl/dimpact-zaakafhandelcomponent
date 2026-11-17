/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SortDirection } from "@angular/material/sort";
import moment from "moment";
import { PutBody } from "../../shared/http/http-client";
import { ListParameters } from "../../shared/model/list-parameters";
import { ToggleSwitchOptions } from "../../shared/table-zoek-filters/toggle-filter/toggle-switch-options";
import { GeneratedType } from "../../shared/utils/generated-types";

export const ZAAKAFHANDELPARAMETERS_PAGE_SIZE = 25;

export class ZaakafhandelParametersListParameters extends ListParameters {
  valide: ToggleSwitchOptions = ToggleSwitchOptions.CHECKED;
  geldig: ToggleSwitchOptions = ToggleSwitchOptions.CHECKED;
  zaaktype: GeneratedType<"RestZaaktypeFilterOptie"> | null = null;
  processDefinition: Partial<
    GeneratedType<"RestProcessDefinitionOverzicht">
  > | null = null;
  beginGeldigheid: GeneratedType<"RestDatumRange"> = { van: null, tot: null };
  eindeGeldigheid: GeneratedType<"RestDatumRange"> = { van: null, tot: null };

  constructor(sort: string, order: SortDirection) {
    super(sort, order);
  }
}

const sortFields: Record<
  string,
  GeneratedType<"RestZaaktypeConfigurationSortField">
> = {
  valide: "VALIDE",
  omschrijving: "OMSCHRIJVING",
  model: "PROCESS_DEFINITION",
  geldig: "GELDIG",
  beginGeldigheid: "BEGIN_GELDIGHEID",
  eindeGeldigheid: "EINDE_GELDIGHEID",
};

const toBoolean = (toggleSwitchOption: ToggleSwitchOptions) => {
  switch (toggleSwitchOption) {
    case ToggleSwitchOptions.CHECKED:
      return true;
    case ToggleSwitchOptions.UNCHECKED:
      return false;
    default:
      return null;
  }
};

const toLocalDate = (date?: string | null) =>
  date ? moment(date).format("YYYY-MM-DD") : null;

const toLocalDateRange = (
  range: GeneratedType<"RestDatumRange">,
): GeneratedType<"RestDatumRange"> => ({
  van: toLocalDate(range.van),
  tot: toLocalDate(range.tot),
});

export const toZaaktypeConfigurationListBody = (
  listParameters: ZaakafhandelParametersListParameters,
): PutBody<"/rest/zaakafhandelparameters"> => ({
  page: listParameters.page,
  rows: ZAAKAFHANDELPARAMETERS_PAGE_SIZE,
  sortField:
    listParameters.order && sortFields[listParameters.sort]
      ? sortFields[listParameters.sort]
      : null,
  sortOrder: listParameters.order === "desc" ? "DESC" : "ASC",
  isValide: toBoolean(listParameters.valide),
  isNuGeldig: toBoolean(listParameters.geldig),
  zaaktypeIdentificatie: listParameters.zaaktype?.identificatie ?? null,
  processDefinitionKey: listParameters.processDefinition?.key ?? null,
  beginGeldigheid: toLocalDateRange(listParameters.beginGeldigheid),
  eindeGeldigheid: toLocalDateRange(listParameters.eindeGeldigheid),
});

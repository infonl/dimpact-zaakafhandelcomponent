/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SortDirection } from "@angular/material/sort";
import { ListParameters } from "../../shared/model/list-parameters";
import { ToggleSwitchOptions } from "../../shared/table-zoek-filters/toggle-filter/toggle-switch-options";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakafhandelParametersListQuery } from "../zaakafhandel-parameters.service";

export const ZAAKAFHANDELPARAMETERS_PAGE_SIZE = 25;

export class ZaakafhandelParametersListParameters extends ListParameters {
  valide: ToggleSwitchOptions = ToggleSwitchOptions.CHECKED;
  geldig: ToggleSwitchOptions = ToggleSwitchOptions.CHECKED;
  zaaktype: GeneratedType<"RestZaaktypeFilterOptie"> | null = null;
  processDefinition: Partial<
    GeneratedType<"RestProcessDefinitionOverzicht">
  > | null = null;
  beginGeldigheid: GeneratedType<"RestDateRange"> = {
    from: null,
    through: null,
  };
  eindeGeldigheid: GeneratedType<"RestDateRange"> = {
    from: null,
    through: null,
  };

  constructor(sort: string, order: SortDirection) {
    super(sort, order);
  }
}

const sortFields = {
  valide: "VALIDE",
  omschrijving: "OMSCHRIJVING",
  model: "PROCESS_DEFINITION",
  geldig: "GELDIG",
  beginGeldigheid: "BEGIN_GELDIGHEID",
  eindeGeldigheid: "EINDE_GELDIGHEID",
} as const satisfies Record<
  string,
  GeneratedType<"RestZaaktypeConfigurationSortField">
>;

export type ZaakafhandelParametersSortColumn = keyof typeof sortFields;

const isSortColumn = (sort: string): sort is ZaakafhandelParametersSortColumn =>
  Object.hasOwn(sortFields, sort);

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

export const toZaaktypeConfigurationListQuery = (
  listParameters: ZaakafhandelParametersListParameters,
): ZaakafhandelParametersListQuery => ({
  page: listParameters.page,
  rows: ZAAKAFHANDELPARAMETERS_PAGE_SIZE,
  sortField:
    listParameters.order && isSortColumn(listParameters.sort)
      ? sortFields[listParameters.sort]
      : null,
  sortOrder: listParameters.order === "desc" ? "DESC" : "ASC",
  isValide: toBoolean(listParameters.valide),
  isNuGeldig: toBoolean(listParameters.geldig),
  zaaktypeIdentificatie: listParameters.zaaktype?.identificatie ?? null,
  processDefinitionKey: listParameters.processDefinition?.key ?? null,
  beginGeldigheidVan: listParameters.beginGeldigheid.from ?? null,
  beginGeldigheidTot: listParameters.beginGeldigheid.through ?? null,
  eindeGeldigheidVan: listParameters.eindeGeldigheid.from ?? null,
  eindeGeldigheidTot: listParameters.eindeGeldigheid.through ?? null,
});

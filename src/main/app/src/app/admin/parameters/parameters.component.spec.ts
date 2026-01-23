/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { MatSelectChange } from "@angular/material/select";
import { DatumRange } from "src/app/zoeken/model/datum-range";
import { SessionStorageUtil } from "../../shared/storage/session-storage.util";
import { ToggleSwitchOptions } from "../../shared/table-zoek-filters/toggle-filter/toggle-switch-options";
import { ParametersComponent } from "./parameters.component";
import { ZaakafhandelParametersListParameters } from "./zaakafhandel-parameters-list-parameters";

describe("ParametersComponent applyFilter", () => {
  const filterTestCases = [
    {
      event: ToggleSwitchOptions.CHECKED as ToggleSwitchOptions,
      filter: "valide" as keyof ZaakafhandelParametersListParameters,
      expectedValue: ToggleSwitchOptions.CHECKED,
    },
    {
      event: ToggleSwitchOptions.UNCHECKED as ToggleSwitchOptions,
      filter: "geldig" as keyof ZaakafhandelParametersListParameters,
      expectedValue: ToggleSwitchOptions.UNCHECKED,
    },
    {
      event: {
        value: { identificatie: "1", omschrijving: "Zaak test" },
      } as MatSelectChange,
      filter: "zaaktype" as keyof ZaakafhandelParametersListParameters,
      expectedValue: { identificatie: "1", omschrijving: "Zaak test" },
    },
    {
      event: { value: { key: "a", naam: "Case" } } as MatSelectChange,
      filter: "caseDefinition" as keyof ZaakafhandelParametersListParameters,
      expectedValue: { key: "a", naam: "Case" },
    },
    {
      event: "name",
      filter: "sort" as keyof ZaakafhandelParametersListParameters,
      expectedValue: "name",
    },
    {
      event: "asc",
      filter: "order" as keyof ZaakafhandelParametersListParameters,
      expectedValue: "asc",
    },
    {
      event: 1,
      filter: "page" as keyof ZaakafhandelParametersListParameters,
      expectedValue: 1,
    },
    {
      event: 10,
      filter: "maxResults" as keyof ZaakafhandelParametersListParameters,
      expectedValue: 10,
    },
  ];

  it.each(filterTestCases)(
    "should update '%s' in filterParameters correctly",
    ({ event, filter, expectedValue }) => {
      const setItemSpy = jest
        .spyOn(SessionStorageUtil, "setItem")
        .mockImplementation(jest.fn());

      const component = new ParametersComponent(
        {} as any, // skip constructor deps
        { getZaakafhandelParameters: jest.fn() } as any,
        { getUniqueItemsList: jest.fn() } as any,
      );

      component["storedParameterFilters"] = "test-key";
      component.filterParameters = {
        valide: ToggleSwitchOptions.INDETERMINATE,
        geldig: ToggleSwitchOptions.INDETERMINATE,
        zaaktype: null,
        caseDefinition: null,
        beginGeldigheid: new DatumRange(),
        eindeGeldigheid: new DatumRange(),
        sort: "",
        order: "",
        page: 0,
        maxResults: 25,
      } as ZaakafhandelParametersListParameters;

      component.applyFilter({
        event,
        filter,
      });

      expect(component.filterParameters[filter]).toEqual(expectedValue);
      expect(setItemSpy).toHaveBeenCalledWith(
        "test-key",
        component.filterParameters,
      );
    },
  );
});

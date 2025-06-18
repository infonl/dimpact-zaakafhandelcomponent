/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ZoekFilters } from "../../gebruikersvoorkeuren/zoekopdracht/zoekfilters.model";
import { GeneratedType } from "../../shared/utils/generated-types";
import { DatumRange } from "./datum-range";
import { DatumVeld } from "./datum-veld";
import { FilterParameters } from "./filter-parameters";
import { FilterVeld } from "./filter-veld";
import { ZoekVeld } from "./zoek-veld";

export class ZoekParameters implements ZoekFilters {
  readonly filtersType = "ZoekParameters";
  type?: GeneratedType<"ZoekObjectType">;
  alleenMijnZaken = false;
  alleenOpenstaandeZaken = false;
  alleenAfgeslotenZaken = false;
  alleenMijnTaken = false;
  zoeken: Partial<Record<ZoekVeld, string>> = {};
  filters: Partial<Record<FilterVeld, FilterParameters>> = {};
  datums: Partial<Record<DatumVeld, DatumRange>> = {};
  sorteerVeld?: GeneratedType<"SorteerVeld">;
  sorteerRichting: "desc" | "asc" | "" = "";
  rows = 25;
  page = 0;

  static heeftActieveFilters(zoekFilters: ZoekFilters) {
    if (zoekFilters.zoeken) {
      return Object.values(zoekFilters.zoeken).some(Boolean);
    }
    if (zoekFilters.filters) {
      return Object.values(zoekFilters.filters).some(
        (filter) => filter?.values?.length,
      );
    }
    if (zoekFilters.datums) {
      return Object.values(zoekFilters.datums).some(
        ({ van, tot }) => van != null || tot != null,
      );
    }
    return false;
  }
}

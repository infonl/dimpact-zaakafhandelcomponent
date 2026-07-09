/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ZoekFilters } from "../../gebruikersvoorkeuren/zoekopdracht/zoekopdracht.component";
import { DatumRange } from "./datum-range";
import { DatumVeld } from "./datum-veld";
import { FilterParameters } from "./filter-parameters";
import { FilterVeld } from "./filter-veld";
import { SorteerVeld } from "./sorteer-veld";
import { ZoekObjectType } from "./zoek-object-type";
import { ZoekVeld } from "./zoek-veld";

export class ZoekParameters implements ZoekFilters {
  readonly filtersType = "ZoekParameters";
  type?: ZoekObjectType;
  alleenMijnZaken = false;
  alleenOpenstaandeZaken = false;
  alleenAfgeslotenZaken = false;
  alleenMijnTaken = false;
  zoeken: Partial<Record<ZoekVeld, string>> = {};
  filters: Partial<Record<FilterVeld, FilterParameters>> = {};
  datums: Partial<Record<DatumVeld, DatumRange>> = {};
  sorteerVeld?: SorteerVeld;
  sorteerRichting: "desc" | "asc" | "" = "";
  rows = 25;
  page = 0;

  static heeftActieveFilters(zoekFilters: any): boolean {
    console.log(zoekFilters);
    if (zoekFilters.zoeken) {
      return Object.values(zoekFilters.zoeken).some(Boolean);
    }
    if (zoekFilters.filters) {
      return Object.values(zoekFilters.filters).some(
        (filter) => (filter as any)?.values?.length,
      );
    }
    if (zoekFilters.datums) {
      return Object.values(zoekFilters.datums).some(
        (datum) =>
          (datum as DatumRange).van != null ||
          (datum as DatumRange).tot != null,
      );
    }
    return false;
  }
}

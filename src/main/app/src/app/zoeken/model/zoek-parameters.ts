/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SortDirection } from "@angular/material/sort";
import { ZoekFilters } from "../../gebruikersvoorkeuren/zoekopdracht/zoekfilters.model";
import { GeneratedType } from "../../shared/utils/generated-types";

/**
 * A stricter version of RestZoekParameters used internally where
 * zoeken, filters, and datums are always initialized (never null/undefined).
 */
export type ZoekParametersInternal = Omit<
  GeneratedType<"RestZoekParameters">,
  "zoeken" | "filters" | "datums" | "sorteerRichting" | "sorteerVeld"
> & {
  filtersType: "ZoekParameters";
  zoeken: Record<string, string | undefined>;
  filters: Record<string, GeneratedType<"FilterParameters">>;
  datums: Record<string, GeneratedType<"RestDatumRange">>;
  sorteerRichting: SortDirection;
  sorteerVeld?: string;
};

export function getDefaultZoekParameters(): ZoekParametersInternal {
  return {
    filtersType: "ZoekParameters",
    rows: 25,
    page: 0,
    alleenMijnZaken: false,
    alleenOpenstaandeZaken: false,
    alleenAfgeslotenZaken: false,
    alleenMijnTaken: false,
    datums: {},
    zoeken: {},
    filters: {},
    sorteerRichting: "",
  };
}

export function hasActiveSearchFilters(zoekFilters: ZoekFilters) {
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

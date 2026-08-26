/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SortDirection } from "@angular/material/sort";
import { ZoekFilters } from "../../gebruikersvoorkeuren/zoekopdracht/zoekfilters.model";
import { GeneratedType } from "../../shared/utils/generated-types";

type RestZoekParameters = GeneratedType<"RestZoekParameters">;

/**
 * The search parameters as a werklijst keeps them: the collections and the sort
 * direction are always filled in, where the wire format leaves them optional.
 */
export type ZoekParameters = Omit<
  RestZoekParameters,
  "zoeken" | "filters" | "datums" | "sorteerRichting"
> & {
  zoeken: NonNullable<RestZoekParameters["zoeken"]>;
  filters: NonNullable<RestZoekParameters["filters"]>;
  datums: NonNullable<RestZoekParameters["datums"]>;
  sorteerRichting: SortDirection;
};

export function getDefaultZoekParameters(): ZoekParameters {
  return {
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

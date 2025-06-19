/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ZoekFilters } from "../../gebruikersvoorkeuren/zoekopdracht/zoekfilters.model";
import { GeneratedType } from "../../shared/utils/generated-types";

export const DEFAULT_ZOEK_PARAMETERS: GeneratedType<"RestZoekParameters"> = {
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

export function heeftActieveZoekFilters(zoekFilters: ZoekFilters) {
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

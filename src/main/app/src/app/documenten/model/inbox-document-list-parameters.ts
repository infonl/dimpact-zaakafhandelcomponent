/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SortDirection } from "@angular/material/sort";
import { ZoekFilters } from "../../gebruikersvoorkeuren/zoekopdracht/zoekfilters.model";
import { ListParameters } from "../../shared/model/list-parameters";
import { DatumRange } from "../../zoeken/model/datum-range";

/**
 * @deprecated - use the `GeneratedType`
 */
export class InboxDocumentListParameters
  extends ListParameters
  implements ZoekFilters
{
  readonly filtersType = "InboxDocumentListParameters";
  identificatie: string;
  creatiedatum = new DatumRange();
  titel: string;

  constructor(sort: string, order: SortDirection) {
    super(sort, order);
  }

  static heeftActieveFilters(zoekFilters: ZoekFilters): boolean {
    if (zoekFilters.identificatie != null) {
      return true;
    }
    if (
      zoekFilters.creatiedatum?.van != null ||
      zoekFilters.creatiedatum?.tot != null
    ) {
      return true;
    }
    if (zoekFilters.titel != null) {
      return true;
    }
    return false;
  }
}

/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SortDirection } from "@angular/material/sort";
import { ZoekFilters } from "../../gebruikersvoorkeuren/zoekopdracht/zoekfilters.model";
import { ListParameters } from "../../shared/model/list-parameters";
import { DatumRange } from "../../zoeken/model/datum-range";

export class InboxProductaanvraagListParameters
  extends ListParameters
  implements ZoekFilters
{
  readonly filtersType = "InboxProductaanvraagListParameters";
  type: string;
  ontvangstdatum = new DatumRange();
  initiatorID: string;

  constructor(sort: string, order: SortDirection) {
    super(sort, order);
  }

  static heeftActieveFilters(zoekFilters: ZoekFilters) {
    return (
      zoekFilters.type != null ||
      zoekFilters.ontvangstdatum?.van != null ||
      zoekFilters.ontvangstdatum?.tot != null ||
      zoekFilters.initiatorID != null
    );
  }
}

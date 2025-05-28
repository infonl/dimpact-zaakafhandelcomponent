/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SortDirection } from "@angular/material/sort";
import { ZoekFilters } from "../../gebruikersvoorkeuren/zoekopdracht/zoekfilters.model";
import { ListParameters } from "../../shared/model/list-parameters";
import { GeneratedType } from "../../shared/utils/generated-types";
import { DatumRange } from "../../zoeken/model/datum-range";

/**
 * @deprecated - use the `GeneratedType`
 */
export class OntkoppeldDocumentListParameters
  extends ListParameters
  implements ZoekFilters
{
  readonly filtersType = "OntkoppeldDocumentListParameters";
  zaakID: string;
  ontkoppeldDoor: GeneratedType<"RestUser"> | string;
  ontkoppeldOp = new DatumRange();
  creatiedatum = new DatumRange();
  titel: string;
  reden: string;

  constructor(sort: string, order: SortDirection) {
    super(sort, order);
  }

  static heeftActieveFilters(zoekFilters: ZoekFilters): boolean {
    if (zoekFilters.zaakID != null) {
      return true;
    }
    if (zoekFilters.ontkoppeldDoor != null) {
      return true;
    }
    if (
      zoekFilters.ontkoppeldOp?.van != null ||
      zoekFilters.ontkoppeldOp?.tot != null
    ) {
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
    if (zoekFilters.reden != null) {
      return true;
    }
    return false;
  }
}

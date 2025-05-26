/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../shared/utils/generated-types";
import { DatumRange } from "../../zoeken/model/datum-range";

export type ZoekFilters = {
  readonly filtersType: string;
  zoeken?: Record<string, unknown>;
  filters?: Record<string, undefined | { values: Array<unknown> }>;
  datums?: Record<string, DatumRange>;
  identificatie?: string;
  creatiedatum?: DatumRange;
  titel?: string;
  zaakID?: string;
  ontkoppeldDoor?: GeneratedType<"RestUser"> | string;
  ontkoppeldOp?: DatumRange;
  reden?: string;
  ontvangstdatum?: DatumRange;
  initiatorID?: string;
  type?: string;
};

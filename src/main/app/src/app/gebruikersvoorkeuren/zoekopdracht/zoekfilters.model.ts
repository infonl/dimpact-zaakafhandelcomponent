/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../shared/utils/generated-types";

export type ZoekFilters = {
  readonly filtersType:
    | "ZoekParameters"
    | "DetachedDocumentListParameters"
    | "InboxDocumentListParameters";
  zoeken?: Record<string, unknown>;
  filters?: Record<string, undefined | { values: Array<unknown> }>;
  datums?: Record<string, GeneratedType<"RestDatumRange">>;
  identificatie?: string;
  creatiedatum?: GeneratedType<"RestDatumRange">;
  titel?: string;
  zaakID?: string;
  ontkoppeldDoor?: GeneratedType<"RestUser"> | string;
  ontkoppeldOp?: GeneratedType<"RestDatumRange">;
  reden?: string;
  ontvangstdatum?: GeneratedType<"RestDatumRange">;
  initiatorID?: string;
  type?: string | null;
};

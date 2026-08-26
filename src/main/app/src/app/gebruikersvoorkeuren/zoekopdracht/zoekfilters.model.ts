/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../shared/utils/generated-types";

export type ZoekFilters = {
  readonly filtersType?:
    | "ZoekParameters"
    | "DetachedDocumentListParameters"
    | "InboxDocumentListParameters";
  zoeken?: Record<string, unknown>;
  filters?: Record<string, undefined | { values: Array<unknown> }>;
  datums?: Record<string, GeneratedType<"RestDatumRange">>;
  identificatie?: string | null;
  creatiedatum?: GeneratedType<"RestDatumRange"> | null;
  titel?: string | null;
  zaakID?: string | null;
  ontkoppeldDoor?: GeneratedType<"RestUser"> | string | null;
  ontkoppeldOp?: GeneratedType<"RestDatumRange"> | null;
  reden?: string | null;
  ontvangstdatum?: GeneratedType<"RestDatumRange"> | null;
  initiatorID?: string | null;
  type?: string | null;
};

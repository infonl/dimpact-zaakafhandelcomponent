/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Werklijst } from "./werklijst";

/**
 * @deprecated - use the `GeneratedType`
 */
export class Zoekopdracht {
  id: number;
  naam: string;
  lijstID: Werklijst;
  actief: boolean;
  creatiedatum: string;
  json: string;
}

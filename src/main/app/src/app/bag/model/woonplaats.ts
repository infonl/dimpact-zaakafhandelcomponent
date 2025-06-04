/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { BAGObject } from "./bagobject";

/**
 * @deprecated - use the `GeneratedType`
 */
export class Woonplaats extends BAGObject {
  naam: string;
  status: "AANGEWEZEN" | "INGETROKKEN";
}

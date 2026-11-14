/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ZoekObjectType } from "./zoek-object-type";

export interface ZoekObject {
  id: string;
  type: ZoekObjectType;
}

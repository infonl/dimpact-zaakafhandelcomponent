/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Resultaat } from "../../shared/model/resultaat";
import { FilterResultaat } from "./filter-resultaat";
import { FilterVeld } from "./filter-veld";

/**
 * @deprecated - use the `GeneratedType`
 */
export class ZoekResultaat<TYPE> extends Resultaat<TYPE> {
  filters: Partial<Record<FilterVeld, FilterResultaat[]>> = {};
}

/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { isInDateRange } from "../../../date-time";
import { GeneratedType } from "../../utils/generated-types";

export class ClientMatcher {
  static matchDatum(
    dataField: string,
    filterField: GeneratedType<"RestDateRange">,
  ) {
    if (!dataField) {
      return false;
    }
    return isInDateRange(dataField, filterField);
  }

  static matchBoolean(dataField: boolean, filterField: boolean) {
    return dataField === filterField;
  }

  static matchObject<T extends Record<string, unknown>>(
    key: keyof T,
    dataField?: T | null,
    filterField?: T | null,
  ) {
    return dataField?.[key] === filterField?.[key];
  }
}

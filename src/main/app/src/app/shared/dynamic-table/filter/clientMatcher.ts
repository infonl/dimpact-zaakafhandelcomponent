/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import moment from "moment";
import { GeneratedType } from "../../utils/generated-types";

export class ClientMatcher {
  static matchDatum(
    dataField: string,
    filterField: GeneratedType<"RestDateRange">,
  ) {
    if (!dataField) {
      return false;
    }
    const inputDate: moment.Moment = moment(dataField);

    if (!!filterField.from && !!filterField.through) {
      return (
        inputDate.isSameOrAfter(moment(filterField.from)) &&
        inputDate.isSameOrBefore(moment(filterField.through))
      );
    } else if (filterField.from) {
      return inputDate.isSameOrAfter(moment(filterField.from));
    } else if (filterField.through) {
      return inputDate.isSameOrBefore(moment(filterField.through));
    }
    return false;
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

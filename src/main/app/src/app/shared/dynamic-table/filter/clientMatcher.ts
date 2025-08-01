/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import moment from "moment";
import { DatumRange } from "../../../zoeken/model/datum-range";

export class ClientMatcher {
  static matchDatum(dataField: string, filterField: DatumRange) {
    if (!dataField) {
      return false;
    }
    const inputDate: moment.Moment = moment(dataField);

    if (!!filterField.van && !!filterField.tot) {
      return (
        inputDate.isSameOrAfter(moment(filterField.van)) &&
        inputDate.isSameOrBefore(moment(filterField.tot))
      );
    } else if (filterField.van) {
      return inputDate.isSameOrAfter(moment(filterField.van));
    } else if (filterField.tot) {
      return inputDate.isSameOrBefore(moment(filterField.tot));
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

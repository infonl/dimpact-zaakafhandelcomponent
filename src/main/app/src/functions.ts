/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import moment from "moment";

export function alterMoment() {
  /**
   * returning an `ISO8601` string that reflects the utcOffset
   * @source https://momentjs.com/docs/#/displaying/as-json/
   */
  moment.fn.toJSON = function () {
    return this.format();
  };
  /**
   * Prevent UTC conversion by default
   * @source https://momentjs.com/docs/#/displaying/as-iso-string/
   */
  const originalToISOString = moment.fn.toISOString;
  moment.fn.toISOString = function (keepOffset = true) {
    return originalToISOString.call(this, keepOffset);
  };
}

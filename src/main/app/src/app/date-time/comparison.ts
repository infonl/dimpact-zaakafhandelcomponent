/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import moment from "moment";
import { DateInput } from "./picked-date";

const TYPED_DAY_FORMATS = ["YYYY-MM-DD", "DD-MM-YYYY", "MM/DD/YYYY"];

function toTypedDay(value: DateInput) {
  return moment(value, TYPED_DAY_FORMATS, false);
}

export function isEarlierDay(value: DateInput, reference: DateInput) {
  return toTypedDay(value).isBefore(toTypedDay(reference), "day");
}

export function isLaterDay(value: DateInput, reference: DateInput) {
  return toTypedDay(value).isAfter(toTypedDay(reference), "day");
}

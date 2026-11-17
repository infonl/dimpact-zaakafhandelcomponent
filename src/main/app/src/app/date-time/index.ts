/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

export { fromApiDate, toApiDate, type PickedDate } from "./api-date";
export {
  addDays,
  daysBetween,
  isAfter,
  isPickedDate,
  isSameOrAfter,
  now,
  startOfDay,
  toApiDateTime,
  today,
  toPickedDate,
  type DateInput,
} from "./picked-date";
export {
  FILTER_DATE_FORMATS,
  ZAC_DATE_FORMATS,
  provideFilterDateFormats,
  provideZacDateAdapter,
} from "./date-adapter";

/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import moment from "moment";
import { PickedDate } from "./api-date";

export type DateInput = PickedDate | string;

const API_DATE = /^\d{4}-\d{2}-\d{2}$/;

function toMoment(value: DateInput) {
  if (typeof value !== "string") return moment(value);
  return API_DATE.test(value)
    ? moment(value, "YYYY-MM-DD", true)
    : moment(value, moment.ISO_8601, true);
}

export function isPickedDate(value: unknown): value is PickedDate {
  return value instanceof Date || moment.isMoment(value);
}

export function now(): PickedDate {
  return moment();
}

export function today(): PickedDate {
  return moment().startOf("day");
}

export function toPickedDate(
  value: DateInput | null | undefined,
): PickedDate | null {
  if (value === null || value === undefined || value === "") return null;
  const date = toMoment(value);
  return date.isValid() ? date : null;
}

export function startOfDay(value: DateInput): PickedDate {
  return toMoment(value).startOf("day");
}

export function addDays(value: DateInput, days: number): PickedDate {
  return toMoment(value).add(days, "days");
}

export function daysBetween(from: DateInput, to: DateInput) {
  return toMoment(to).diff(toMoment(from), "days");
}

export function isAfter(value: DateInput, other: DateInput, unit?: "day") {
  return toMoment(value).isAfter(toMoment(other), unit);
}

export function isSameOrAfter(
  value: DateInput,
  other: DateInput,
  unit?: "day",
) {
  return toMoment(value).isSameOrAfter(toMoment(other), unit);
}

export function toApiDateTime(value: PickedDate | null | undefined) {
  if (!value) return null;
  const date = moment(value);
  return date.isValid() ? date.format() : null;
}

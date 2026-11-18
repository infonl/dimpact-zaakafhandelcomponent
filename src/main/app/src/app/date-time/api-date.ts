/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import moment, { Moment } from "moment";

export type PickedDate = Date | Moment;

const API_DATE = /^(\d{4})-(\d{2})-(\d{2})$/;

const pad = (value: number) => String(value).padStart(2, "0");

export function toApiDate(value: PickedDate | null | undefined) {
  if (!value) return null;
  if (moment.isMoment(value)) {
    return value.isValid() ? value.format("YYYY-MM-DD") : null;
  }
  if (Number.isNaN(value.getTime())) return null;
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`;
}

export function fromApiDate(value: string | null | undefined) {
  if (!value) return null;
  const apiDate = API_DATE.exec(value);
  if (apiDate) {
    const [, year, month, day] = apiDate.map(Number);
    return new Date(year, month - 1, day);
  }
  const instant = moment(value, moment.ISO_8601, true);
  if (!instant.isValid()) return null;
  const localDate = instant.toDate();
  return new Date(
    localDate.getFullYear(),
    localDate.getMonth(),
    localDate.getDate(),
  );
}

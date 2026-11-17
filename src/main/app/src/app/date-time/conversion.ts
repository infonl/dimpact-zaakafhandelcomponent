/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import moment from "moment";
import { PickedDate } from "./api-date";

export function fromTimestamp(milliseconds: number): PickedDate {
  return moment(milliseconds);
}

export function toFormDataDateTime(value: string) {
  return moment(value).format("YYYY-MM-DDThh:mmZ");
}

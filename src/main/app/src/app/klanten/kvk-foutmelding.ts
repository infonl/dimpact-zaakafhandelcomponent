/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";

export function toKvkFoutmelding(error: HttpErrorResponse | null) {
  if (!error) return null;
  return error.status === 404
    ? "msg.hint.kvk.outdated"
    : "msg.hint.kvk.other-error";
}

/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import { TranslateService } from "@ngx-translate/core";

export function toDialogErrorMessage(
  translateService: TranslateService,
  error: unknown,
): string {
  const message =
    error instanceof HttpErrorResponse
      ? (error.error?.message ?? error.message)
      : null;
  return translateService.instant(message || "dialoog.error.body.technisch");
}

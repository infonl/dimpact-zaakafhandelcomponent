/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import { Injectable, inject } from "@angular/core";
import { CreateMutationOptions } from "@tanstack/angular-query-experimental";
import { PathParameters } from "../shared/http/http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";
import { GeneratedType } from "../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class CsvService {
  private readonly zacQueryClient = inject(ZacQueryClient);

  exportToCSV() {
    return this.zacQueryClient.POST("/rest/csv/export", {
      responseType: "blob",
    } as PathParameters<"/rest/csv/export", "post"> &
      Record<string, unknown>) as unknown as CreateMutationOptions<
      Blob,
      HttpErrorResponse,
      GeneratedType<"RestZoekParameters">,
      void
    >;
  }
}

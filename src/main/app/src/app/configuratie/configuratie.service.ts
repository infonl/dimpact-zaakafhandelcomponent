/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { queryOptions } from "@tanstack/angular-query-experimental";
import { StaleTimes, ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class ConfiguratieService {
  private readonly zacQueryClient = inject(ZacQueryClient);

  listTalen() {
    return queryOptions({
      ...this.zacQueryClient.GET("/rest/configuratie/talen"),
      staleTime: "static", // deploy-time configuration, never refetch or invalidate
    });
  }

  readDefaultTaal() {
    return queryOptions({
      ...this.zacQueryClient.GET("/rest/configuratie/talen/default"),
      staleTime: StaleTimes.Infinite,
      gcTime: StaleTimes.Infinite,
    });
  }

  readMaxFileSizeMB() {
    return queryOptions({
      ...this.zacQueryClient.GET("/rest/configuratie/max-file-size-mb"),
      staleTime: "static", // deploy-time configuration, never refetch or invalidate
    });
  }

  readAllowedFileTypesQuery() {
    return queryOptions({
      ...this.zacQueryClient.GET("/rest/configuratie/file-types"),
      staleTime: "static", // deploy-time configuration, never refetch or invalidate
    });
  }

  readGemeenteCode() {
    return queryOptions({
      ...this.zacQueryClient.GET("/rest/configuratie/gemeente/code"),
      staleTime: "static", // deploy-time configuration, never refetch or invalidate
    });
  }

  readBrpDoelbindingSetupEnabled() {
    return queryOptions({
      ...this.zacQueryClient.GET(
        "/rest/configuratie/brp/doelbinding-setup-enabled",
      ),
      staleTime: "static", // deploy-time configuration, never refetch or invalidate
    });
  }
}

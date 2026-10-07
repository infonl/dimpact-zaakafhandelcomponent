/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class IndexingService {
  private readonly zacQueryClient = inject(ZacQueryClient);

  commitPendingChangesToSearchIndex() {
    return this.zacQueryClient.POST(
      "/rest/indexeren/commit-pending-changes-to-search-index",
    );
  }
}

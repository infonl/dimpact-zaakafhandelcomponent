/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ZacHttpClient } from "../shared/http/zac-http-client";

@Injectable({
  providedIn: "root",
})
export class IndexingService {
  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  commitPendingChangesToSearchIndex() {
    return this.zacHttpClient.POST(
      "/rest/indexeren/commit-pending-changes-to-search-index",
      undefined as never,
      {},
    );
  }
}

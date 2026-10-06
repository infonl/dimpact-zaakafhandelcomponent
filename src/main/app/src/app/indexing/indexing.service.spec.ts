/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { sleep, testQueryClient } from "../../../setupJest";
import { runMutation } from "../shared/http/run-mutation";
import { IndexingService } from "./indexing.service";

describe(IndexingService.name, () => {
  let service: IndexingService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
    });

    service = TestBed.inject(IndexingService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  describe("commitPendingChangesToSearchIndex", () => {
    it("posts the commit without a body", async () => {
      runMutation(
        testQueryClient,
        service.commitPendingChangesToSearchIndex(),
        undefined as never,
      ).subscribe();
      await sleep();

      const request = httpTestingController.expectOne(
        "/rest/indexeren/commit-pending-changes-to-search-index",
      );
      expect(request.request.method).toBe("POST");
      expect(request.request.body).toBeNull();
      request.flush(null);
    });
  });
});

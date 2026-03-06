/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { convertToParamMap } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { fromPartial } from "@total-typescript/shoehorn";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { KlantenService } from "../klanten.service";
import { PersoonResolverService } from "./persoon-resolver.service";

describe(PersoonResolverService.name, () => {
  let persoonResolverService: PersoonResolverService;
  let foutAfhandelingService: FoutAfhandelingService;
  let queryClient: QueryClient;

  const temporaryPersonId = "1438529a-eb41-4ff9-ac98-8c1f18892b7a";

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        PersoonResolverService,
        FoutAfhandelingService,
        KlantenService,
        QueryClient,
        provideHttpClient(withInterceptorsFromDi()),
      ],
      imports: [TranslateModule.forRoot()],
    });

    persoonResolverService = TestBed.inject(PersoonResolverService);

    foutAfhandelingService = TestBed.inject(FoutAfhandelingService);
    jest
      .spyOn(foutAfhandelingService, "httpErrorAfhandelen")
      .mockImplementation();

    TestBed.inject(KlantenService);
    queryClient = TestBed.inject(QueryClient);
    jest
      .spyOn(queryClient, "ensureQueryData")
      .mockResolvedValue(
        fromPartial<GeneratedType<"RestPersoon">>({ indicaties: [] }),
      );
  });

  describe(PersoonResolverService.prototype.resolve.name, () => {
    it("should call ensureQueryData with the correct temporaryPersonId", async () => {
      await persoonResolverService.resolve(
        fromPartial({
          get paramMap() {
            return convertToParamMap({ temporaryPersonId });
          },
        }),
      );

      expect(queryClient.ensureQueryData).toHaveBeenCalledWith(
        expect.objectContaining({
          queryKey: expect.arrayContaining([
            "/rest/klanten/person/{temporaryPersonId}",
            expect.objectContaining({
              path: { temporaryPersonId },
            }),
          ]),
        }),
      );
    });

    it("should handle retry logic and call error handler on final failure", async () => {
      const mockError = new Error("Network error");

      jest
        .spyOn(queryClient, "ensureQueryData")
        .mockImplementation((options) => {
          // Simulate retry callback
          if (typeof options.retry === "function") {
            // First retry - should return true
            expect(options.retry(0, mockError)).toBe(true);
            // Final retry - should return false and call error handler
            expect(options.retry(3, mockError)).toBe(false);
          }
          return Promise.resolve(
            fromPartial<GeneratedType<"RestPersoon">>({ indicaties: [] }),
          );
        });

      await persoonResolverService.resolve(
        fromPartial({
          get paramMap() {
            return convertToParamMap({ temporaryPersonId });
          },
        }),
      );

      expect(foutAfhandelingService.httpErrorAfhandelen).toHaveBeenCalledWith(
        mockError,
      );
    });
  });
});

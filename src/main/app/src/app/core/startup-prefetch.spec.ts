/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ApplicationInitStatus } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import {
  provideTanStackQuery,
  QueryClient,
} from "@tanstack/angular-query-experimental";
import { waitFor } from "@testing-library/angular";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../setupJest";
import { ConfiguratieService } from "../configuratie/configuratie.service";
import { provideStartupPrefetch } from "./startup-prefetch";

describe(provideStartupPrefetch.name, () => {
  const allowedFileTypesQueryKey = ["/rest/configuratie/file-types"];

  const setUp = (queryFn: () => Promise<unknown>) => {
    TestBed.configureTestingModule({
      providers: [
        provideTanStackQuery(testQueryClient),
        provideStartupPrefetch(),
        {
          provide: ConfiguratieService,
          useValue: fromPartial<ConfiguratieService>({
            readAllowedFileTypesQuery: () =>
              fromPartial({ queryKey: allowedFileTypesQueryKey, queryFn }),
          }),
        },
      ],
    });
    return TestBed.inject(ApplicationInitStatus).donePromise;
  };

  it("should fetch the allowed file types when the application starts", async () => {
    const allowedFileTypes = [{ extension: ".txt", mediaType: "text/plain" }];
    await setUp(() => Promise.resolve(allowedFileTypes));

    await waitFor(() =>
      expect(
        TestBed.inject(QueryClient).getQueryData(allowedFileTypesQueryKey),
      ).toEqual(allowedFileTypes),
    );
  });

  it("should not wait for the allowed file types before the application starts", async () => {
    let isInitialised = false;
    await setUp(() => new Promise(() => {})).then(() => {
      isInitialised = true;
    });

    expect(isInitialised).toBe(true);
  });

  it("should still start the application when the allowed file types fail to load", async () => {
    await expect(
      setUp(() => Promise.reject(new Error("fakeNetworkFailure"))),
    ).resolves.not.toThrow();
  });
});

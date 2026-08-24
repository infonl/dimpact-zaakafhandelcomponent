/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { of } from "rxjs";
import { fromPartial } from "../../../test-helpers";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { QUERY_CLIENT } from "./query-client";

describe("QUERY_CLIENT", () => {
  const foutAfhandelen = jest.fn().mockReturnValue(of());
  const error = new HttpErrorResponse({ status: 500 });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: FoutAfhandelingService,
          useValue: fromPartial<FoutAfhandelingService>({ foutAfhandelen }),
        },
      ],
    });
  });

  it("reports a failed read through the error handling", async () => {
    const queryClient = TestBed.inject(QUERY_CLIENT);

    await expect(
      queryClient.fetchQuery({
        queryKey: ["fakeEndpoint"],
        queryFn: () => Promise.reject(error),
        retry: false,
      }),
    ).rejects.toBe(error);

    expect(foutAfhandelen).toHaveBeenCalledWith(error);
  });

  it("reports a read that has given up once, not once per retry", async () => {
    const queryClient = TestBed.inject(QUERY_CLIENT);

    await expect(
      queryClient.fetchQuery({
        queryKey: ["fakeEndpoint"],
        queryFn: () => Promise.reject(error),
        retry: 2,
        retryDelay: 0,
      }),
    ).rejects.toBe(error);

    expect(foutAfhandelen).toHaveBeenCalledTimes(1);
  });

  it("reports nothing for a read that succeeds", async () => {
    const queryClient = TestBed.inject(QUERY_CLIENT);

    await queryClient.fetchQuery({
      queryKey: ["fakeEndpoint"],
      queryFn: () => Promise.resolve("fakeResponse"),
    });

    expect(foutAfhandelen).not.toHaveBeenCalled();
  });
});

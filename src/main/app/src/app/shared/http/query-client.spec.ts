/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { of } from "rxjs";
import { fromPartial } from "../../../test-helpers";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { HttpParamsError } from "./http-client";
import { QUERY_CLIENT } from "./query-client";

describe("QUERY_CLIENT", () => {
  const foutAfhandelen = jest.fn().mockReturnValue(of());
  const error = new HttpErrorResponse({ status: 500 });

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        {
          provide: FoutAfhandelingService,
          useValue: fromPartial<FoutAfhandelingService>({
            foutAfhandelen,
          }),
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

  it("reports nothing for a read that has already reported its own failure", async () => {
    const queryClient = TestBed.inject(QUERY_CLIENT);
    const alreadyReported = "De server heeft code 500 geretourneerd.";

    await expect(
      queryClient.fetchQuery({
        queryKey: ["fakeEndpoint"],
        queryFn: () => Promise.reject(alreadyReported),
        retry: false,
      }),
    ).rejects.toBe(alreadyReported);

    expect(foutAfhandelen).not.toHaveBeenCalled();
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

  it("reports nothing for a missing path parameter, since that is a programming error the user cannot act on", async () => {
    const queryClient = TestBed.inject(QUERY_CLIENT);
    const httpParamsError = new HttpParamsError("fakeMissingParameter");

    await expect(
      queryClient.fetchQuery({
        queryKey: ["fakeEndpoint"],
        queryFn: () => Promise.reject(httpParamsError),
        retry: false,
      }),
    ).rejects.toBe(httpParamsError);

    expect(foutAfhandelen).not.toHaveBeenCalled();
    expect(console.error).not.toHaveBeenCalled();
  });

  it("reports nothing for a read that says it handles its own failure", async () => {
    const queryClient = TestBed.inject(QUERY_CLIENT);

    await expect(
      queryClient.fetchQuery({
        queryKey: ["fakeEndpoint"],
        queryFn: () => Promise.reject(error),
        retry: false,
        meta: { reportErrors: false },
      }),
    ).rejects.toBe(error);

    expect(foutAfhandelen).not.toHaveBeenCalled();
    expect(console.error).not.toHaveBeenCalled();
  });

  it("only logs a failed refetch to the console, so a background poll neither closes a dialog the user is in nor interrupts them", async () => {
    const queryClient = TestBed.inject(QUERY_CLIENT);
    const queryKey = ["fakeEndpoint"];

    await queryClient.fetchQuery({
      queryKey,
      queryFn: () => Promise.resolve("fakeResponse"),
    });
    await expect(
      queryClient.fetchQuery({
        queryKey,
        queryFn: () => Promise.reject(error),
        retry: false,
        staleTime: 0,
      }),
    ).rejects.toBe(error);

    expect(foutAfhandelen).not.toHaveBeenCalled();
    expect(console.error).toHaveBeenCalledWith(error);
  });

  it("reports a refetch that fails because the session expired through the error handling, so the user is sent to log in", async () => {
    const queryClient = TestBed.inject(QUERY_CLIENT);
    const queryKey = ["fakeEndpoint"];
    const loggedOut = new HttpErrorResponse({
      status: 0,
      url: "https://example.com/rest/fakeEndpoint",
    });

    await queryClient.fetchQuery({
      queryKey,
      queryFn: () => Promise.resolve("fakeResponse"),
    });
    await expect(
      queryClient.fetchQuery({
        queryKey,
        queryFn: () => Promise.reject(loggedOut),
        retry: false,
        staleTime: 0,
      }),
    ).rejects.toBe(loggedOut);

    expect(foutAfhandelen).toHaveBeenCalledWith(loggedOut);
    expect(console.error).not.toHaveBeenCalled();
  });
});

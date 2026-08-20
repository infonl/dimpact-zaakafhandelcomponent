/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import {
  type CreateMutationOptions,
  type DefaultError,
  provideQueryClient,
} from "@tanstack/angular-query-experimental";
import { testQueryClient } from "../../../../setupJest";
import { injectMutation } from "./inject-mutation";

describe(injectMutation.name, () => {
  const calls: string[] = [];

  beforeEach(() => {
    calls.length = 0;
    TestBed.configureTestingModule({
      providers: [provideQueryClient(testQueryClient)],
    });
  });

  function injectWithOverrides(
    options: CreateMutationOptions<string, DefaultError, string, unknown>,
    overrides?: Omit<
      CreateMutationOptions<string, DefaultError, string, unknown>,
      "mutationFn" | "mutationKey"
    >,
  ) {
    return TestBed.runInInjectionContext(() =>
      injectMutation(() => options, overrides),
    );
  }

  const succeedingService: CreateMutationOptions<
    string,
    DefaultError,
    string,
    unknown
  > = {
    mutationFn: (variables) => Promise.resolve(`fakeResponse-${variables}`),
    onSuccess: () => void calls.push("service onSuccess"),
  };

  const failingService: CreateMutationOptions<
    string,
    DefaultError,
    string,
    unknown
  > = {
    mutationFn: () => Promise.reject(new Error("fakeError")),
    onError: () => void calls.push("service onError"),
  };

  it("mutates with the variables the caller passes and returns the response", async () => {
    const mutation = injectWithOverrides(succeedingService);

    await expect(mutation.mutateAsync("fakeVariables")).resolves.toBe(
      "fakeResponse-fakeVariables",
    );
  });

  it("runs both onSuccess handlers, the service first", async () => {
    const mutation = injectWithOverrides(succeedingService, {
      onSuccess: () => void calls.push("caller onSuccess"),
    });

    await mutation.mutateAsync("fakeVariables");

    expect(calls).toEqual(["service onSuccess", "caller onSuccess"]);
  });

  it("keeps the error reporting of the service when the caller adds its own", async () => {
    const mutation = injectWithOverrides(failingService, {
      onError: () => void calls.push("caller onError"),
    });

    await expect(mutation.mutateAsync("fakeVariables")).rejects.toThrow(
      "fakeError",
    );

    expect(calls).toEqual(["service onError", "caller onError"]);
  });

  it("reports through the service handler when the caller has none", async () => {
    const mutation = injectWithOverrides(failingService);

    await expect(mutation.mutateAsync("fakeVariables")).rejects.toThrow(
      "fakeError",
    );

    expect(calls).toEqual(["service onError"]);
  });
});

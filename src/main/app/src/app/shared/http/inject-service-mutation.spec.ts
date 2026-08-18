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
import { injectServiceMutation } from "./inject-service-mutation";

describe(injectServiceMutation.name, () => {
  const calls: string[] = [];

  beforeEach(() => {
    calls.length = 0;
    TestBed.configureTestingModule({
      providers: [provideQueryClient(testQueryClient)],
    });
  });

  afterEach(() => {
    testQueryClient.clear();
  });

  function injectWithOverrides(
    serviceOptions: CreateMutationOptions<string, DefaultError, void, unknown>,
    overrides?: CreateMutationOptions<string, DefaultError, void, unknown>,
  ) {
    return TestBed.runInInjectionContext(() =>
      injectServiceMutation(() => serviceOptions, overrides),
    );
  }

  const failingService: CreateMutationOptions<
    string,
    DefaultError,
    void,
    unknown
  > = {
    mutationFn: () => Promise.reject(new Error("fakeError")),
    onError: () => void calls.push("service onError"),
  };

  describe("onError", () => {
    it("reports through the service handler when the caller has none", async () => {
      const mutation = injectWithOverrides(failingService);

      await expect(mutation.mutateAsync()).rejects.toThrow("fakeError");

      expect(calls).toEqual(["service onError"]);
    });

    it("lets a caller handler replace the service handler", async () => {
      const mutation = injectWithOverrides(failingService, {
        onError: () => void calls.push("caller onError"),
      });

      await expect(mutation.mutateAsync()).rejects.toThrow("fakeError");

      expect(calls).toEqual(["caller onError"]);
    });
  });

  describe("onSuccess", () => {
    it("runs both handlers, the service first", async () => {
      const mutation = injectWithOverrides(
        {
          mutationFn: () => Promise.resolve("fakeData"),
          onSuccess: () => void calls.push("service onSuccess"),
        },
        { onSuccess: () => void calls.push("caller onSuccess") },
      );

      await mutation.mutateAsync();

      expect(calls).toEqual(["service onSuccess", "caller onSuccess"]);
    });
  });
});

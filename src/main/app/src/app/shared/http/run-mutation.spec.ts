/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { mutationOptions } from "@tanstack/angular-query-experimental";
import { firstValueFrom } from "rxjs";
import { testQueryClient } from "../../../../setupJest";
import { runMutation } from "./run-mutation";

describe(runMutation.name, () => {
  describe("a mutation handed to a dialog", () => {
    it("does not run until something subscribes", () => {
      const mutationFn = jest.fn().mockResolvedValue("fakeResponse");

      runMutation(
        testQueryClient,
        mutationOptions({ mutationKey: ["fakeKey"], mutationFn }),
        "fakeVariables",
      );

      expect(mutationFn).not.toHaveBeenCalled();
    });

    it("runs the request with the variables it was given", async () => {
      const mutationFn = jest.fn().mockResolvedValue("fakeResponse");

      const response = await firstValueFrom(
        runMutation(
          testQueryClient,
          mutationOptions({ mutationKey: ["fakeKey"], mutationFn }),
          "fakeVariables",
        ),
      );

      expect(response).toBe("fakeResponse");
      expect(mutationFn).toHaveBeenCalledWith(
        "fakeVariables",
        expect.objectContaining({
          client: testQueryClient,
          mutationKey: ["fakeKey"],
        }),
      );
    });

    it("runs the onSuccess the service attached to the mutation", async () => {
      const onSuccess = jest.fn();

      await firstValueFrom(
        runMutation(
          testQueryClient,
          mutationOptions({
            mutationKey: ["fakeKey"],
            mutationFn: () => Promise.resolve("fakeResponse"),
            onSuccess,
          }),
          "fakeVariables",
        ),
      );

      expect(onSuccess).toHaveBeenCalled();
    });

    it("runs the onError the client attached to the mutation and fails the observable", async () => {
      const onError = jest.fn();
      const error = new Error("fakeError");

      const request = firstValueFrom(
        runMutation(
          testQueryClient,
          mutationOptions({
            mutationKey: ["fakeKey"],
            mutationFn: () => Promise.reject(error),
            onError,
            retry: false,
          }),
          "fakeVariables",
        ),
      );

      await expect(request).rejects.toThrow("fakeError");
      expect(onError).toHaveBeenCalledWith(
        error,
        "fakeVariables",
        undefined,
        expect.anything(),
      );
    });
  });
});

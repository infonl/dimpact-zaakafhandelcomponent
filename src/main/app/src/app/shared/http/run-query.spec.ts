/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { queryOptions } from "@tanstack/angular-query-experimental";
import { firstValueFrom } from "rxjs";
import { testQueryClient } from "../../../../setupJest";
import { runQuery } from "./run-query";

describe(runQuery.name, () => {
  describe("a query reported to a caller that is not a component", () => {
    it("does not run until something subscribes", () => {
      const queryFn = jest.fn().mockResolvedValue("fakeResponse");

      runQuery(
        testQueryClient,
        queryOptions({ queryKey: ["fakeKey"], queryFn }),
      );

      expect(queryFn).not.toHaveBeenCalled();
    });

    it("reports what the query resolves to", async () => {
      const response = await firstValueFrom(
        runQuery(
          testQueryClient,
          queryOptions({
            queryKey: ["fakeKey"],
            queryFn: () => Promise.resolve("fakeResponse"),
          }),
        ),
      );

      expect(response).toBe("fakeResponse");
    });

    it("shares the cache with the components, so a fresh result is not fetched twice", async () => {
      const queryFn = jest.fn().mockResolvedValue("fakeResponse");
      const options = queryOptions({
        queryKey: ["fakeKey"],
        queryFn,
        staleTime: Infinity,
      });

      await firstValueFrom(runQuery(testQueryClient, options));
      await firstValueFrom(runQuery(testQueryClient, options));

      expect(queryFn).toHaveBeenCalledTimes(1);
    });

    it("fails the observable when the request fails", async () => {
      const request = firstValueFrom(
        runQuery(
          testQueryClient,
          queryOptions({
            queryKey: ["fakeKey"],
            queryFn: () => Promise.reject(new Error("fakeError")),
            retry: false,
          }),
        ),
      );

      await expect(request).rejects.toThrow("fakeError");
    });
  });
});

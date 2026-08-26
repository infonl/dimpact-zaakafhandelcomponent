/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  type CreateMutationOptions,
  type DefaultError,
  type MutationFunctionContext,
  QueryClient,
} from "@tanstack/angular-query-experimental";
import { fromPartial } from "../../../test-helpers";
import { mergeMutationOptions } from "./merge-mutation-options";

describe(mergeMutationOptions.name, () => {
  const mutationContext: MutationFunctionContext = {
    client: new QueryClient(),
    meta: undefined,
  };
  const calls: string[] = [];

  beforeEach(() => {
    calls.length = 0;
  });

  const base: CreateMutationOptions<string, DefaultError, void, string> = {
    mutationFn: () => Promise.resolve("data"),
    onMutate: () => {
      calls.push("base onMutate");
      return "base context";
    },
    onError: () => void calls.push("base onError"),
    onSuccess: () => void calls.push("base onSuccess"),
    onSettled: () => void calls.push("base onSettled"),
  };

  it("should run both callbacks, base first", async () => {
    const merged = mergeMutationOptions(base, {
      onError: () => void calls.push("override onError"),
      onSuccess: () => void calls.push("override onSuccess"),
      onSettled: () => void calls.push("override onSettled"),
    });

    await merged.onError?.(
      new Error("fake"),
      undefined,
      undefined,
      mutationContext,
    );
    await merged.onSuccess?.(
      "data",
      undefined,
      "base context",
      mutationContext,
    );
    await merged.onSettled?.(
      "data",
      null,
      undefined,
      "base context",
      mutationContext,
    );

    expect(calls).toEqual([
      "base onError",
      "override onError",
      "base onSuccess",
      "override onSuccess",
      "base onSettled",
      "override onSettled",
    ]);
  });

  it("should keep the base callbacks when the overrides define none", async () => {
    const merged = mergeMutationOptions(base, {});

    await merged.onError?.(
      new Error("fake"),
      undefined,
      undefined,
      mutationContext,
    );

    expect(calls).toEqual(["base onError"]);
  });

  it("should let the outermost onMutate context win", async () => {
    const merged = mergeMutationOptions(base, {
      onMutate: () => {
        calls.push("override onMutate");
        return "override context";
      },
    });

    const context = await merged.onMutate?.(undefined, mutationContext);

    expect(calls).toEqual(["base onMutate", "override onMutate"]);
    expect(context).toBe("override context");
  });

  it("should type the callbacks of the overrides from the base", () => {
    const typedBase = fromPartial<
      CreateMutationOptions<
        { name: string },
        DefaultError,
        { id: number },
        void
      >
    >({});
    let seen: string | undefined;

    const merged = mergeMutationOptions(typedBase, {
      // annotating these would hide a regression: they must be inferred
      onSuccess: (data, variables) => {
        const name: string = data.name;
        const id: number = variables.id;
        seen = `${name}-${id}`;
      },
    });

    void merged.onSuccess?.(
      { name: "fakeName" },
      { id: 1 },
      undefined,
      mutationContext,
    );

    expect(seen).toBe("fakeName-1");
  });

  it("should keep the meta of whichever layer defines one", () => {
    const merged = mergeMutationOptions(
      { ...base, meta: { reportErrors: false } },
      {},
    );

    expect(merged.meta).toEqual({ reportErrors: false });
  });

  it("should warn that a meta entry of the overrides replaces that of the mutation", () => {
    const warn = jest.spyOn(console, "warn").mockImplementation(() => {});

    const merged = mergeMutationOptions(
      { ...base, meta: { reportErrors: false } },
      { meta: { reportErrors: true } },
    );

    expect(merged.meta).toEqual({ reportErrors: true });
    expect(warn).toHaveBeenCalledWith(
      expect.stringContaining("meta.reportErrors"),
      { mutation: false, overrides: true },
    );

    warn.mockRestore();
  });

  it("should not warn about a meta entry both layers give the same value", () => {
    const warn = jest.spyOn(console, "warn").mockImplementation(() => {});

    mergeMutationOptions(
      { ...base, meta: { reportErrors: false } },
      { meta: { reportErrors: false } },
    );

    expect(warn).not.toHaveBeenCalled();

    warn.mockRestore();
  });

  it("should warn that the onMutate context of the mutation is dropped", async () => {
    const warn = jest.spyOn(console, "warn").mockImplementation(() => {});

    const merged = mergeMutationOptions(base, {
      onMutate: () => "override context",
    });
    await merged.onMutate?.(undefined, mutationContext);

    expect(warn).toHaveBeenCalledWith(
      expect.stringContaining("onMutate context"),
      { mutation: "base context", overrides: "override context" },
    );

    warn.mockRestore();
  });
});

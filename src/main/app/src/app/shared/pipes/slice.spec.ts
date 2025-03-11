/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SlicePipe } from "./slice.pipe";

describe("SlicePipe", () => {
  let pipe: SlicePipe;

  beforeEach(() => {
    pipe = new SlicePipe();
  });

  it("should slice the string correctly", () => {
    const value = "Hello, World!";
    expect(pipe.transform(value, 0, 5)).toBe("Hello");
    expect(pipe.transform(value, 7)).toBe("World!");
    expect(pipe.transform(value, -6)).toBe("World!");
    expect(pipe.transform(value, 0, -1)).toBe("Hello, World");
  });

  it("should return the value if it is not a string", () => {
    expect(pipe.transform(123 as any, 0, 2)).toBe(123);
    expect(pipe.transform(null as any, 0, 2)).toBe(null);
    expect(pipe.transform(undefined as any, 0, 2)).toBe(undefined);
  });
});

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

  it.each([
    ["Hello, World!", 0, 5, "Hello"],
    ["Hello, World!", 7, undefined, "World!"],
    ["Hello, World!", -6, undefined, "World!"],
    ["Hello, World!", 0, -1, "Hello, World"],
  ])(
    "should transform %p with start %p and end %p to %p",
    (value, start, end, expected) => {
      expect(pipe.transform(value, start, end)).toBe(expected);
    },
  );
});

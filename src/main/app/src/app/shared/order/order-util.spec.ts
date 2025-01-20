/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { OrderUtil } from "./order-util";

describe(OrderUtil.name, () => {
  describe(OrderUtil.orderBy.name, () => {
    it.each([
      [
        ["a", "b"],
        ["a", "b"],
      ],
      [
        ["b", "a"],
        ["a", "b"],
      ],
      [
        [1, 2],
        [1, 2],
      ],
      [
        [2, 1],
        [1, 2],
      ],
    ])(
      "Will sort %p to %p when no key is provided",
      (input: (string | number)[], expected) => {
        const result = input.sort(OrderUtil.orderBy());

        expect(result).toEqual(expected);
      },
    );
  });

  it.each([
    [
      [
        { a: "foo", b: 1 },
        { a: "bar", b: 3 },
        { a: "baz", b: 2 },
      ],
      [
        { a: "bar", b: 3 },
        { a: "baz", b: 2 },
        { a: "foo", b: 1 },
      ],
      "a",
    ],
    [
      [
        { a: "foo", b: 1 },
        { a: "bar", b: 3 },
        { a: "baz", b: 2 },
      ],
      [
        { a: "foo", b: 1 },
        { a: "baz", b: 2 },
        { a: "bar", b: 3 },
      ],
      "b",
    ],
  ])(
    "Will sort %p to %p when sorting by %s",
    (input, expected, sortKey: "a" | "b") => {
      const result = input.sort(OrderUtil.orderBy(sortKey));

      expect(result).toEqual(expected);
    },
  );

  describe(OrderUtil.orderAsIs.name, () => {
      it('will return the array as is', () => {
          const result = [1,2,3].sort(OrderUtil.orderAsIs());

          expect(result).toEqual([1,2,3]);
      })
  });
});

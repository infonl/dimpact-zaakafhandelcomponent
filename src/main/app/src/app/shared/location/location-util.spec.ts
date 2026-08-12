/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../utils/generated-types";
import { LocationUtil } from "./location-util";

describe(LocationUtil.isSameGeometry.name, () => {
  const makePoint = (
    latitude: number,
    longitude: number,
  ): GeneratedType<"RestGeometry"> => ({
    type: "POINT",
    point: { latitude, longitude },
  });

  it.each([
    [makePoint(52.1, 4.3), makePoint(52.1, 4.3), true],
    [makePoint(52.1, 4.3), makePoint(53.1, 4.3), false],
    [makePoint(52.1, 4.3), makePoint(52.1, 5.4), false],
    [makePoint(52.1, 4.3), undefined, false],
    [undefined, null, false],
    [null, undefined, false],
    [undefined, undefined, false],
    [makePoint(52.1, 4.3), { type: "POLYGON" as const, polygon: [] }, false],
    [{ type: "POINT" as const }, { type: "POINT" as const }, true],
    [{ type: "POINT" as const }, makePoint(52.1, 4.3), false],
    [
      { type: "POLYGON" as const, polygon: [] },
      { type: "POLYGON" as const, polygon: [] },
      false,
    ],
    [
      { type: "GEOMETRY_COLLECTION" as const, geometries: [] },
      { type: "GEOMETRY_COLLECTION" as const, geometries: [] },
      false,
    ],
  ])("geometry %p and %p are the same: %p", (left, right, expected) => {
    expect(LocationUtil.isSameGeometry(left, right)).toBe(expected);
  });
});

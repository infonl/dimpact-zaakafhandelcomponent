/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeometryType } from "../../zaken/model/geometryType";
import { GeneratedType } from "../utils/generated-types";
import { LocationUtil } from "./location-util";

describe(LocationUtil.isSameGeometry.name, () => {
  const makePoint = (
    latitude: number,
    longitude: number,
  ): GeneratedType<"RestGeometry"> => ({
    type: GeometryType.POINT,
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
    [makePoint(52.1, 4.3), { type: GeometryType.POLYGON, polygon: [] }, false],
    [{ type: GeometryType.POINT }, { type: GeometryType.POINT }, true],
    [{ type: GeometryType.POINT }, makePoint(52.1, 4.3), false],
    [
      { type: GeometryType.POLYGON, polygon: [] },
      { type: GeometryType.POLYGON, polygon: [] },
      false,
    ],
    [
      { type: GeometryType.GEOMETRY_COLLECTION, geometries: [] },
      { type: GeometryType.GEOMETRY_COLLECTION, geometries: [] },
      false,
    ],
  ])("geometry %p and %p are the same: %p", (left, right, expected) => {
    expect(LocationUtil.isSameGeometry(left, right)).toBe(expected);
  });
});

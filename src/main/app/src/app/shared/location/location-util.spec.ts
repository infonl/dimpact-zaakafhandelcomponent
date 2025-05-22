/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { LocationUtil } from "./location-util";
import { GeometryType } from "../../zaken/model/geometryType";
import { GeneratedType } from "../utils/generated-types";

describe(LocationUtil.isSameGeometry.name, () => {
  const makePoint = (
    lat: number,
    lon: number,
  ): GeneratedType<"RestGeometry"> => ({
    type: GeometryType.POINT,
    point: { latitude: lat, longitude: lon },
  });

  it.each([
    ["same POINT geometry", makePoint(52.1, 4.3), makePoint(52.1, 4.3), true],
    [
      "different POINT latitude",
      makePoint(52.1, 4.3),
      makePoint(53.1, 4.3),
      false,
    ],
    [
      "different POINT longitude",
      makePoint(52.1, 4.3),
      makePoint(52.1, 5.4),
      false,
    ],
    ["one POINT is undefined", makePoint(52.1, 4.3), undefined, false],
    ["undefined and null", undefined, null, false],
    ["null and undefine", null, undefined, false],
    ["both undefined", undefined, undefined, false],
    [
      "different types (POINT vs POLYGON)",
      makePoint(52.1, 4.3),
      { type: GeometryType.POLYGON, polygon: [] },
      false,
    ],
    [
      "POINT with missing point property",
      { type: GeometryType.POINT },
      { type: GeometryType.POINT },
      true,
    ],
    [
      "POINT with one missing point property",
      { type: GeometryType.POINT },
      makePoint(52.1, 4.3),
      false,
    ],
    [
      "POLYGON comparison (not yet implemented)",
      { type: GeometryType.POLYGON, polygon: [] },
      { type: GeometryType.POLYGON, polygon: [] },
      false,
    ],
    [
      "GEOMETRY_COLLECTION comparison (not yet implemented)",
      { type: GeometryType.GEOMETRY_COLLECTION, geometries: [] },
      { type: GeometryType.GEOMETRY_COLLECTION, geometries: [] },
      false,
    ],
  ])("%s", (_desc, left, right, expected) => {
    expect(LocationUtil.isSameGeometry(left, right)).toBe(expected);
  });
});

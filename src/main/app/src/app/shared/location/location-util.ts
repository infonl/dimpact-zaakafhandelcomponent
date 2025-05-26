/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Coordinate } from "ol/coordinate";
import { GeometryType } from "../../zaken/model/geometryType";
import { GeneratedType } from "../utils/generated-types";

export class LocationUtil {
  /**
   * Het converteren van een het centroide_ll attribuut vanuit de response van de locatieserver
   * @param wkt centroide_ll zijn latitude,longitude coordinaten in graden volgens de ETRS:89 projectie.
   * @private
   */
  public static wktToPoint(wkt: string) {
    const coordinate = wkt
      .replace("POINT(", "")
      .replace(")", "")
      .split(" ")
      .map(Number);
    return this.coordinateToPoint(coordinate);
  }

  public static coordinateToPoint([x, y]: Coordinate) {
    const geometry: GeneratedType<"RestGeometry"> = {
      type: GeometryType.POINT,
      point: {
        latitude: y,
        longitude: x,
      },
    };
    return geometry;
  }

  public static pointToCoordinate(
    point: GeneratedType<"RestGeometry">["point"],
  ): Coordinate {
    if (!point) return [0, 0];

    const { latitude, longitude } = point;
    // the map library uses [X, Y] instead of [latitude, longitude]
    return [longitude ?? 0, latitude ?? 0];
  }

  public static format(geometry?: GeneratedType<"RestGeometry">) {
    if (geometry?.type == GeometryType.POINT) {
      return geometry.point?.latitude + ", " + geometry.point?.longitude;
    }
    return null;
  }

  public static isSameGeometry(
    left?: GeneratedType<"RestGeometry"> | null,
    right?: GeneratedType<"RestGeometry"> | null,
  ) {
    if (left?.type !== right?.type) return false;

    switch (left?.type) {
      case GeometryType.POINT:
        return (
          left?.point?.latitude === right?.point?.latitude &&
          left?.point?.longitude === right?.point?.longitude
        );
      case GeometryType.POLYGON:
        console.log("Polygon comparison not implemented");
        return false;
      case GeometryType.GEOMETRY_COLLECTION:
        console.log("Geometry collection comparison not implemented");
        return false;
      default:
        return false;
    }
  }
}

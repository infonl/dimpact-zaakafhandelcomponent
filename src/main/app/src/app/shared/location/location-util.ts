/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Coordinate } from "ol/coordinate";
import { GeometryType } from "../../zaken/model/geometryType";
import { Api } from "../utils/generated-types";

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
    const geometry: Api<"RestGeometry"> = {
      type: GeometryType.POINT,
      point: {
        latitude: y,
        longitude: x,
      },
    };
    return geometry;
  }

  public static pointToCoordinate(
    point: Api<"RestGeometry">["point"],
  ): Coordinate {
    if (!point) return [0, 0];

    const { latitude, longitude } = point;
    // the map library uses [X, Y] instead of [latitude, longitude]
    return [longitude ?? 0, latitude ?? 0];
  }

  public static format(geometry?: Api<"RestGeometry">) {
    if (geometry?.type == GeometryType.POINT) {
      return geometry.point?.latitude + ", " + geometry.point?.longitude;
    }
    return null;
  }
}

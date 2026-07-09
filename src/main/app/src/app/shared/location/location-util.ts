/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Coordinate } from "ol/coordinate";
import { Geometry } from "../../zaken/model/geometry";
import { GeometryCoordinate } from "../../zaken/model/geometryCoordinate";
import { GeometryType } from "../../zaken/model/geometryType";

export class LocationUtil {
  /**
   * Het converteren van een het centroide_ll attribuut vanuit de response van de locatieserver
   * @param wkt centroide_ll zijn latitude,longitude coordinaten in graden volgens de ETRS:89 projectie.
   * @private
   */
  public static wktToPoint(wkt: string): Geometry {
    const coordinate = wkt
      .replace("POINT(", "")
      .replace(")", "")
      .split(" ")
      .map(Number);
    return this.coordinateToPoint(coordinate);
  }

  public static coordinateToPoint([x, y]: Coordinate): Geometry {
    const geometrie = new Geometry(GeometryType.POINT);
    geometrie.point = new GeometryCoordinate(y, x);
    return geometrie;
  }

  public static pointToCoordinate({
    latitude,
    longitude,
  }: GeometryCoordinate): Coordinate {
    // the map library uses [X,Y] in stead of [latitude, longitude]
    return [longitude, latitude];
  }

  public static format(geometry: Geometry) {
    if (geometry && geometry.type == GeometryType.POINT) {
      return geometry.point.latitude + ", " + geometry.point.longitude;
    }
    return null;
  }
}

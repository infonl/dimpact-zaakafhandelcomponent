/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeometryCoordinate } from "./geometryCoordinate";
import { GeometryType } from "./geometryType";

/**
 * @deprecated - use the `GeneratedType`
 */
export class Geometry {
  constructor(type: GeometryType) {
    this.type = type;
  }

  type: GeometryType;

  point: GeometryCoordinate;

  polygon: GeometryCoordinate[][];

  geometrycollection: Geometry[];
}

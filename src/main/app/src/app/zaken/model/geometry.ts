/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeometryCoordinate } from "./geometryCoordinate";
import { GeometryType } from "./geometryType";

/**
 * @deprecated - use the `GeneratedType`
 */
export type Geometry = {
  type: GeometryType;
  point: GeometryCoordinate;
  polygon: GeometryCoordinate[][];
  geometrycollection: Geometry[];
};

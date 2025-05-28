/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Geometry } from "../../zaken/model/geometry";
import { BAGObject } from "./bagobject";

/**
 * @deprecated - use the `GeneratedType`
 */
export class AdresseerbaarObject extends BAGObject {
  status: string;
  typeAdresseerbaarObject: "LIGPLAATS" | "STANDPLAATS" | "VERBLIJFSOBJECT";
  vboDoel: string;
  vboOppervlakte: number;
  geometry: Geometry;
}

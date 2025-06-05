/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Geometry } from "../../zaken/model/geometry";
import { AdresseerbaarObject } from "./adresseerbaar-object";
import { BAGObject } from "./bagobject";
import { Nummeraanduiding } from "./nummeraanduiding";
import { OpenbareRuimte } from "./openbare-ruimte";
import { Pand } from "./pand";
import { Woonplaats } from "./woonplaats";

/**
 * @deprecated - use the `GeneratedType`
 */
export class Adres extends BAGObject {
  postcode: string;
  huisnummerWeergave: string;
  huisnummer: string;
  huisletter: string;
  huisnummertoevoeging: string;
  openbareRuimteNaam: string;
  woonplaatsNaam: string;
  openbareRuimte: OpenbareRuimte;
  nummeraanduiding: Nummeraanduiding;
  woonplaats: Woonplaats;
  adresseerbaarObject: AdresseerbaarObject;
  panden: Pand[];
  geometry: Geometry;
}

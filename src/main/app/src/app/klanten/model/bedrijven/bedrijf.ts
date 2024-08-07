/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { IdentificatieType } from "../klanten/identificatieType";
import { Klant } from "../klanten/klant";

export class Bedrijf implements Klant {
  vestigingsnummer: string;
  kvkNummer: string;
  rsin: string;
  adres: string;
  postcode: string;
  type: string;
  identificatieType: IdentificatieType;
  identificatie: string;
  naam: string;
  emailadres: string;
  telefoonnummer: string;
}

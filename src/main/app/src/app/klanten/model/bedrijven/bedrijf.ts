/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../../shared/utils/generated-types";
import { Klant } from "../klanten/klant";

export class Bedrijf implements Klant {
  vestigingsnummer: string;
  kvkNummer: string;
  rsin: string;
  adres: string;
  postcode: string;
  type: string;
  identificatieType: GeneratedType<"IdentificatieType">;
  identificatie: string;
  naam: string;
  emailadres: string;
  telefoonnummer: string;
}

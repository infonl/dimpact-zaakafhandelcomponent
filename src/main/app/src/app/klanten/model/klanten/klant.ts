/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../../shared/utils/generated-types";

export interface Klant {
  identificatieType: GeneratedType<"IdentificatieType">;
  identificatie: string;
  naam: string;
  emailadres: string;
  telefoonnummer: string;
}

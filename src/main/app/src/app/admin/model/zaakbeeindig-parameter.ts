/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakbeeindigReden } from "./zaakbeeindig-reden";

export class ZaakbeeindigParameter {
  id: string;
  zaakbeeindigReden: ZaakbeeindigReden;
  resultaattype: GeneratedType<"RestResultaattype">;
}

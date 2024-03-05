/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Resultaattype } from "../../zaken/model/resultaattype";
import { ZaakbeeindigReden } from "./zaakbeeindig-reden";

export class ZaakbeeindigParameter {
  id: string;
  zaakbeeindigReden: ZaakbeeindigReden;
  resultaattype: Resultaattype;
}

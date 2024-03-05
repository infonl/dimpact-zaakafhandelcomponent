/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ZaakbeeindigParameter } from "./zaakbeeindig-parameter";
import { ZaakbeeindigReden } from "./zaakbeeindig-reden";
import { ZaaknietontvankelijkReden } from "./zaaknietontvankelijk-reden";

export class ZaaknietontvankelijkParameter extends ZaakbeeindigParameter {
  zaakbeeindigReden: ZaakbeeindigReden =
    ZaaknietontvankelijkReden.getInstance();
}

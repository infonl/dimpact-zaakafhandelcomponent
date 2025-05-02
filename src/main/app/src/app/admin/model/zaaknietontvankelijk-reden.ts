/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakbeeindigReden } from "./zaakbeeindig-reden";

export class ZaaknietontvankelijkReden extends ZaakbeeindigReden {
  private static ZNOR = new ZaaknietontvankelijkReden();
  id = "znor";
  naam = "Zaak is niet ontvankelijk";

  private constructor() {
    super();
  }

  static is(reden?: GeneratedType<"RESTZaakbeeindigReden">): boolean {
    return reden?.id === this.ZNOR.id;
  }
}

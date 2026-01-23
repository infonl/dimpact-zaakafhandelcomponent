/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Klant } from "./klant";
import { Roltype } from "./roltype";

export class KlantGegevens {
  constructor(public klant: Klant) {}

  betrokkeneRoltype: Roltype;
  betrokkeneToelichting: string;
}

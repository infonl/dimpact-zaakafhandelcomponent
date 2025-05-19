/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../../shared/utils/generated-types";
import { Klant } from "./klant";

export class KlantGegevens {
  constructor(public klant: Klant) {}

  betrokkeneRoltype: GeneratedType<"RestRoltype">;
  betrokkeneToelichting: string;
}

/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../../shared/utils/generated-types";

/**
 * @deprecated - use the `GeneratedType`
 */
export class KlantGegevens {
  constructor(public klant: GeneratedType<"RestPersoon" | "RestBedrijf">) {}

  betrokkeneRoltype: GeneratedType<"RestRoltype">;
  betrokkeneToelichting: string;
}

/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../shared/utils/generated-types";
import { BetrokkeneIdentificatie } from "../zaken/model/betrokkeneIdentificatie";

export function buildBedrijfRouteLink(
  bedrijf?: GeneratedType<"RestBedrijf"> | null,
) {
  if (!bedrijf) return;
  const tempBedrijf = new BetrokkeneIdentificatie(bedrijf);

  switch (tempBedrijf.type) {
    case "RSIN":
      return ["/bedrijf", tempBedrijf.kvkNummer];
    case "VN":
      return [
        "/bedrijf",
        tempBedrijf.kvkNummer,
        "vestiging",
        tempBedrijf.vestigingsnummer,
      ];
    default:
      throw new Error("Unknown bedrijf type");
  }
}

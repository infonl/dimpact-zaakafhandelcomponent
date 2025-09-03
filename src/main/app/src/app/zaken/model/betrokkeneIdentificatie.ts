/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../shared/utils/generated-types";

export class BetrokkeneIdentificatie
  implements GeneratedType<"BetrokkeneIdentificatie">
{
  public readonly bsnNummer?: string | null = null;
  public readonly kvkNummer?: string | null = null;
  public readonly vestigingsnummer?: string | null = null;
  /**
   * @deprecated - use `kvkNummer` or `rsin` instead
   *
   * This should only be used for backwards compatibility and fetching data from the API.
   */
  public readonly rsin?: string | null = null;
  public readonly type: GeneratedType<"IdentificatieType">;

  constructor(
    betrokkene: GeneratedType<
      "RestPersoon" | "RestBedrijf" | "BetrokkeneIdentificatie"
    >,
  ) {
    this.type = this.getType(betrokkene);
    console.log(betrokkene, this.type);
    switch (this.type) {
      case "BSN":
        if ("bsn" in betrokkene) {
          this.bsnNummer = betrokkene.bsn;
          break;
        }
        throw new Error(
          `${BetrokkeneIdentificatie.name}: Tried to add a ${this.type} betrokkene without a BSN number`,
        );
        break;
      case "VN":
        if ("kvkNummer" in betrokkene || "vestigingsnummer" in betrokkene) {
          this.kvkNummer = betrokkene.kvkNummer;
          this.vestigingsnummer = betrokkene.vestigingsnummer;
          break;
        }
        throw new Error(
          `${BetrokkeneIdentificatie.name}: Tried to add a ${this.type} betrokkene without a kvkNummer or vestigingsnummer`,
        );
      case "RSIN":
        if ("kvkNummer" in betrokkene || "rsin" in betrokkene) {
          this.kvkNummer = betrokkene.kvkNummer;
          this.rsin = betrokkene.rsin ?? betrokkene.kvkNummer; // For a `rechtspersoon` (RSIN) we have the kvkNummer
          break;
        }
        throw new Error(
          `${BetrokkeneIdentificatie.name}: Tried to add a ${this.type} betrokkene without a kvkNummer or rsin`,
        );
      default:
        throw new Error(
          `${BetrokkeneIdentificatie.name}: Unsupported identificatie type ${this.type}`,
        );
    }
  }

  private getType(
    betrokkene: GeneratedType<
      "RestPersoon" | "RestBedrijf" | "BetrokkeneIdentificatie"
    >,
  ) {
    if ("identificatieType" in betrokkene) {
      return betrokkene.identificatieType!;
    }

    if ("type" in betrokkene) {
      return betrokkene.type as GeneratedType<"IdentificatieType">;
    }

    throw new Error(
      `${BetrokkeneIdentificatie.name}: Unsupported betrokkene type`,
    );
  }
}

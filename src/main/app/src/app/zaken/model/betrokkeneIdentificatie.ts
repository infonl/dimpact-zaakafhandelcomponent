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
    switch (this.type) {
      case "BSN":
        if ("bsn" in betrokkene && betrokkene.bsn !== null) {
          this.bsnNummer = betrokkene.bsn;
          break;
        }
        if ("bsnNummer" in betrokkene && betrokkene.bsnNummer !== null) {
          this.bsnNummer = betrokkene.bsnNummer;
          break;
        }
        throw new Error(
          `${BetrokkeneIdentificatie.name}: Tried to add a ${this.type} betrokkene without a BSN number`,
        );
      case "VN":
        if ("vestigingsnummer" in betrokkene) {
          this.vestigingsnummer = betrokkene.vestigingsnummer;
          if ("kvkNummer" in betrokkene && betrokkene.kvkNummer !== null) {
            this.kvkNummer = betrokkene.kvkNummer;
          }
          break;
        }
        throw new Error(
          `${BetrokkeneIdentificatie.name}: Tried to add a ${this.type} betrokkene without a vestigingsnummer`,
        );
      case "RSIN":
        if (
          ("kvkNummer" in betrokkene && betrokkene.kvkNummer !== null) ||
          ("rsin" in betrokkene && betrokkene.rsin !== null)
        ) {
          this.kvkNummer = betrokkene.kvkNummer; // A `rechtspersoon` has the type RSIN
          this.rsin = betrokkene.rsin; // For backwards compatibility
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
  ): GeneratedType<"IdentificatieType"> {
    if ("identificatieType" in betrokkene) {
      return betrokkene.identificatieType!;
    }

    if ("type" in betrokkene) {
      switch (
        betrokkene.type as GeneratedType<"BedrijfType" | "IdentificatieType">
      ) {
        case "RECHTSPERSOON":
        case "RSIN":
          return "RSIN";
        case "VN":
        case "BSN":
          return betrokkene.type as GeneratedType<"IdentificatieType">;
        default:
          throw new Error(`Unsupported betrokkene type ${betrokkene.type}`);
      }
    }

    throw new Error(
      `${BetrokkeneIdentificatie.name}: Unsupported betrokkene type`,
    );
  }
}

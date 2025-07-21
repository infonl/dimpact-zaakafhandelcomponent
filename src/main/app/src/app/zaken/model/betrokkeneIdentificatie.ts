import { GeneratedType } from "../../shared/utils/generated-types";

export class BetrokkeneIdentificatie
  implements GeneratedType<"BetrokkeneIdentificatie">
{
  public readonly bsnNummer?: string | null = null;
  public readonly kvkNummer?: string | null = null;
  public readonly vestigingsnummer?: string | null = null;
  public readonly rsinNummer?: string | null = null;
  public readonly type: GeneratedType<"IdentificatieType">;

  constructor(betrokkene: GeneratedType<"RestPersoon" | "RestBedrijf">) {
    this.type = betrokkene.identificatieType!;
    switch (betrokkene.identificatieType) {
      case "BSN":
        if ("bsn" in betrokkene) {
          this.bsnNummer = betrokkene.bsn;
        } else {
          throw new Error(
            `${BetrokkeneIdentificatie.name}: Tried to add a betrokkene without a BSN number`,
          );
        }
        break;
      case "VN":
        if ("kvkNummer" in betrokkene || "vestigingsnummer" in betrokkene) {
          this.kvkNummer = betrokkene.kvkNummer;
          this.vestigingsnummer = betrokkene.vestigingsnummer;
        } else {
          throw new Error(
            `${BetrokkeneIdentificatie.name}: Tried to add a betrokkene without a KVK or vestigings number`,
          );
        }
        break;
      case "RSIN":
        if ("rsin" in betrokkene) {
          this.rsinNummer = betrokkene.rsin;
        } else {
          throw new Error(
            `${BetrokkeneIdentificatie.name}: Tried to add a betrokkene without a RSIN number`,
          );
        }
        break;
      default:
        throw new Error(
          `${BetrokkeneIdentificatie.name}: Unsupported identificatie type ${betrokkene.identificatieType}`,
        );
    }
  }
}

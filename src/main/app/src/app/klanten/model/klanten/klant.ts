import { GeneratedType } from "../../../shared/utils/generated-types";

export interface Klant {
  identificatieType: GeneratedType<"IdentificatieType">;
  identificatie: string;
  naam: string;
  emailadres: string;
  telefoonnummer: string;
}

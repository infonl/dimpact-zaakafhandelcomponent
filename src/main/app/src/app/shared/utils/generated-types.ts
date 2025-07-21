/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { components } from "../../../generated/types/zac-openapi-types";

type NestedSchemaProperty<Type, Key> = Key extends `${infer P}.${infer R}`
  ? P extends keyof Type
    ? NestedSchemaProperty<Type[P], R>
    : never
  : Key extends keyof Type
    ? Type[Key]
    : never;

export type GeneratedType<Key extends keyof components["schemas"]> =
  NestedSchemaProperty<components["schemas"], Key>;

export class BetrokkeneIdentificatie {
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
                        `${BetrokkeneIdentificatie.name}: Tried to add a betrokkene without a KVK number`,
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

    public toJson(): Record<string, never> {
        return {
            bsnNummer: this.bsnNummer as never,
            kvkNummer: this.kvkNummer as never,
            vestigingsnummer: this.vestigingsnummer as never,
            rsinNummer: this.rsinNummer as never,
            type: this.type as never,
        };
    }
}

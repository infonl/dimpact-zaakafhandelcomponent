/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { KlantenService } from "../klanten.service";
import { GeneratedType } from "src/app/shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class BedrijfResolverService {
  private initiatorIdentificatie: GeneratedType<"BetrokkeneIdentificatie"> | null =
    null;

  constructor(private klantenService: KlantenService) {}

  resolve(route: ActivatedRouteSnapshot) {
    const lookupType = route.data["lookupType"];

    const baseIdentificatie = {
      kvkNummer: null,
      vestigingsnummer: null,
      rsinNummer: null,
      bsnNummer: null,
    };

    switch (lookupType) {
      case "kvkvestigingsnummer":
        this.initiatorIdentificatie = {
          ...baseIdentificatie,
          type: "VN",
          kvkNummer: route.paramMap.get("kvk"),
          vestigingsnummer: route.paramMap.get("vestigingsnummer"),
        };
        break;

      case "vestigingsnummer":
        this.initiatorIdentificatie = {
          ...baseIdentificatie,
          type: "VN",
          vestigingsnummer: route.paramMap.get("vestigingsnummer"),
        };
        break;

      case "kvk":
        this.initiatorIdentificatie = {
          ...baseIdentificatie,
          type: "RSIN",
          kvkNummer: route.paramMap.get("kvk"),
        };
        break;

      case "rsin":
        this.initiatorIdentificatie = {
          ...baseIdentificatie,
          type: "RSIN",
          rsinNummer: route.paramMap.get("rsin"),
        };
        break;

      default:
        break;
    }

    if (!this.initiatorIdentificatie) {
      throw new Error(
        `${BedrijfResolverService.name}: Unknown lookup type or missing route params`,
      );
    }

    return this.klantenService.readBedrijf(this.initiatorIdentificatie);
  }
}

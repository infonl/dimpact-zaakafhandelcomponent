/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { KlantenService } from "../klanten.service";
import {
  KVK_LENGTH,
  VESTIGINGSNUMMER_LENGTH,
} from "src/app/shared/utils/constants";
import { GeneratedType } from "src/app/shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class BedrijfResolverService {
  private initiatorIdentificatie: GeneratedType<"BetrokkeneIdentificatie"> | null =
    null;

  constructor(private klantenService: KlantenService) {}

  resolve(route: ActivatedRouteSnapshot) {
    const id = route.paramMap.get("vesOrRSIN");

    if (!id) {
      throw new Error(
        `${BedrijfResolverService.name}: no 'vesOrRSIN' found in route`,
      );
    }

    const baseIdentificatie = {
      kvkNummer: null,
      vestigingsnummer: null,
      rsin: null,
      bsnNummer: null,
    };

    switch (id.length) {
      case VESTIGINGSNUMMER_LENGTH:
        this.initiatorIdentificatie = {
          ...baseIdentificatie,
          type: "VN",
          vestigingsnummer: id,
        };
        break;

      case KVK_LENGTH:
        this.initiatorIdentificatie = {
          ...baseIdentificatie,
          type: "RSIN",
          kvkNummer: route.paramMap.get("kvk"),
        };
        break;

      default:
        this.initiatorIdentificatie = {
          ...baseIdentificatie,
          type: "RSIN",
          rsin: route.paramMap.get("rsin"),
        };
        break;
    }

    return this.klantenService.readBedrijf(this.initiatorIdentificatie);
  }
}

/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import {
  KVK_LENGTH,
  VESTIGINGSNUMMER_LENGTH,
} from "src/app/shared/utils/constants";
import { BetrokkeneIdentificatie } from "../../zaken/model/betrokkeneIdentificatie";
import { KlantenService } from "../klanten.service";

@Injectable({
  providedIn: "root",
})
export class BedrijfResolverService {
  constructor(private klantenService: KlantenService) {}

  resolve(route: ActivatedRouteSnapshot) {
    const id = route.paramMap.get("id");

    if (!id) {
      throw new Error(`${BedrijfResolverService.name}: no 'id' found in route`);
    }

    const identificatieType = this.getType(id);
    return this.klantenService.readBedrijf(
      new BetrokkeneIdentificatie({
        identificatieType,
        vestigingsnummer: identificatieType === "VN" ? id : null,
        kvkNummer:
          identificatieType === "RSIN" &&
          identificatieType.length === KVK_LENGTH
            ? id
            : null,
        rsin:
          identificatieType === "RSIN" &&
          identificatieType.length !== KVK_LENGTH
            ? id
            : null,
      }),
    );
  }

  private getType(id: string) {
    switch (id.length) {
      case VESTIGINGSNUMMER_LENGTH:
        return "VN";
      case KVK_LENGTH:
      default:
        return "RSIN";
    }
  }
}

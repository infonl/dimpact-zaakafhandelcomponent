/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { ActivatedRouteSnapshot } from "@angular/router";
import { QueryClient } from "@tanstack/angular-query-experimental";
import {
  KVK_LENGTH,
  VESTIGINGSNUMMER_LENGTH,
} from "src/app/shared/utils/constants";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { BetrokkeneIdentificatie } from "../../zaken/model/betrokkeneIdentificatie";
import { KlantenService } from "../klanten.service";

@Injectable({
  providedIn: "root",
})
export class BedrijfResolverService {
  private readonly klantenService = inject(KlantenService);
  private readonly foutafhandelingService = inject(FoutAfhandelingService);
  private readonly queryClient = inject(QueryClient);

  async resolve(route: ActivatedRouteSnapshot) {
    const id = route.paramMap.get("id");
    const vestigingsnummer = route.paramMap.get("vestigingsnummer");

    if (!id) {
      return Promise.reject(
        new Error(`${BedrijfResolverService.name}: no 'id' found in route`),
      );
    }

    const identificatieType = vestigingsnummer ? "VN" : this.getType(id);

    try {
      const betrokkeneIdentificatie = new BetrokkeneIdentificatie({
        identificatieType,
        vestigingsnummer: vestigingsnummer
          ? vestigingsnummer
          : identificatieType === "VN"
            ? id
            : null,
        kvkNummer: id.length === KVK_LENGTH ? id : null,
        rsin:
          identificatieType === "RSIN" && id.length !== KVK_LENGTH ? id : null,
      });

      return this.queryClient.ensureQueryData(
        this.klantenService.readBedrijf(betrokkeneIdentificatie),
      );
    } catch {
      this.handleBetrokkeneError();
    }
  }

  private handleBetrokkeneError() {
    this.foutafhandelingService.openFoutDialog(
      "msg.error.search.bedrijf.not-found",
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

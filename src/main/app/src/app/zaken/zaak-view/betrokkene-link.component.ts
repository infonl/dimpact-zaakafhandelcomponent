/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, computed, input } from "@angular/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { buildBedrijfRouteLink } from "../../klanten/klanten-routing.module";
import { KlantenService } from "../../klanten/klanten.service";
import { BetrokkeneIdentificatie } from "../model/betrokkeneIdentificatie";

@Component({
  selector: "betrokkene-link",
  templateUrl: "./betrokkene-link.component.html",
  styleUrls: [],
})
export class BetrokkeneLinkComponent {
  constructor(private readonly klantenService: KlantenService) {}

  protected readonly persoonQuery = injectQuery(() => {
    const betrokkene = this.betrokkene();

    if (!this.isBsnType()) {
      return {
        queryKey: ["persoon", betrokkene.identificatie],
        enabled: false,
      };
    }

    return  this.klantenService.readPersoon(
      betrokkene.identificatie,
    );
  });

  protected readonly bedrijfQuery = injectQuery(() => {
    const betrokkene = this.betrokkene();

    if (this.isBsnType()) {
      return {
        queryKey: ["bedrijf", betrokkene.identificatie],
        enabled: false,
      };
    }

    return  this.klantenService.readBedrijf(
      new BetrokkeneIdentificatie(betrokkene),
    );
  });

  protected readonly betrokkene =
    input.required<GeneratedType<"RestZaakBetrokkene">>();

  protected readonly bedrijfRouteLink = computed(() =>
    buildBedrijfRouteLink(this.betrokkene()),
  );

  private isBsnType() {
    const betrokkene = this.betrokkene();
    return betrokkene.type === "BSN" || betrokkene.identificatieType === "BSN";
  }
}

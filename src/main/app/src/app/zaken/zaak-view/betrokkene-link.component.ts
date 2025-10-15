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
  protected readonly persoonQuery = injectQuery(() => {
    const betrokkene = this.betrokkene();

    if (betrokkene.type !== "BSN" && betrokkene.identificatieType !== "BSN") {
      return {
        queryKey: ["persoon", betrokkene.identificatie],
        enabled: false,
      };
    }

    const persoonQuery = this.klantService.readPersoon(
      betrokkene.identificatie,
    );

    return {
      queryKey: persoonQuery.queryKey,
      queryFn: persoonQuery.queryFn,
    };
  });

  protected bedrijfQuery = injectQuery(() => {
    const betrokkene = this.betrokkene();

    if (betrokkene.type === "BSN" || betrokkene.identificatieType === "BSN") {
      return {
        queryKey: ["bedrijf", betrokkene.identificatie],
        enabled: false,
      };
    }

    const bedrijfQuery = this.klantService.readBedrijf(
      new BetrokkeneIdentificatie(betrokkene),
    );

    return {
      queryKey: bedrijfQuery.queryKey,
      queryFn: bedrijfQuery.queryFn,
    };
  });

  protected readonly betrokkene =
    input.required<GeneratedType<"RestZaakBetrokkene">>();

  protected readonly bedrijfRouteLink = computed(() =>
    buildBedrijfRouteLink(this.betrokkene()),
  );

  constructor(private readonly klantService: KlantenService) {}
}

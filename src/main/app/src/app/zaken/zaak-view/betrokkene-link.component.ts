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
  protected readonly persoonQuery = injectQuery(() => ({
    ...this.klantService.readPersoon(this.betrokkene().identificatie),
    enabled: this.betrokkene().type === "BSN",
  }));

  protected bedrijfQuery = injectQuery(() => ({
    ...this.klantService.readBedrijf(
      new BetrokkeneIdentificatie(this.betrokkene()),
    ),
    enabled: this.betrokkene().type !== "BSN",
  }));

  protected readonly betrokkene =
    input.required<GeneratedType<"RestZaakBetrokkene">>();

  protected readonly bedrijfRouteLink = computed(() =>
    buildBedrijfRouteLink(this.betrokkene()),
  );

  constructor(private readonly klantService: KlantenService) {}
}

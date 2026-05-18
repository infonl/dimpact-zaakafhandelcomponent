/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, computed, inject, input } from "@angular/core";
import { MatIconAnchor } from "@angular/material/button";
import { MatIcon } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatTooltipModule } from "@angular/material/tooltip";
import { RouterLink } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { buildBedrijfRouteLink } from "../../klanten/klanten-routing.module";
import { KlantenService } from "../../klanten/klanten.service";
import { BetrokkeneIdentificatie } from "../model/betrokkeneIdentificatie";

@Component({
  selector: "betrokkene-link",
  templateUrl: "./betrokkene-link.component.html",
  styleUrls: [],
  standalone: true,
  imports: [
    MatIconAnchor,
    MatIcon,
    MatProgressSpinnerModule,
    MatTooltipModule,
    RouterLink,
    TranslateModule,
  ],
})
export class BetrokkeneLinkComponent {
  private readonly klantenService = inject(KlantenService);

  protected readonly persoonQuery = injectQuery(() => {
    const betrokkene = this.betrokkene();

    if (!this.isBsnType() || !betrokkene.temporaryPersonId) {
      return {
        queryKey: [
          "persoon",
          betrokkene.temporaryPersonId,
          this.zaaktypeUuid(),
        ],
        enabled: false,
      };
    }

    return this.klantenService.readPersoon(
      betrokkene.temporaryPersonId,
      this.zaaktypeUuid(),
    );
  });

  protected readonly bedrijfQuery = injectQuery(() => {
    const betrokkene = this.betrokkene();

    if (this.isBsnType() || !betrokkene.identificatieType) {
      return {
        queryKey: ["bedrijf", betrokkene.vestigingsnummer ?? betrokkene.kvkNummer],
        enabled: false,
      };
    }

    return this.klantenService.readBedrijf(
      new BetrokkeneIdentificatie(betrokkene),
    );
  });

  protected readonly betrokkene =
    input.required<GeneratedType<"RestZaakBetrokkene">>();

  protected readonly zaaktypeUuid = input.required<string>();

  protected readonly bedrijfRouteLink = computed(() => {
    const betrokkene = this.betrokkene();
    return buildBedrijfRouteLink(betrokkene);
  });

  private isBsnType() {
    const betrokkene = this.betrokkene();
    return betrokkene.type === "BSN" || betrokkene.identificatieType === "BSN";
  }
}

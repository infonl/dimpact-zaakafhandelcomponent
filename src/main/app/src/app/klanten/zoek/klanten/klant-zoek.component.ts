/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, inject, Output } from "@angular/core";
import { MatIconModule } from "@angular/material/icon";
import { MatTabsModule } from "@angular/material/tabs";
import { MatTooltip } from "@angular/material/tooltip";
import { TranslateModule } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { PolicyService } from "../../../policy/policy.service";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { BedrijfZoekComponent } from "../bedrijven/bedrijf-zoek.component";
import { PersoonZoekComponent } from "../personen/persoon-zoek.component";

@Component({
  selector: "zac-klant-zoek",
  templateUrl: "./klant-zoek.component.html",
  styleUrls: ["./klant-zoek.component.less"],
  standalone: true,
  imports: [
    MatTabsModule,
    MatIconModule,
    MatTooltip,
    TranslateModule,
    PersoonZoekComponent,
    BedrijfZoekComponent,
  ],
})
export class KlantZoekComponent {
  @Output() klant = new EventEmitter<
    GeneratedType<"RestBedrijf" | "RestPersoon">
  >();

  private readonly policyService = inject(PolicyService);
  protected readonly overigeRechtenQuery = injectQuery(() =>
    this.policyService.readOverigeRechten(),
  );

  protected klantGeselecteerd(
    klant: GeneratedType<"RestBedrijf" | "RestPersoon">,
  ) {
    this.klant.emit(klant);
  }
}
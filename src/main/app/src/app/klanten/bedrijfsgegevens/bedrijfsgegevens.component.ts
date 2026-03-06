/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, computed, input, output, signal } from "@angular/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { TextIcon } from "../../shared/edit/text-icon";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BetrokkeneIdentificatie } from "../../zaken/model/betrokkeneIdentificatie";
import { buildBedrijfRouteLink } from "../klanten-routing.module";
import { KlantenService } from "../klanten.service";

@Component({
  selector: "zac-bedrijfsgegevens",
  templateUrl: "./bedrijfsgegevens.component.html",
  styleUrls: ["./bedrijfsgegevens.component.less"],
  standalone: false,
})
export class BedrijfsgegevensComponent {
  protected isVerwijderbaar = input<boolean | null>(false);
  protected isWijzigbaar = input<boolean | null>(false);
  protected initiatorIdentificatie =
    input.required<GeneratedType<"BetrokkeneIdentificatie">>();

  protected delete = output<GeneratedType<"RestBedrijf"> | null>();
  protected edit = output<GeneratedType<"RestBedrijf"> | null>();

  protected readonly bedrijfQuery = injectQuery(() =>
    this.klantenService.readBedrijf(
      new BetrokkeneIdentificatie(this.initiatorIdentificatie()),
    ),
  );

  protected vestigingsprofielOphalenMogelijk = computed(
    () => !!this.bedrijfQuery.data()?.vestigingsnummer,
  );

  protected vestigingsprofiel =
    signal<GeneratedType<"RestVestigingsprofiel"> | null>(null);

  protected warningIcon = new TextIcon(
    () => true,
    "warning",
    "warning-icon",
    "",
    "error",
  );

  constructor(private klantenService: KlantenService) {}

  protected bedrijfRouteLink() {
    return buildBedrijfRouteLink(this.bedrijfQuery.data());
  }

  ophalenVestigingsprofiel() {
    const vestigingsnummer = this.bedrijfQuery.data()?.vestigingsnummer;
    if (!vestigingsnummer) return;

    this.klantenService
      .readVestigingsprofiel(vestigingsnummer)
      .subscribe((value) => {
        this.vestigingsprofiel.set(value);
      });
  }
}

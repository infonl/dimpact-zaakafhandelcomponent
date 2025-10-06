/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, computed, input, output, signal } from "@angular/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { lastValueFrom } from "rxjs";
import { TextIcon } from "../../shared/edit/text-icon";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BetrokkeneIdentificatie } from "../../zaken/model/betrokkeneIdentificatie";
import { buildBedrijfRouteLink } from "../klanten-routing.module";
import { KlantenService } from "../klanten.service";

@Component({
  selector: "zac-bedrijfsgegevens",
  templateUrl: "./bedrijfsgegevens.component.html",
  styleUrls: ["./bedrijfsgegevens.component.less"],
})
export class BedrijfsgegevensComponent {
  protected isVerwijderbaar = input<boolean | null>(false);
  protected isWijzigbaar = input<boolean | null>(false);
  protected initiatorIdentificatie =
    input<GeneratedType<"BetrokkeneIdentificatie"> | null>(null);

  protected delete = output<GeneratedType<"RestBedrijf"> | null>();
  protected edit = output<GeneratedType<"RestBedrijf"> | null>();

  protected bedrijfQuery = injectQuery(() => ({
    enabled: !!this.initiatorIdentificatie(),
    retry: false,
    queryKey: [
      this.initiatorIdentificatie()?.type,
      this.initiatorIdentificatie()
        ? new BetrokkeneIdentificatie(this.initiatorIdentificatie()!).uniqueKey
        : null,
    ],
    queryFn: () => {
      const betrokkkene = this.initiatorIdentificatie();
      if (!betrokkkene) return Promise.resolve(null);
      return lastValueFrom(
        this.klantenService.readBedrijf(
          new BetrokkeneIdentificatie(betrokkkene),
        ),
      );
    },
  }));

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

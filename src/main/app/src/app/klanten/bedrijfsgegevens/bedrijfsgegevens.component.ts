/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgFor, NgIf } from "@angular/common";
import {
  Component,
  computed,
  inject,
  input,
  output,
  signal,
} from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatTooltipModule } from "@angular/material/tooltip";
import { RouterLink } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { TextIcon } from "../../shared/edit/text-icon";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BetrokkeneIdentificatie } from "../../zaken/model/betrokkeneIdentificatie";
import { buildBedrijfRouteLink } from "../klanten-routing.module";
import { KlantenService } from "../klanten.service";

@Component({
  selector: "zac-bedrijfsgegevens",
  templateUrl: "./bedrijfsgegevens.component.html",
  styleUrls: ["./bedrijfsgegevens.component.less"],
  standalone: true,
  imports: [
    NgIf,
    NgFor,
    MatExpansionModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatTooltipModule,
    MatButtonModule,
    RouterLink,
    TranslateModule,
    StaticTextComponent,
  ],
})
export class BedrijfsgegevensComponent {
  private readonly klantenService = inject(KlantenService);

  protected zaak = input.required<GeneratedType<"RestZaak">>();

  protected delete = output<GeneratedType<"RestBedrijf"> | null>();
  protected edit = output<GeneratedType<"RestBedrijf"> | null>();

  protected readonly initiatorIdentificatie = computed(
    () => this.zaak().initiatorIdentificatie!,
  );

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

  protected bedrijfRouteLink() {
    return buildBedrijfRouteLink(this.bedrijfQuery.data());
  }

  protected ophalenVestigingsprofiel() {
    const vestigingsnummer = this.bedrijfQuery.data()?.vestigingsnummer;
    if (!vestigingsnummer) return;

    this.klantenService
      .readVestigingsprofiel(vestigingsnummer)
      .subscribe((value) => {
        this.vestigingsprofiel.set(value);
      });
  }
}

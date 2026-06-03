/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgFor, NgIf } from "@angular/common";
import {
  Component,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  untracked,
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

  protected profielOphalenMogelijk = computed(() => {
    const data = this.bedrijfQuery.data();
    return !!data?.vestigingsnummer || !!data?.kvkNummer;
  });

  protected profiel = signal<GeneratedType<"RestBedrijfsprofiel"> | null>(null);

  private _prevIdentificatie: string | null = null;

  constructor() {
    effect(() => {
      const currentId = JSON.stringify(this.initiatorIdentificatie());
      untracked(() => {
        if (
          this._prevIdentificatie !== null &&
          this._prevIdentificatie !== currentId
        ) {
          this.profiel.set(null);
        }
        this._prevIdentificatie = currentId;
      });
    });
  }

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

  protected ophalenProfiel() {
    const data = this.bedrijfQuery.data();
    if (data?.vestigingsnummer) {
      this.klantenService
        .readVestigingsprofiel(data.vestigingsnummer)
        .subscribe((value) => {
          this.profiel.set(value);
        });
    } else if (data?.kvkNummer) {
      this.klantenService
        .readBasisprofiel(data.kvkNummer)
        .subscribe((value) => {
          this.profiel.set(value);
        });
    }
  }
}

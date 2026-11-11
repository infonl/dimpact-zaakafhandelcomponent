/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  computed,
  inject,
  input,
  linkedSignal,
  output,
} from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatTooltipModule } from "@angular/material/tooltip";
import { RouterLink } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { TextIcon } from "../../shared/edit/text-icon";
import { I18nKeyPipe } from "../../shared/pipes/i18n-key.pipe";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BetrokkeneIdentificatie } from "../../zaken/model/betrokkeneIdentificatie";
import { buildBedrijfRouteLink } from "../bedrijf-route-link";
import { injectKvkProfiel, KvkProfielAanvraag } from "../inject-kvk-profiel";
import { KlantenService } from "../klanten.service";

@Component({
  selector: "zac-bedrijfsgegevens",
  templateUrl: "./bedrijfsgegevens.component.html",
  styleUrls: ["./bedrijfsgegevens.component.less"],
  standalone: true,
  imports: [
    I18nKeyPipe,
    MatExpansionModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatTooltipModule,
    MatButtonModule,
    RouterLink,
    TranslatePipe,
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

  private readonly profielOpgevraagd = linkedSignal({
    source: () => JSON.stringify(this.initiatorIdentificatie()),
    computation: () => false,
  });

  private readonly kvkProfiel = injectKvkProfiel((): KvkProfielAanvraag => {
    const bedrijf = this.bedrijfQuery.data();
    if (bedrijf?.vestigingsnummer)
      return { vestigingsnummer: bedrijf.vestigingsnummer };
    if (bedrijf?.kvkNummer) return { kvkNummer: bedrijf.kvkNummer };
    return null;
  });

  protected readonly profielFoutmelding = this.kvkProfiel.foutmelding;

  protected readonly adressen = this.kvkProfiel.adressen;

  protected readonly profiel = computed(() =>
    this.profielOpgevraagd() ? this.kvkProfiel.profiel() : null,
  );

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
    this.profielOpgevraagd.set(true);
  }
}

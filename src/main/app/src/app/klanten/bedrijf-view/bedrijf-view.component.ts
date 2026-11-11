/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, computed, inject, signal } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatIconModule } from "@angular/material/icon";
import { MatSidenavModule } from "@angular/material/sidenav";
import { ActivatedRoute } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import { KlantContactmomentenTabelComponent } from "../../contactmomenten/klant-contactmomenten-tabel/klant-contactmomenten-tabel.component";
import { TextIcon } from "../../shared/edit/text-icon";
import { I18nKeyPipe } from "../../shared/pipes/i18n-key.pipe";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { injectKvkProfiel, KvkProfielAanvraag } from "../inject-kvk-profiel";
import { KlantZakenTabelComponent } from "../klant-zaken-tabel/klant-zaken-tabel.component";

@Component({
  templateUrl: "./bedrijf-view.component.html",
  styleUrls: ["./bedrijf-view.component.less"],
  standalone: true,
  imports: [
    I18nKeyPipe,
    MatSidenavModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    TranslatePipe,
    StaticTextComponent,
    KlantZakenTabelComponent,
    KlantContactmomentenTabelComponent,
  ],
})
export class BedrijfViewComponent {
  private readonly route = inject(ActivatedRoute);

  protected readonly bedrijf = signal<GeneratedType<"RestBedrijf"> | null>(
    null,
  );
  protected readonly profielOphalenMogelijk = signal(true);
  warningIcon = new TextIcon(
    () => true,
    "warning",
    "warning-icon",
    "",
    "error",
  );
  private readonly profielOpgevraagd = signal(false);

  private readonly profielAanvraag = computed((): KvkProfielAanvraag => {
    const bedrijf = this.bedrijf();
    if (bedrijf?.vestigingsnummer)
      return { vestigingsnummer: bedrijf.vestigingsnummer };
    if (bedrijf?.type === "RECHTSPERSOON" && bedrijf.kvkNummer)
      return { kvkNummer: bedrijf.kvkNummer };
    return null;
  });

  private readonly kvkProfiel = injectKvkProfiel(() => this.profielAanvraag());

  protected readonly profielFoutmelding = this.kvkProfiel.foutmelding;

  protected readonly adressen = this.kvkProfiel.adressen;

  protected readonly profiel = computed(() =>
    this.profielOpgevraagd() ? this.kvkProfiel.profiel() : null,
  );

  constructor() {
    this.route.data.subscribe((data) => {
      const bedrijf: GeneratedType<"RestBedrijf"> | null = data.bedrijf;
      this.bedrijf.set(bedrijf);
      this.profielOpgevraagd.set(false);
      this.profielOphalenMogelijk.set(!!this.profielAanvraag());
    });
  }

  protected ophalenProfiel() {
    this.profielOphalenMogelijk.set(false);
    this.profielOpgevraagd.set(true);
  }
}

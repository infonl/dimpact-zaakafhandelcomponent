/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  computed,
  inject,
  signal,
  ChangeDetectionStrategy,
} from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatIconModule } from "@angular/material/icon";
import { MatSidenavModule } from "@angular/material/sidenav";
import { ActivatedRoute } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { KlantContactmomentenTabelComponent } from "../../contactmomenten/klant-contactmomenten-tabel/klant-contactmomenten-tabel.component";
import { TextIcon } from "../../shared/edit/text-icon";
import { I18nKeyPipe } from "../../shared/pipes/i18n-key.pipe";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { KlantZakenTabelComponent } from "../klant-zaken-tabel/klant-zaken-tabel.component";
import { KlantenService } from "../klanten.service";

@Component({
  templateUrl: "./bedrijf-view.component.html",
  styleUrls: ["./bedrijf-view.component.less"],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
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
  private readonly klantenService = inject(KlantenService);

  protected bedrijf: GeneratedType<"RestBedrijf"> | null = null;
  protected profielOphalenMogelijk = true;
  warningIcon = new TextIcon(
    () => true,
    "warning",
    "warning-icon",
    "",
    "error",
  );
  private readonly profielAanvraag = signal<Pick<
    GeneratedType<"RestBedrijf">,
    "vestigingsnummer" | "kvkNummer"
  > | null>(null);

  private readonly vestigingsprofielQuery = injectQuery(() => {
    const vestigingsnummer = this.profielAanvraag()?.vestigingsnummer;
    return {
      ...this.klantenService.readVestigingsprofiel(vestigingsnummer ?? ""),
      enabled: !!vestigingsnummer,
    };
  });

  private readonly basisprofielQuery = injectQuery(() => {
    const profielAanvraag = this.profielAanvraag();
    return {
      ...this.klantenService.readBasisprofiel(profielAanvraag?.kvkNummer ?? ""),
      enabled:
        !profielAanvraag?.vestigingsnummer && !!profielAanvraag?.kvkNummer,
    };
  });

  protected readonly profiel = computed(() => {
    const profielAanvraag = this.profielAanvraag();
    if (!profielAanvraag) return null;
    if (profielAanvraag.vestigingsnummer)
      return this.vestigingsprofielQuery.data() ?? null;
    return this.basisprofielQuery.data() ?? null;
  });

  constructor() {
    this.route.data.subscribe((data) => {
      this.bedrijf = data.bedrijf;
      this.profielOphalenMogelijk =
        !!this.bedrijf?.vestigingsnummer ||
        (this.bedrijf?.type === "RECHTSPERSOON" && !!this.bedrijf?.kvkNummer);
    });
  }

  protected ophalenProfiel() {
    this.profielOphalenMogelijk = false;
    this.profielAanvraag.set({
      vestigingsnummer: this.bedrijf?.vestigingsnummer,
      kvkNummer: this.bedrijf?.kvkNummer,
    });
  }
}

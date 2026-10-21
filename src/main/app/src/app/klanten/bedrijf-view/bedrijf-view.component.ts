/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatIconModule } from "@angular/material/icon";
import { MatSidenavModule } from "@angular/material/sidenav";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { KlantContactmomentenTabelComponent } from "../../contactmomenten/klant-contactmomenten-tabel/klant-contactmomenten-tabel.component";
import { TextIcon } from "../../shared/edit/text-icon";
import { runQuery } from "../../shared/http/run-query";
import { I18nKeyPipe } from "../../shared/pipes/i18n-key.pipe";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { KlantZakenTabelComponent } from "../klant-zaken-tabel/klant-zaken-tabel.component";
import { KlantenService } from "../klanten.service";

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
    TranslateModule,
    StaticTextComponent,
    KlantZakenTabelComponent,
    KlantContactmomentenTabelComponent,
  ],
})
export class BedrijfViewComponent {
  protected bedrijf: GeneratedType<"RestBedrijf"> | null = null;
  protected profiel: GeneratedType<"RestBedrijfsprofiel"> | null = null;
  protected profielOphalenMogelijk = true;
  warningIcon = new TextIcon(
    () => true,
    "warning",
    "warning-icon",
    "",
    "error",
  );
  private readonly queryClient = inject(QueryClient);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly klantenService: KlantenService,
  ) {
    this.route.data.subscribe((data) => {
      this.bedrijf = data.bedrijf;
      this.profielOphalenMogelijk =
        !!this.bedrijf?.vestigingsnummer ||
        (this.bedrijf?.type === "RECHTSPERSOON" && !!this.bedrijf?.kvkNummer);
    });
  }

  protected ophalenProfiel() {
    this.profielOphalenMogelijk = false;
    if (this.bedrijf?.vestigingsnummer) {
      runQuery(
        this.queryClient,
        this.klantenService.readVestigingsprofiel(
          this.bedrijf.vestigingsnummer,
        ),
      ).subscribe((value) => {
        this.profiel = value;
      });
    } else if (this.bedrijf?.kvkNummer) {
      runQuery(
        this.queryClient,
        this.klantenService.readBasisprofiel(this.bedrijf.kvkNummer),
      ).subscribe((value) => {
        this.profiel = value;
      });
    }
  }
}

/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import { Component } from "@angular/core";
import { MatCardModule } from "@angular/material/card";
import { MatSidenavModule } from "@angular/material/sidenav";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { KlantContactmomentenTabelComponent } from "../../contactmomenten/klant-contactmomenten-tabel/klant-contactmomenten-tabel.component";
import { UtilService } from "../../core/service/util.service";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { KlantZakenTabelComponent } from "../klant-zaken-tabel/klant-zaken-tabel.component";

@Component({
  templateUrl: "./persoon-view.component.html",
  styleUrls: ["./persoon-view.component.less"],
  standalone: true,
  imports: [
    NgIf,
    MatSidenavModule,
    MatCardModule,
    TranslateModule,
    DatumPipe,
    StaticTextComponent,
    KlantZakenTabelComponent,
    KlantContactmomentenTabelComponent,
  ],
})
export class PersoonViewComponent {
  protected persoon: GeneratedType<"RestPersoon"> | null = null;

  constructor(
    private readonly utilService: UtilService,
    private readonly route: ActivatedRoute,
  ) {
    this.utilService.setTitle("persoonsgegevens");
    this.route.data.subscribe((data) => {
      this.persoon = data.persoon;
    });
  }
}

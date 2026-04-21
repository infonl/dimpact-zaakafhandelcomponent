/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Output } from "@angular/core";
import { MatIconModule } from "@angular/material/icon";
import { MatTabsModule } from "@angular/material/tabs";
import { TranslateModule } from "@ngx-translate/core";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { BedrijfZoekComponent } from "../../zoek/bedrijven/bedrijf-zoek.component";
import { PersoonZoekComponent } from "../../zoek/personen/persoon-zoek.component";

@Component({
  selector: "zac-klant-zoek",
  templateUrl: "./klant-zoek.component.html",
  styleUrls: ["./klant-zoek.component.less"],
  standalone: true,
  imports: [
    MatTabsModule,
    MatIconModule,
    TranslateModule,
    PersoonZoekComponent,
    BedrijfZoekComponent,
  ],
})
export class KlantZoekComponent {
  @Output() klant = new EventEmitter<
    GeneratedType<"RestBedrijf" | "RestPersoon">
  >();

  protected klantGeselecteerd(
    klant: GeneratedType<"RestBedrijf" | "RestPersoon">,
  ) {
    this.klant.emit(klant);
  }
}

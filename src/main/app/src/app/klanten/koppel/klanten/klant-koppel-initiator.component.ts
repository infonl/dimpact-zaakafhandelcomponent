/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, Output } from "@angular/core";
import { TranslateModule } from "@ngx-translate/core";
import { MaterialFormBuilderModule } from "src/app/shared/material-form-builder/material-form-builder.module";
import { SharedModule } from "src/app/shared/shared.module";
import { KlantenModule } from "../../klanten.module";
import { Klant } from "../../model/klanten/klant";
import { KlantGegevens } from "../../model/klanten/klant-gegevens";

@Component({
  selector: "zac-klant-koppel-initiator-persoon",
  standalone: true,
  imports: [
    SharedModule,
    TranslateModule,
    MaterialFormBuilderModule,
    KlantenModule,
  ],
  template: `
    <ng-template mat-tab-label>
      @if (type === "bedrijf") {
        <mat-icon>business</mat-icon>
      }
      @if (type === "persoon") {
        <mat-icon>emoji_people</mat-icon>
      }
      @if (type === "bedrijf") {
        <span>{{ "betrokkene.bedrijf" | translate }}</span>
      }
      @if (type === "persoon") {
        <span> {{ "betrokkene.persoon" | translate }}</span>
      }
    </ng-template>
    @if (type === "persoon") {
      <zac-persoon-zoek
        [syncEnabled]="true"
        (persoon)="klantGeselecteerd($event)"
      ></zac-persoon-zoek>
    }
    @if (type === "bedrijf") {
      <zac-bedrijf-zoek
        [syncEnabled]="true"
        (bedrijf)="klantGeselecteerd($event)"
      ></zac-bedrijf-zoek>
    }
  `,
})
export class KlantKoppelInitiator {
  @Input() type: "persoon" | "bedrijf" = "persoon";
  @Output() klantGegevens = new EventEmitter<KlantGegevens>();

  klantGeselecteerd(klant: Klant): void {
    this.klantGegevens.emit(new KlantGegevens(klant));
  }
}

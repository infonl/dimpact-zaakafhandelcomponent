/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, Output } from "@angular/core";
import { TranslateModule } from "@ngx-translate/core";
import { MaterialFormBuilderModule } from "src/app/shared/material-form-builder/material-form-builder.module";
import { SharedModule } from "src/app/shared/shared.module";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { KlantenModule } from "../../klanten.module";
import { KlantGegevens } from "../../model/klanten/klant-gegevens";

@Component({
    selector: "zac-klant-koppel-initiator-persoon",
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
        [zaaktypeUUID]="zaaktypeUUID"
        (persoon)="klantGeselecteerd($event)"
      ></zac-persoon-zoek>
    }
    @if (type === "bedrijf") {
      <zac-bedrijf-zoek
        [syncEnabled]="true"
        (bedrijf)="klantGeselecteerd($event)"
      ></zac-bedrijf-zoek>
    }
  `
})
export class KlantKoppelInitiator {
  @Input() type: "persoon" | "bedrijf" = "persoon";
  @Input() zaaktypeUUID?: string | null = null;
  @Output() klantGegevens = new EventEmitter<KlantGegevens>();

  klantGeselecteerd(klant: GeneratedType<"RestBedrijf" | "RestPersoon">): void {
    this.klantGegevens.emit(new KlantGegevens(klant));
  }
}

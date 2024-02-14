/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { Klant } from "../../model/klanten/klant";
import { SelectFormFieldBuilder } from "../../../shared/material-form-builder/form-components/select/select-form-field-builder";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { SelectFormField } from "../../../shared/material-form-builder/form-components/select/select-form-field";
import { KlantenService } from "../../klanten.service";
import { KlantGegevens } from "../../model/klanten/klant-gegevens";
import { InputFormField } from "../../../shared/material-form-builder/form-components/input/input-form-field";
import { InputFormFieldBuilder } from "../../../shared/material-form-builder/form-components/input/input-form-field-builder";
import { MatDrawer } from "@angular/material/sidenav";
import { KlantKoppelBetrokkeneComponent } from "./klant-koppel-betrokkene.component";
import { KlantKoppelInitiator } from "./klant-koppel-initiator.component";
import { MatTab, MatTabGroup } from "@angular/material/tabs";
import { MatIcon } from "@angular/material/icon";
import { TranslateModule } from "@ngx-translate/core";
import { SharedModule } from "src/app/shared/shared.module";

@Component({
  selector: "zac-klant-koppel",
  imports: [
    KlantKoppelBetrokkeneComponent,
    KlantKoppelInitiator,
    SharedModule,
    TranslateModule,
  ],
  template: `
    <div class="sidenav-title">
      <h3>
        <mat-icon>person_add_alt_1</mat-icon>
        {{
          (initiator
            ? "actie.initiator.toevoegen"
            : "actie.betrokkene.toevoegen"
          ) | translate
        }}
      </h3>
      <button mat-icon-button (click)="sideNav.close()">
        <mat-icon>close</mat-icon>
      </button>
    </div>

    <!--Initiator-->
    <mat-tab-group mat-stretch-tabs="false" *ngIf="initiator">
      <mat-tab>
        <ng-template mat-tab-label>
          <mat-icon>emoji_people</mat-icon>
          {{ "betrokkene.persoon" | translate }}
        </ng-template>
        <zac-klant-koppel-initiator-persoon
          type="persoon"
          (klantGegevens)="klantGegevens.emit($event)"
        />
      </mat-tab>
      <mat-tab>
        <ng-template mat-tab-label>
          <mat-icon>business</mat-icon>
          {{ "betrokkene.bedrijf" | translate }}
        </ng-template>
        <zac-klant-koppel-initiator-persoon
          type="bedrijf"
          (klantGegevens)="klantGegevens.emit($event)"
        />
      </mat-tab>
    </mat-tab-group>

    <!--Betrokkene-->
    <mat-tab-group mat-stretch-tabs="false" *ngIf="!initiator">
      <mat-tab>
        <ng-template mat-tab-label>
          <mat-icon>emoji_people</mat-icon>
          {{ "betrokkene.persoon" | translate }}
        </ng-template>
        <zac-klant-koppel-betrokkene-persoon
          type="persoon"
          [zaaktypeUUID]="zaaktypeUUID"
          (klantGegevens)="klantGegevens.emit($event)"
        />
      </mat-tab>
      <mat-tab>
        <ng-template mat-tab-label>
          <mat-icon>business</mat-icon>
          {{ "betrokkene.bedrijf" | translate }}
        </ng-template>
        <zac-klant-koppel-betrokkene-persoon
          type="bedrijf"
          [zaaktypeUUID]="zaaktypeUUID"
          (klantGegevens)="klantGegevens.emit($event)"
        />
      </mat-tab>
    </mat-tab-group>
  `,
  standalone: true,

  styleUrls: ["./klant-koppel.component.less"],
})
export class KlantKoppelComponent {
  @Input() initiator = false;
  @Input() zaaktypeUUID: string;
  @Input() sideNav: MatDrawer;
  @Output() klantGegevens = new EventEmitter<KlantGegevens>();
}

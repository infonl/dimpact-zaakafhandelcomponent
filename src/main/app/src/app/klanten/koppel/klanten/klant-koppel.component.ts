/*
 * SPDX-FileCopyrightText: 2024-2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, input, Input, Output } from "@angular/core";
import { MatDrawer } from "@angular/material/sidenav";
import { TranslateModule } from "@ngx-translate/core";
import { SharedModule } from "src/app/shared/shared.module";
import { KlantGegevens } from "../../model/klanten/klant-gegevens";
import { KlantKoppelBetrokkeneComponent } from "./klant-koppel-betrokkene.component";
import { KlantKoppelInitiator } from "./klant-koppel-initiator.component";

@Component({
  selector: "zac-klant-koppel",
  imports: [
    KlantKoppelBetrokkeneComponent,
    KlantKoppelInitiator,
    SharedModule,
    TranslateModule,
  ],
  template: `
    <div class="side-nav-container">
      <mat-toolbar role="heading" class="gap-16">
        <mat-icon>person_add_alt_1</mat-icon>
        <span class="flex-grow-1">
          {{
            (initiator
              ? "actie.initiator.toevoegen"
              : "actie.betrokkene.toevoegen"
            ) | translate
          }}
        </span>
        <button mat-icon-button (click)="sideNav.close()">
          <mat-icon>close</mat-icon>
        </button>
      </mat-toolbar>
      <mat-divider></mat-divider>

      <!--Initiator-->
      <mat-tab-group mat-stretch-tabs="false" *ngIf="initiator">
        <mat-tab *ngIf="allowPersoon">
          <ng-template mat-tab-label>
            <mat-icon>emoji_people</mat-icon>
            {{ "betrokkene.persoon" | translate }}
          </ng-template>
          <zac-klant-koppel-initiator-persoon
            type="persoon"
            [context]="this.context()"
            (klantGegevens)="klantGegevens.emit($event)"
          />
        </mat-tab>
        <mat-tab *ngIf="allowBedrijf">
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
        <mat-tab *ngIf="allowPersoon">
          <ng-template mat-tab-label>
            <mat-icon>emoji_people</mat-icon>
            {{ "betrokkene.persoon" | translate }}
          </ng-template>
          <zac-klant-koppel-betrokkene-persoon
            type="persoon"
            [context]="this.context()"
            [zaaktypeUUID]="zaaktypeUUID"
            (klantGegevens)="klantGegevens.emit($event)"
          />
        </mat-tab>
        <mat-tab *ngIf="allowBedrijf">
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

      <mat-action-row class="px-3">
        <button mat-raised-button (click)="sideNav.close()">
          {{ "actie.annuleren" | translate }}
        </button>
      </mat-action-row>
    </div>
  `,
  standalone: true,
})
export class KlantKoppelComponent {
  @Input() initiator = false;
  @Input() zaaktypeUUID: string;
  @Input() sideNav: MatDrawer;
  @Input() allowPersoon: boolean;
  @Input() allowBedrijf: boolean;
  @Output() klantGegevens = new EventEmitter<KlantGegevens>();

  context = input.required<string>();
}

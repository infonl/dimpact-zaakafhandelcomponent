/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import { Component, EventEmitter, Input, Output } from "@angular/core";
import { MatIconButton } from "@angular/material/button";
import {
  MatCard,
  MatCardContent,
  MatCardHeader,
  MatCardSubtitle,
  MatCardTitle,
} from "@angular/material/card";
import { MatIcon } from "@angular/material/icon";
import { MatTableDataSource } from "@angular/material/table";
import {
  MatTab,
  MatTabContent,
  MatTabGroup,
  MatTabLabel,
} from "@angular/material/tabs";
import { TranslatePipe } from "@ngx-translate/core";
import { IndicatiesLayout } from "../../../shared/indicaties/indicaties.component";
import { ZaakIndicatiesComponent } from "../../../shared/indicaties/zaak-indicaties/zaak-indicaties.component";
import { LocationPipe } from "../../../shared/pipes/location.pipe";
import { StaticTextComponent } from "../../../shared/static-text/static-text.component";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { ZaakBetrokkeneListComponent } from "../../zaak-betrokkenen-list/zaak-betrokkene-list.component";
import { ZaakDetailsAlgemeenTabComponent } from "./zaak-details-algemeen-tab/zaak-details-algemeen-tab.component";
import { ZaakDetailsBagObjectenTabComponent } from "./zaak-details-bag-objecten-tab/zaak-details-bag-objecten-tab.component";
import { ZaakDetailsGerelateerdeZakenTabComponent } from "./zaak-details-gerelateerde-zaken-tab/zaak-details-gerelateerde-zaken-tab.component";
import { LocatieTonenComponent } from "../../zaak-locatie-tonen/zaak-locatie-tonen.component";
import { ZaakHistorieComponent } from "../../zaken-historie/zaak-historie.component";

@Component({
  selector: "zac-zaak-details-card",
  templateUrl: "./zaak-details-card.component.html",
  styleUrls: ["./zaak-details-card.component.less"],
  standalone: true,
  imports: [
    NgIf,
    MatCard,
    MatCardContent,
    MatCardHeader,
    MatCardSubtitle,
    MatCardTitle,
    MatIcon,
    MatIconButton,
    MatTab,
    MatTabContent,
    MatTabGroup,
    MatTabLabel,
    StaticTextComponent,
    ZaakBetrokkeneListComponent,
    ZaakDetailsAlgemeenTabComponent,
    ZaakDetailsBagObjectenTabComponent,
    ZaakDetailsGerelateerdeZakenTabComponent,
    ZaakHistorieComponent,
    ZaakIndicatiesComponent,
    LocatieTonenComponent,
    LocationPipe,
    TranslatePipe,
  ],
})
export class ZaakDetailsCardComponent {
  @Input({ required: true }) zaak!: GeneratedType<"RestZaak">;
  @Input() zaakOpschorting?: GeneratedType<"RESTZaakOpschorting">;
  @Input({ required: true }) bagObjectenDataSource!: MatTableDataSource<
    GeneratedType<"RESTBAGObjectGegevens">
  >;
  @Input() showBetrokkeneKoppelingen = false;

  @Output() editCaseDetails = new EventEmitter<void>();
  @Output() editLocationDetails = new EventEmitter<void>();
  @Output() zaakOntkoppelen = new EventEmitter<
    GeneratedType<"RestGerelateerdeZaak">
  >();
  @Output() bagObjectVerwijderen = new EventEmitter<
    GeneratedType<"RESTBAGObjectGegevens">
  >();

  protected readonly indicatiesLayout = IndicatiesLayout;
}

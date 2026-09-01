/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, input, output } from "@angular/core";
import { MatIconButton } from "@angular/material/button";
import {
  MatCard,
  MatCardContent,
  MatCardHeader,
  MatCardSubtitle,
  MatCardTitle,
} from "@angular/material/card";
import { MatIcon } from "@angular/material/icon";
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
import { LocatieTonenComponent } from "../../zaak-locatie-tonen/zaak-locatie-tonen.component";
import { ZaakHistorieComponent } from "../../zaken-historie/zaak-historie.component";
import { ZaakDetailsAlgemeenTabComponent } from "./zaak-details-algemeen-tab/zaak-details-algemeen-tab.component";
import { ZaakDetailsBagObjectenTabComponent } from "./zaak-details-bag-objecten-tab/zaak-details-bag-objecten-tab.component";
import { ZaakDetailsGerelateerdeZakenTabComponent } from "./zaak-details-gerelateerde-zaken-tab/zaak-details-gerelateerde-zaken-tab.component";

@Component({
  selector: "zac-zaak-details-card",
  templateUrl: "./zaak-details-card.component.html",
  styleUrls: ["./zaak-details-card.component.less"],
  standalone: true,
  imports: [
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
  readonly zaak = input.required<GeneratedType<"RestZaak">>();
  readonly zaakOpschorting = input<GeneratedType<"RESTZaakOpschorting">>();
  readonly bagObjecten =
    input.required<GeneratedType<"RESTBAGObjectGegevens">[]>();
  readonly showBetrokkeneKoppelingen = input(false);

  readonly editCaseDetails = output<void>();
  readonly editLocationDetails = output<void>();
  readonly zaakOntkoppelen = output<GeneratedType<"RestGerelateerdeZaak">>();
  readonly bagObjectVerwijderen =
    output<GeneratedType<"RESTBAGObjectGegevens">>();

  protected readonly indicatiesLayout = IndicatiesLayout;
}

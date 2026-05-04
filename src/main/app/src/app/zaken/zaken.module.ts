/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";

import { BagZoekComponent } from "../bag/bag-zoek/bag-zoek.component";
import { GebruikersvoorkeurenModule } from "../gebruikersvoorkeuren/gebruikersvoorkeuren.module";
import { InformatieObjectenModule } from "../informatie-objecten/informatie-objecten.module";
import { KlantenModule } from "../klanten/klanten.module";
import { KlantKoppelComponent } from "../klanten/koppel/klanten/klant-koppel.component";
import { MailModule } from "../mail/mail.module";
import { NotitiesComponent } from "../notities/notities.component";
import { PlanItemsModule } from "../plan-items/plan-items.module";
import { DocumentIconComponent } from "../shared/document-icon/document-icon.component";
import { InformatieObjectIndicatiesComponent } from "../shared/indicaties/informatie-object-indicaties/informatie-object-indicaties.component";
import { MimetypeToExtensionPipe } from "../shared/pipes/mimetypeToExtension.pipe";
import { SharedModule } from "../shared/shared.module";
import { ZoekenModule } from "../zoeken/zoeken.module";
import { BesluitCreateComponent } from "./besluit-create/besluit-create.component";
import { BesluitEditComponent } from "./besluit-edit/besluit-edit.component";
import { BesluitViewComponent } from "./besluit-view/besluit-view.component";
import { ZaakCreateComponent } from "./zaak-create/zaak-create.component";
import { CaseDetailsEditComponent } from "./zaak-details-wijzigen/zaak-details-wijzigen.component";
import { ZaakDocumentenComponent } from "./zaak-documenten/zaak-documenten.component";
import { ZaakLinkComponent } from "./zaak-link/zaak-link.component";
import { LocatieTonenComponent } from "./zaak-locatie-tonen/zaak-locatie-tonen.component";
import { CaseLocationEditComponent } from "./zaak-locatie-wijzigen/zaak-locatie-wijzigen.component";
import { ZaakProcessFlowComponent } from "./zaak-process-flow/zaak-process-flow.component";
import { ZaakVerkortComponent } from "./zaak-verkort/zaak-verkort.component";
import { BetrokkeneLinkComponent } from "./zaak-view/betrokkene-link.component";
import { ZaakViewComponent } from "./zaak-view/zaak-view.component";
import { ZakenAfgehandeldComponent } from "./zaken-afgehandeld/zaken-afgehandeld.component";
import { ZakenMijnComponent } from "./zaken-mijn/zaken-mijn.component";
import { ZakenRoutingModule } from "./zaken-routing.module";
import { ZakenWerkvoorraadComponent } from "./zaken-werkvoorraad/zaken-werkvoorraad.component";

@NgModule({
  declarations: [
    BesluitCreateComponent,
    BesluitEditComponent,
    BesluitViewComponent,
    ZaakViewComponent,
    ZaakCreateComponent,
    ZakenWerkvoorraadComponent,
    ZakenMijnComponent,
    CaseDetailsEditComponent,
    ZaakLinkComponent,
    ZaakDocumentenComponent,
  ],
  exports: [ZaakVerkortComponent, ZaakDocumentenComponent],
  imports: [
    NotitiesComponent,
    SharedModule,
    ZakenAfgehandeldComponent,
    ZakenRoutingModule,
    KlantenModule,
    InformatieObjectenModule,
    PlanItemsModule,
    MailModule,
    ZoekenModule,
    GebruikersvoorkeurenModule,
    BagZoekComponent,
    DocumentIconComponent,
    InformatieObjectIndicatiesComponent,
    KlantKoppelComponent,
    MimetypeToExtensionPipe,
    CaseLocationEditComponent,
    LocatieTonenComponent,
    ZaakProcessFlowComponent,
    ZaakVerkortComponent,
    BetrokkeneLinkComponent,
  ],
})
export class ZakenModule {}

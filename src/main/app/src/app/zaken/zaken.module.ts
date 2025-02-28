/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";

import { BAGModule } from "../bag/bag.module";
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
import { IntakeAfrondenDialogComponent } from "./intake-afronden-dialog/intake-afronden-dialog.component";
import { ZaakAfhandelenDialogComponent } from "./zaak-afhandelen-dialog/zaak-afhandelen-dialog.component";
import { ZaakCreateComponent } from "./zaak-create/zaak-create.component";
import { CaseDetailsEditComponent } from "./zaak-details-wijzigen/zaak-details-wijzigen.component";
import { ZaakDocumentenComponent } from "./zaak-documenten/zaak-documenten.component";
import { ZaakInitiatorToevoegenComponent } from "./zaak-initiator-toevoegen/zaak-initiator-toevoegen.component";
import { ZaakKoppelenDialogComponent } from "./zaak-koppelen/zaak-koppelen-dialog.component";
import { ZaakOntkoppelenDialogComponent } from "./zaak-ontkoppelen/zaak-ontkoppelen-dialog.component";
import { ZaakOpschortenDialogComponent } from "./zaak-opschorten-dialog/zaak-opschorten-dialog.component";
import { ZaakVerkortComponent } from "./zaak-verkort/zaak-verkort.component";
import { ZaakVerlengenDialogComponent } from "./zaak-verlengen-dialog/zaak-verlengen-dialog.component";
import { ZaakViewComponent } from "./zaak-view/zaak-view.component";
import { ZaakdataFormComponent } from "./zaakdata/zaakdata-form/zaakdata-form.component";
import { ZaakdataComponent } from "./zaakdata/zaakdata.component";
import { ZakenAfgehandeldComponent } from "./zaken-afgehandeld/zaken-afgehandeld.component";
import { ZakenMijnComponent } from "./zaken-mijn/zaken-mijn.component";
import { ZakenRoutingModule } from "./zaken-routing.module";
import { ZakenVerdelenDialogComponent } from "./zaken-verdelen-dialog/zaken-verdelen-dialog.component";
import { ZakenVrijgevenDialogComponent } from "./zaken-vrijgeven-dialog/zaken-vrijgeven-dialog.component";
import { ZakenWerkvoorraadComponent } from "./zaken-werkvoorraad/zaken-werkvoorraad.component";
import { LocatieZoekComponent } from "./zoek/locatie-zoek/locatie-zoek.component";

@NgModule({
  declarations: [
    BesluitCreateComponent,
    BesluitEditComponent,
    BesluitViewComponent,
    IntakeAfrondenDialogComponent,
    ZaakViewComponent,
    ZaakVerkortComponent,
    ZaakCreateComponent,
    ZakenWerkvoorraadComponent,
    ZakenMijnComponent,
    ZakenAfgehandeldComponent,
    ZaakAfhandelenDialogComponent,
    ZakenVerdelenDialogComponent,
    ZaakKoppelenDialogComponent,
    ZaakOntkoppelenDialogComponent,
    ZakenVrijgevenDialogComponent,
    ZaakOpschortenDialogComponent,
    ZaakVerlengenDialogComponent,
    ZaakInitiatorToevoegenComponent,
    CaseDetailsEditComponent,
    NotitiesComponent,
    LocatieZoekComponent,
    ZaakDocumentenComponent,
    ZaakdataComponent,
    ZaakdataFormComponent,
  ],
  exports: [ZaakVerkortComponent, ZaakDocumentenComponent],
  imports: [
    SharedModule,
    ZakenRoutingModule,
    KlantenModule,
    InformatieObjectenModule,
    PlanItemsModule,
    MailModule,
    ZoekenModule,
    GebruikersvoorkeurenModule,
    BAGModule,
    DocumentIconComponent,
    InformatieObjectIndicatiesComponent,
    KlantKoppelComponent,
    MimetypeToExtensionPipe,
  ],
})
export class ZakenModule {}

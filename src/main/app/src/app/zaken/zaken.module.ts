/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";

import { BagZoekComponent } from "../bag/bag-zoek/bag-zoek.component";
import { InformatieObjectVerzendenComponent } from "../informatie-objecten/informatie-object-verzenden/informatie-object-verzenden.component";
import { InformatieObjectenModule } from "../informatie-objecten/informatie-objecten.module";
import { KlantenModule } from "../klanten/klanten.module";
import { KlantKoppelComponent } from "../klanten/koppel/klanten/klant-koppel/klant-koppel.component";
import { MailCreateComponent } from "../mail/mail-create/mail-create.component";
import { OntvangstbevestigingComponent } from "../mail/ontvangstbevestiging/ontvangstbevestiging.component";
import { NotitiesComponent } from "../notities/notities.component";
import { HumanTaskDoComponent } from "../plan-items/human-task-do/human-task-do.component";
import { PlanItemsModule } from "../plan-items/plan-items.module";
import { DocumentIconComponent } from "../shared/document-icon/document-icon.component";
import { InformatieObjectIndicatiesComponent } from "../shared/indicaties/informatie-object-indicaties/informatie-object-indicaties.component";
import { MimetypeToExtensionPipe } from "../shared/pipes/mimetypeToExtension.pipe";
import { SharedModule } from "../shared/shared.module";
import { BesluitCreateComponent } from "./besluit-create/besluit-create.component";
import { BesluitEditComponent } from "./besluit-edit/besluit-edit.component";
import { BesluitViewComponent } from "./besluit-view/besluit-view.component";
import { BetrokkeneLinkComponent } from "./zaak-betrokkenen/betrokkene-link.component";
import { CaseDetailsEditComponent } from "./zaak-details-wijzigen/zaak-details-wijzigen.component";

import { ZaakDocumentenComponent } from "./zaak-documenten/zaak-documenten.component";
import { ZaakInitiatorToevoegenComponent } from "./zaak-initiator-toevoegen/zaak-initiator-toevoegen.component";
import { ZaakLinkComponent } from "./zaak-link/zaak-link.component";
import { LocatieTonenComponent } from "./zaak-locatie-tonen/zaak-locatie-tonen.component";
import { CaseLocationEditComponent } from "./zaak-locatie-wijzigen/zaak-locatie-wijzigen.component";
import { ZaakProcessFlowComponent } from "./zaak-process-flow/zaak-process-flow.component";
import { ZaakTakenComponent } from "./zaak-taken/zaak-taken.component";
import { ZaakVerkortComponent } from "./zaak-verkort/zaak-verkort.component";
import { ZaakViewComponent } from "./zaak-view/zaak-view.component";
import { ZakenRoutingModule } from "./zaken-routing.module";

@NgModule({
  declarations: [BesluitEditComponent, ZaakViewComponent],
  exports: [ZaakVerkortComponent, ZaakDocumentenComponent],
  imports: [
    NotitiesComponent,
    SharedModule,
    BesluitViewComponent,
    ZaakDocumentenComponent,
    ZakenRoutingModule,
    KlantenModule,
    InformatieObjectenModule,
    InformatieObjectVerzendenComponent,
    HumanTaskDoComponent,
    PlanItemsModule,
    MailCreateComponent,
    OntvangstbevestigingComponent,
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
    ZaakInitiatorToevoegenComponent,
    ZaakTakenComponent,
    BesluitCreateComponent,
    CaseDetailsEditComponent,
    ZaakLinkComponent,
  ],
})
export class ZakenModule {}

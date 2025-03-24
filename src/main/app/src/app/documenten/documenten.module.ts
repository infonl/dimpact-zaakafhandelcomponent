/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";

import { GebruikersvoorkeurenModule } from "../gebruikersvoorkeuren/gebruikersvoorkeuren.module";
import { SharedModule } from "../shared/shared.module";
import { ZoekenModule } from "../zoeken/zoeken.module";
import { DocumentenRoutingModule } from "./documenten-routing.module";
import { InboxDocumentenListComponent } from "./inbox-documenten-list/inbox-documenten-list.component";
import { OntkoppeldeDocumentenListComponent } from "./ontkoppelde-documenten-list/ontkoppelde-documenten-list.component";
import { InformatieObjectLinkComponent } from "../informatie-objecten/informatie-object-link/informatie-object-link.component";

@NgModule({
  declarations: [
    OntkoppeldeDocumentenListComponent,
    InboxDocumentenListComponent,
    InformatieObjectLinkComponent,
  ],
  imports: [
    SharedModule,
    DocumentenRoutingModule,
    ZoekenModule,
    GebruikersvoorkeurenModule,
  ],
})
export class DocumentenModule {}

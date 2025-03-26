/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";

import { RouteReuseStrategy } from "@angular/router";
import { DocumentIconComponent } from "../shared/document-icon/document-icon.component";
import { InformatieObjectIndicatiesComponent } from "../shared/indicaties/informatie-object-indicaties/informatie-object-indicaties.component";
import { MimetypeToExtensionPipe } from "../shared/pipes/mimetypeToExtension.pipe";
import { SharedModule } from "../shared/shared.module";
import { InformatieObjectAddComponent } from "./informatie-object-add/informatie-object-add.component";
import { InformatieObjectCreateAttendedComponent } from "./informatie-object-create-attended/informatie-object-create-attended.component";
import { InformatieObjectEditComponent } from "./informatie-object-edit/informatie-object-edit.component";
import { InformatieObjectVerzendenComponent } from "./informatie-object-verzenden/informatie-object-verzenden.component";
import { InformatieObjectViewComponent } from "./informatie-object-view/informatie-object-view.component";
import { InformatieObjectenRoutingModule } from "./informatie-objecten-routing.module";
import { RouteReuseStrategyService } from "./route-reuse-strategy.service";
import { InformatieObjectLinkComponent } from "./informatie-object-link/informatie-object-link.component";

@NgModule({
  declarations: [
    InformatieObjectViewComponent,
    InformatieObjectEditComponent,
    InformatieObjectAddComponent,
    InformatieObjectCreateAttendedComponent,
    InformatieObjectVerzendenComponent,
    InformatieObjectLinkComponent,
  ],
  exports: [
    InformatieObjectAddComponent,
    InformatieObjectCreateAttendedComponent,
    InformatieObjectVerzendenComponent,
    InformatieObjectLinkComponent,
  ],
  providers: [
    { provide: RouteReuseStrategy, useClass: RouteReuseStrategyService },
  ],
  imports: [
    SharedModule,
    InformatieObjectenRoutingModule,
    DocumentIconComponent,
    InformatieObjectIndicatiesComponent,
    MimetypeToExtensionPipe,
  ],
})
export class InformatieObjectenModule {}

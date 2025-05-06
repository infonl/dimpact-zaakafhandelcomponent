/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";

import { MatExpansionModule } from "@angular/material/expansion";
import { SharedModule } from "../shared/shared.module";
import { FoutDialogComponent } from "./dialog/fout-dialog.component";
import { FoutAfhandelingRoutingModule } from "./fout-afhandeling-routing.module";
import { FoutAfhandelingComponent } from "./fout-afhandeling.component";
import { ActieOnmogelijkDialogComponent } from "./dialog/actie-onmogelijk-dialog.component";

@NgModule({
  declarations: [
    FoutAfhandelingComponent,
    FoutDialogComponent,
    ActieOnmogelijkDialogComponent,
  ],
  exports: [FoutAfhandelingComponent],
  imports: [SharedModule, MatExpansionModule, FoutAfhandelingRoutingModule],
})
export class FoutAfhandelingModule {}

/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";
import { MaterialModule } from "../shared/material/material.module";
import { IdentityComponent } from "./identity.component";
import { IdentityService } from "./identity.service";

@NgModule({
  providers: [IdentityService],
  declarations: [IdentityComponent],
  imports: [MaterialModule],
})
export class IdentityModule {}

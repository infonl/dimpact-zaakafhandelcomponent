/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";
import { SharedModule } from "../shared/shared.module";
import { SignaleringenRoutingModule } from "./signaleringen-routing.module";
import { SignaleringenSettingsComponent } from "./signaleringen-settings/signaleringen-settings.component";

@NgModule({
  imports: [SignaleringenSettingsComponent,SharedModule, SignaleringenRoutingModule],
})
export class SignaleringenModule {}

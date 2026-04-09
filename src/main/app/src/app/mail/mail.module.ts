/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";

import { SharedModule } from "../shared/shared.module";
import { MailCreateComponent } from "./mail-create/mail-create.component";
import { OntvangstbevestigingComponent } from "./ontvangstbevestiging/ontvangstbevestiging.component";

@NgModule({
  imports: [SharedModule, MailCreateComponent, OntvangstbevestigingComponent],
  exports: [MailCreateComponent, OntvangstbevestigingComponent],
})
export class MailModule {}

/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";

import { SharedModule } from "../shared/shared.module";
import { ProcessTaskDoComponent } from "./process-task-do/process-task-do.component";

@NgModule({
  declarations: [ProcessTaskDoComponent],
  exports: [ProcessTaskDoComponent],
  imports: [SharedModule],
})
export class PlanItemsModule {}

/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";

import { FormulierenModule } from "../formulieren/formulieren.module";
import { SharedModule } from "../shared/shared.module";
import { HumanTaskDoComponent } from "./human-task-do/human-task-do.component";
import { ProcessTaskDoComponent } from "./process-task-do/process-task-do.component";

@NgModule({
  declarations: [HumanTaskDoComponent, ProcessTaskDoComponent],
  exports: [HumanTaskDoComponent, ProcessTaskDoComponent],
  imports: [SharedModule, FormulierenModule],
})
export class PlanItemsModule {}

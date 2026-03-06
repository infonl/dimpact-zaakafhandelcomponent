/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";
import { BestandsomvangPipe } from "./bestandsomvang.pipe";
import { DagenPipe } from "./dagen.pipe";
import { DatumPipe } from "./datum.pipe";
import { LocationPipe } from "./location.pipe";

@NgModule({
  imports: [BestandsomvangPipe, DatumPipe, LocationPipe, DagenPipe],
  exports: [BestandsomvangPipe, DatumPipe, LocationPipe, DagenPipe],
})
export class PipesModule {}

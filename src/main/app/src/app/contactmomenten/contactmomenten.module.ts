/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgModule } from "@angular/core";
import { SharedModule } from "../shared/shared.module";
import { KlantContactmomentenTabelComponent } from "./klant-contactmomenten-tabel/klant-contactmomenten-tabel.component";

@NgModule({
  imports: [SharedModule, KlantContactmomentenTabelComponent],
  exports: [KlantContactmomentenTabelComponent],
})
export class ContactmomentenModule {}

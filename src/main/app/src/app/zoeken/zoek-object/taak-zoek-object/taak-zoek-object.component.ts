/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input } from "@angular/core";
import { MatSidenav } from "@angular/material/sidenav";
import { TranslateModule } from "@ngx-translate/core";
import { DatumPipe } from "../../../shared/pipes/datum.pipe";
import { StaticTextComponent } from "../../../shared/static-text/static-text.component";
import { ZoekObjectLinkComponent } from "../zoek-object-link/zoek-object-link.component";
import { TaakZoekObject } from "../../model/taken/taak-zoek-object";
import { ZoekObjectComponent } from "../zoek-object/zoek-object-component";

@Component({
  selector: "zac-taak-zoek-object",
  styleUrls: ["../zoek-object/zoek-object.component.less"],
  templateUrl: "./taak-zoek-object.component.html",
  standalone: true,
  imports: [ZoekObjectLinkComponent, StaticTextComponent, DatumPipe, TranslateModule],
})
export class TaakZoekObjectComponent extends ZoekObjectComponent {
  @Input({ required: true }) taak!: TaakZoekObject;
  @Input({ required: true }) sideNav!: MatSidenav;
}

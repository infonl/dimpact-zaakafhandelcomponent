/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input } from "@angular/core";
import { MatSidenav } from "@angular/material/sidenav";
import { TranslateModule } from "@ngx-translate/core";
import { DatumPipe } from "../../../shared/pipes/datum.pipe";
import { StaticTextComponent } from "../../../shared/static-text/static-text.component";
import { ZaakZoekObject } from "../../model/zaken/zaak-zoek-object";
import { ZoekObjectLinkComponent } from "../zoek-object-link/zoek-object-link.component";
import { ZoekObjectComponent } from "../zoek-object/zoek-object-component";

@Component({
  selector: "zac-zaak-zoek-object",
  styleUrls: ["../zoek-object/zoek-object.component.less"],
  templateUrl: "./zaak-zoek-object.component.html",
  standalone: true,
  imports: [
    ZoekObjectLinkComponent,
    StaticTextComponent,
    DatumPipe,
    TranslateModule,
  ],
})
export class ZaakZoekObjectComponent extends ZoekObjectComponent {
  @Input({ required: true }) zaak!: ZaakZoekObject;
  @Input({ required: true }) sideNav!: MatSidenav;
}

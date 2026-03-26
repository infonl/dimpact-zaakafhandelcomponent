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
import { DocumentZoekObject } from "../../model/documenten/document-zoek-object";
import { ZoekObjectComponent } from "../zoek-object/zoek-object-component";

@Component({
  selector: "zac-document-zoek-object",
  styleUrls: ["../zoek-object/zoek-object.component.less"],
  templateUrl: "./document-zoek-object.component.html",
  standalone: true,
  imports: [ZoekObjectLinkComponent, StaticTextComponent, DatumPipe, TranslateModule],
})
export class DocumentZoekObjectComponent extends ZoekObjectComponent {
  @Input({ required: true }) document!: DocumentZoekObject;
  @Input({ required: true }) sideNav!: MatSidenav;
}

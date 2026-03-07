/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input } from "@angular/core";
import { MatSidenav } from "@angular/material/sidenav";
import { TaakZoekObject } from "../../model/taken/taak-zoek-object";
import { ZoekObjectComponent } from "../zoek-object/zoek-object-component";

@Component({
  selector: "zac-taak-zoek-object",
  styleUrls: ["../zoek-object/zoek-object.component.less"],
  templateUrl: "./taak-zoek-object.component.html",
  standalone: false,
})
export class TaakZoekObjectComponent extends ZoekObjectComponent {
  @Input() taak: TaakZoekObject;
  @Input() sideNav: MatSidenav;
}

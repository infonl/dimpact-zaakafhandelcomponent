/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Output } from "@angular/core";
import { Klant } from "../../model/klanten/klant";

@Component({
  selector: "zac-klant-zoek",
  templateUrl: "./klant-zoek.component.html",
  styleUrls: ["./klant-zoek.component.less"],
})
export class KlantZoekComponent {
  @Output() klant = new EventEmitter<Klant>();

  constructor() {}

  klantGeselecteerd(klant: Klant): void {
    this.klant.emit(klant);
  }
}

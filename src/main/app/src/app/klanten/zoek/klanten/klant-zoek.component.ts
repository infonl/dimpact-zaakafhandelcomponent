/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Output } from "@angular/core";
import { GeneratedType } from "../../../shared/utils/generated-types";

@Component({
    selector: "zac-klant-zoek",
    templateUrl: "./klant-zoek.component.html",
    styleUrls: ["./klant-zoek.component.less"],
    standalone: false
})
export class KlantZoekComponent {
  @Output() klant = new EventEmitter<
    GeneratedType<"RestBedrijf" | "RestPersoon">
  >();

  klantGeselecteerd(klant: GeneratedType<"RestBedrijf" | "RestPersoon">): void {
    this.klant.emit(klant);
  }
}

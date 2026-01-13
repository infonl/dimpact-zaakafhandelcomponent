/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, Output } from "@angular/core";

@Component({
  selector: "zac-zaak-initiator-toevoegen",
  templateUrl: "./zaak-initiator-toevoegen.component.html",
  styleUrls: ["./zaak-initiator-toevoegen.component.less"],
  standalone: false,
})
export class ZaakInitiatorToevoegenComponent {
  @Input({ required: true }) toevoegenToegestaan = false;
  @Output() add = new EventEmitter<void>();

  constructor() {}
}

/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import { Component, EventEmitter, Output, input } from "@angular/core";
import { MatIconButton } from "@angular/material/button";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatIconModule } from "@angular/material/icon";
import { TranslateModule } from "@ngx-translate/core";

@Component({
  selector: "zac-zaak-initiator-toevoegen",
  templateUrl: "./zaak-initiator-toevoegen.component.html",
  styleUrls: ["./zaak-initiator-toevoegen.component.less"],
  standalone: true,
  imports: [
    NgIf,
    MatExpansionModule,
    MatIconModule,
    MatIconButton,
    TranslateModule,
  ],
})
export class ZaakInitiatorToevoegenComponent {
  readonly toevoegenToegestaan = input.required<boolean>();
  @Output() add = new EventEmitter<void>();
}

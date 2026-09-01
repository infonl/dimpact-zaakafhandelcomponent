/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, input, output } from "@angular/core";
import { MatIconAnchor, MatIconButton } from "@angular/material/button";
import { MatIcon } from "@angular/material/icon";
import {
  MatCell,
  MatCellDef,
  MatColumnDef,
  MatHeaderCell,
  MatHeaderCellDef,
  MatHeaderRow,
  MatHeaderRowDef,
  MatRow,
  MatRowDef,
  MatTable,
} from "@angular/material/table";
import { RouterLink } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import { DatumPipe } from "../../../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../../../shared/pipes/empty.pipe";
import { GeneratedType } from "../../../../shared/utils/generated-types";

@Component({
  selector: "zac-zaak-details-gerelateerde-zaken-tab",
  templateUrl: "./zaak-details-gerelateerde-zaken-tab.component.html",
  styleUrls: ["./zaak-details-gerelateerde-zaken-tab.component.less"],
  styles: [":host { display: block; }"],
  standalone: true,
  imports: [
    RouterLink,
    MatIcon,
    MatIconAnchor,
    MatIconButton,
    MatTable,
    MatColumnDef,
    MatHeaderCell,
    MatHeaderCellDef,
    MatCell,
    MatCellDef,
    MatHeaderRow,
    MatHeaderRowDef,
    MatRow,
    MatRowDef,
    DatumPipe,
    EmptyPipe,
    TranslatePipe,
  ],
})
export class ZaakDetailsGerelateerdeZakenTabComponent {
  readonly gerelateerdeZaken =
    input.required<GeneratedType<"RestGerelateerdeZaak">[]>();

  readonly zaakOntkoppelen = output<GeneratedType<"RestGerelateerdeZaak">>();

  protected gerelateerdeZaakColumns = [
    "identificatie",
    "zaaktypeOmschrijving",
    "statustypeOmschrijving",
    "startdatum",
    "relatieType",
  ] as const;

  protected gerelateerdeZaakColumnsWithAction = [
    ...this.gerelateerdeZaakColumns,
    "actions",
  ];
}

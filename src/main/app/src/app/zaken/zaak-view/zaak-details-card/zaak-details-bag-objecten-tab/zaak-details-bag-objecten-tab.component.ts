/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, input, output } from "@angular/core";
import { MatIconAnchor, MatIconButton } from "@angular/material/button";
import { MatIcon } from "@angular/material/icon";
import { MatSort, MatSortHeader } from "@angular/material/sort";
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
  MatTableDataSource,
} from "@angular/material/table";
import { RouterLink } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import { ReadMoreComponent } from "../../../../shared/read-more/read-more.component";
import { GeneratedType } from "../../../../shared/utils/generated-types";

@Component({
  selector: "zac-zaak-details-bag-objecten-tab",
  templateUrl: "./zaak-details-bag-objecten-tab.component.html",
  styleUrls: ["./zaak-details-bag-objecten-tab.component.less"],
  styles: [":host { display: block; }"],
  standalone: true,
  imports: [
    RouterLink,
    MatIcon,
    MatIconAnchor,
    MatIconButton,
    MatSort,
    MatSortHeader,
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
    ReadMoreComponent,
    TranslatePipe,
  ],
})
export class ZaakDetailsBagObjectenTabComponent {
  readonly bagObjectenDataSource =
    input.required<
      MatTableDataSource<GeneratedType<"RESTBAGObjectGegevens">>
    >();
  readonly isOntkoppelenToegestaan = input(false);

  readonly bagObjectVerwijderen =
    output<GeneratedType<"RESTBAGObjectGegevens">>();

  protected bagObjectenColumns = [
    "identificatie",
    "type",
    "omschrijving",
    "actions",
  ] as const;
}

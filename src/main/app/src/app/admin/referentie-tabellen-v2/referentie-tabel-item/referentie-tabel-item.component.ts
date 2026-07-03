/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, input, output } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatTableModule } from "@angular/material/table";
import { MatTooltipModule } from "@angular/material/tooltip";
import { TranslateModule } from "@ngx-translate/core";
import { EmptyPipe } from "../../../shared/pipes/empty.pipe";
import { GeneratedType } from "../../../shared/utils/generated-types";

/**
 * Expanded content of a single reference table row: a table of its values,
 * each editable and deletable. Mutations are emitted for the parent to persist.
 */
@Component({
  standalone: true,
  selector: "zac-referentie-tabel-item",
  templateUrl: "./referentie-tabel-item.component.html",
  styleUrls: ["./referentie-tabel-item.component.less"],
  imports: [
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    TranslateModule,
    EmptyPipe,
  ],
})
export class ReferentieTabelItemComponent {
  readonly tabel = input.required<GeneratedType<"RestReferenceTable">>();

  readonly addWaarde = output<void>();
  readonly editWaarde = output<GeneratedType<"RestReferenceTableValue">>();
  readonly deleteWaarde = output<GeneratedType<"RestReferenceTableValue">>();

  protected readonly columns = ["index", "naam", "actions"] as const;
}

/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input, OnChanges } from "@angular/core";
import { MatIconAnchor } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatIconModule } from "@angular/material/icon";
import { RouterLink } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { UtilService } from "../../core/service/util.service";
import { TextIcon } from "../../shared/edit/text-icon";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { DateConditionals } from "../../shared/utils/date-conditionals";
import { GeneratedType } from "../../shared/utils/generated-types";

@Component({
  selector: "zac-zaak-verkort",
  templateUrl: "./zaak-verkort.component.html",
  styleUrls: ["./zaak-verkort.component.less"],
  standalone: true,
  imports: [
    MatCardModule,
    MatIconModule,
    MatIconAnchor,
    RouterLink,
    TranslateModule,
    StaticTextComponent,
    EmptyPipe,
    DatumPipe,
  ],
})
export class ZaakVerkortComponent implements OnChanges {
  @Input({ required: true }) zaak!: GeneratedType<"RestZaak">;

  protected einddatumGeplandIcon: TextIcon | null = null;

  constructor(private readonly utilService: UtilService) {}

  ngOnChanges(): void {
    this.einddatumGeplandIcon = new TextIcon(
      DateConditionals.provideFormControlValue(
        DateConditionals.isExceeded,
        this.zaak.einddatum ?? "",
      ),
      "report_problem",
      "warningZaakVerkortVerlopen_icon",
      "msg.datum.overschreden",
      "warning",
    );
  }
}

/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2025 INFO
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input, OnChanges } from "@angular/core";
import { UtilService } from "../../core/service/util.service";
import { TextIcon } from "../../shared/edit/text-icon";
import { DateConditionals } from "../../shared/utils/date-conditionals";
import { GeneratedType } from "../../shared/utils/generated-types";

@Component({
  selector: "zac-zaak-verkort",
  templateUrl: "./zaak-verkort.component.html",
  styleUrls: ["./zaak-verkort.component.less"],
})
export class ZaakVerkortComponent implements OnChanges {
  @Input({ required: true }) zaak!: GeneratedType<"RestZaak">;

  einddatumGeplandIcon: TextIcon | null = null;

  constructor(public readonly utilService: UtilService) {}

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

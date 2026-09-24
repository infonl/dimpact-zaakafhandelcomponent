/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { CommonModule } from "@angular/common";
import { Component, computed, inject, input } from "@angular/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { MaterialModule } from "../../material/material.module";
import { BesluitIndicatie } from "../../model/indicatie";
import { IndicatieItem } from "../../model/indicatie-item";
import { GeneratedType } from "../../utils/generated-types";
import { IndicatiesComponent } from "../indicaties.component";

@Component({
  selector: "zac-besluit-indicaties",
  templateUrl: "../indicaties.component.html",
  styleUrls: ["../indicaties.component.less"],
  standalone: true,
  imports: [CommonModule, MaterialModule, TranslateModule],
})
export class BesluitIndicatiesComponent extends IndicatiesComponent {
  private readonly translate = inject(TranslateService);

  readonly besluit = input.required<GeneratedType<"RestBesluit">>();

  protected readonly indicaties = computed(() => {
    const besluit = this.besluit();
    if (!besluit.isIngetrokken) {
      return [];
    }

    return [
      new IndicatieItem(
        BesluitIndicatie.INGETROKKEN,
        "stop",
        this.getIntrekToelichting(besluit),
      ),
    ];
  });

  private getIntrekToelichting(besluit: GeneratedType<"RestBesluit">) {
    return this.translate.instant("besluit.vervalreden." + besluit.vervalreden);
  }
}

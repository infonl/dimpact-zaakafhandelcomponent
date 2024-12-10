/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input, OnChanges, SimpleChanges } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { Indicatie } from "../../model/indicatie";
import { GeneratedType } from "../../utils/generated-types";
import { IndicatiesComponent } from "../indicaties.component";

export enum BesluitIndicatie {
  INGETROKKEN = "INGETROKKEN",
}

@Component({
  selector: "zac-besluit-indicaties",
  templateUrl: "../indicaties.component.html",
  styleUrls: ["../indicaties.component.less"],
})
export class BesluitIndicatiesComponent
  extends IndicatiesComponent
  implements OnChanges
{
  @Input() besluit: GeneratedType<"RestDecision">;

  constructor(private translate: TranslateService) {
    super();
  }

  ngOnChanges(changes: SimpleChanges): void {
    this.besluit = changes.besluit?.currentValue;
    this.loadIndicaties();
  }

  private loadIndicaties(): void {
    this.indicaties = [];
    if (this.besluit.isIngetrokken) {
      this.indicaties.push(
        new Indicatie(
          BesluitIndicatie.INGETROKKEN,
          "stop",
          this.getIntrekToelichting(),
        ),
      );
    }
  }

  private getIntrekToelichting(): string {
    return this.translate.instant(
      "besluit.vervalreden." + this.besluit.vervalreden,
    );
  }
}

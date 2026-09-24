/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { CommonModule } from "@angular/common";
import { Component, computed, input } from "@angular/core";
import { TranslateModule } from "@ngx-translate/core";
import { MaterialModule } from "../../material/material.module";
import { IndicatieItem } from "../../model/indicatie-item";
import { GeneratedType } from "../../utils/generated-types";
import { IndicatiesComponent } from "../indicaties.component";

@Component({
  selector: "zac-persoon-indicaties",
  templateUrl: "../indicaties.component.html",
  styleUrls: ["../indicaties.component.less"],
  standalone: true,
  imports: [CommonModule, MaterialModule, TranslateModule],
})
export class PersoonIndicatiesComponent extends IndicatiesComponent {
  readonly persoon = input.required<GeneratedType<"RestPersoon">>();

  protected readonly indicaties = computed(() => this.createIndicaties());

  private createIndicaties(): IndicatieItem[] {
    const persoon = this.persoon();
    if (!persoon?.indicaties?.length) {
      return [];
    }

    return persoon.indicaties.reduce((acc, indicatie) => {
      let icon = "info";
      switch (indicatie) {
        case "GEHEIMHOUDING_OP_PERSOONSGEGEVENS":
          icon = "passkey";
          break;
        case "NIET_INGEZETENE":
          icon = "person_off";
          break;
        case "IN_ONDERZOEK":
          icon = "person_search";
          break;
        case "ONDER_CURATELE":
          icon = "account_child_invert";
          break;
        case "OPSCHORTING_BIJHOUDING":
          icon = "person_alert";
          break;
        case "OVERLEDEN":
          icon = "deceased";
          break;
        case "MINISTERIELE_REGELING":
          icon = "order_approve";
          break;
        case "EMIGRATIE":
          icon = "travel";
          break;
      }

      return [...acc, new IndicatieItem(indicatie, icon).temporary()];
    }, [] as IndicatieItem[]);
  }
}

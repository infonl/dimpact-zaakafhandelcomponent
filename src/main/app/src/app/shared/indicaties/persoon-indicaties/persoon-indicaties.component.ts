/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { Component, Input, OnChanges, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { Indicatie } from "../../model/indicatie";
import { GeneratedType } from "../../utils/generated-types";
import { IndicatiesComponent } from "../indicaties.component";

@Component({
  selector: "zac-persoon-indicaties",
  templateUrl: "../indicaties.component.html",
  styleUrls: ["../indicaties.component.less"],
})
export class PersoonIndicatiesComponent
  extends IndicatiesComponent
  implements OnInit, OnChanges
{
  constructor(private translateService: TranslateService) {
    super();
  }

  @Input() persoon: GeneratedType<"RestPersoon">;

  ngOnInit() {
    this.loadIndicaties();
  }

  loadIndicaties(): void {
    this.indicaties = this.persoon.indicaties.reduce((acc, indicatie) => {
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

      return [
        ...acc,
        new Indicatie(
          indicatie,
          icon,
          this.translateService.instant(`indicatie.${indicatie}`),
        ).temporary(),
      ];
    }, [] satisfies Indicatie[]);
  }

  ngOnChanges() {
    this.loadIndicaties();
  }
}

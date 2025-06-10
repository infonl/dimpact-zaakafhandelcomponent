/*
 * SPDX-FileCopyrightText: 2021-2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input, OnChanges, SimpleChanges } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { ZaakZoekObject } from "../../../zoeken/model/zaken/zaak-zoek-object";
import { IndicatieItem } from "../../model/indicatie-item";
import { GeneratedType } from "../../utils/generated-types";
import { IndicatiesComponent } from "../indicaties.component";

@Component({
  selector: "zac-zaak-indicaties",
  templateUrl: "../indicaties.component.html",
  styleUrls: ["../indicaties.component.less"],
})
export class ZaakIndicatiesComponent
  extends IndicatiesComponent
  implements OnChanges
{
  @Input() zaakZoekObject?: ZaakZoekObject;
  @Input() zaak?: GeneratedType<"RestZaak">;

  constructor(private translateService: TranslateService) {
    super();
  }

  ngOnChanges(changes: SimpleChanges): void {
    this.zaak = changes.zaak?.currentValue;
    this.zaakZoekObject = changes.zaakZoekObject?.currentValue;
    this.loadIndicaties();
  }

  loadIndicaties(): void {
    this.indicaties = [];
    const indicaties =
      this.zaak?.indicaties ?? this.zaakZoekObject?.indicaties ?? [];
    indicaties.forEach((indicatie) => {
      switch (indicatie) {
        case "OPSCHORTING":
          this.indicaties.push(
            new IndicatieItem(
              indicatie,
              "pause",
              `${this.translateService.instant("reden")}: ${this.getRedenOpschorting()}`,
            ).temporary(),
          );
          break;
        case "HEROPEND":
          this.indicaties.push(
            new IndicatieItem(
              indicatie,
              "restart_alt",
              this.getStatusToelichting(),
            ).temporary(),
          );
          break;
        case "HOOFDZAAK":
          this.indicaties.push(
            new IndicatieItem(
              indicatie,
              "account_tree",
              this.getHoofdzaakToelichting(),
            ),
          );
          break;
        case "DEELZAAK":
          this.indicaties.push(
            new IndicatieItem(
              indicatie,
              "account_tree",
              this.getDeelZaakToelichting(),
            ).alternate(),
          );
          break;
        case "VERLENGD":
          this.indicaties.push(
            new IndicatieItem(
              indicatie,
              "update",
              `${this.translateService.instant("reden")}: ${this.getRedenVerlenging()}`,
            ),
          );
          break;
        case "ONTVANGSTBEVESTIGING_NIET_VERSTUURD":
          this.indicaties.push(new IndicatieItem(indicatie, "unsubscribe"));
          break;
      }
    });
  }

  private getRedenOpschorting() {
    return (
      this.zaakZoekObject?.redenOpschorting ?? this.zaak?.redenOpschorting ?? ""
    );
  }

  private getStatusToelichting() {
    return (
      this.zaakZoekObject?.statusToelichting ??
      this.zaak?.status?.toelichting ??
      ""
    );
  }

  private getDeelZaakToelichting(): string {
    if (!this.zaak?.gerelateerdeZaken?.length) {
      return "";
    }

    const hoofdzaakID = this.zaak.gerelateerdeZaken.find(
      ({ relatieType }) => relatieType === "HOOFDZAAK",
    )?.identificatie;

    return this.translateService.instant("msg.zaak.relatie", {
      identificatie: hoofdzaakID,
    });
  }

  private getHoofdzaakToelichting(): string {
    if (!this.zaak?.gerelateerdeZaken?.length) {
      return "";
    }

    const deelzaken = this.zaak.gerelateerdeZaken.filter(
      ({ relatieType }) => relatieType === "DEELZAAK",
    );

    const toelichting =
      deelzaken.length === 1 ? "msg.zaak.relatie" : "msg.zaak.relaties";
    const args =
      deelzaken.length === 1
        ? { identificatie: deelzaken[0].identificatie }
        : { aantal: deelzaken.length };
    return this.translateService.instant(toelichting, args);
  }

  private getRedenVerlenging() {
    return this.zaakZoekObject?.redenVerlenging ?? this.zaak?.redenVerlenging;
  }
}

/*
 * SPDX-FileCopyrightText: 2021-2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input, OnChanges, SimpleChanges } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { ZaakRelatietype } from "../../../zaken/model/zaak-relatietype";
import { ZaakZoekObject } from "../../../zoeken/model/zaken/zaak-zoek-object";
import { Indicatie } from "../../model/indicatie";
import { GeneratedType } from "../../utils/generated-types";
import { IndicatiesComponent } from "../indicaties.component";

export enum ZaakIndicatie {
  OPSCHORTING = "OPSCHORTING",
  HEROPEND = "HEROPEND",
  HOOFDZAAK = "HOOFDZAAK",
  DEELZAAK = "DEELZAAK",
  VERLENGD = "VERLENGD",
  ONTVANGSTBEVESTIGING_NIET_VERSTUURD = "ONTVANGSTBEVESTIGING_NIET_VERSTUURD",
}

@Component({
  selector: "zac-zaak-indicaties",
  templateUrl: "../indicaties.component.html",
  styleUrls: ["../indicaties.component.less"],
})
export class ZaakIndicatiesComponent
  extends IndicatiesComponent
  implements OnChanges
{
  @Input() zaakZoekObject: ZaakZoekObject;
  @Input() zaak: GeneratedType<"RestZaak">;

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
    const indicaties = this.zaak
      ? this.zaak.indicaties
      : this.zaakZoekObject.indicaties;
    indicaties.forEach((indicatie) => {
      switch (indicatie) {
        case ZaakIndicatie.OPSCHORTING:
          this.indicaties.push(
            new Indicatie(
              indicatie,
              "pause",
              `${this.translateService.instant("reden")}: ${this.getRedenOpschorting()}`,
            ).temporary(),
          );
          break;
        case ZaakIndicatie.HEROPEND:
          this.indicaties.push(
            new Indicatie(
              indicatie,
              "restart_alt",
              this.getStatusToelichting(),
            ).temporary(),
          );
          break;
        case ZaakIndicatie.HOOFDZAAK:
          this.indicaties.push(
            new Indicatie(
              indicatie,
              "account_tree",
              this.getHoofdzaakToelichting(),
            ),
          );
          break;
        case ZaakIndicatie.DEELZAAK:
          this.indicaties.push(
            new Indicatie(
              indicatie,
              "account_tree",
              this.getDeelZaakToelichting(),
            ).alternate(),
          );
          break;
        case ZaakIndicatie.VERLENGD:
          this.indicaties.push(
            new Indicatie(
              indicatie,
              "update",
              `${this.translateService.instant("reden")}: ${this.getRedenVerlenging()}`,
            ),
          );
          break;
        case ZaakIndicatie.ONTVANGSTBEVESTIGING_NIET_VERSTUURD:
          this.indicaties.push(
            new Indicatie(
              indicatie,
              "unsubscribe",
              this.translateService.instant(
                "indicatie.ONTVANGSTBEVESTIGING_NIET_VERSTUURD",
              ),
            ),
          );
          break;
      }
    });
  }

  private getRedenOpschorting(): string {
    return this.zaakZoekObject
      ? this.zaakZoekObject.redenOpschorting
      : (this.zaak.redenOpschorting ?? "");
  }

  private getStatusToelichting(): string {
    return this.zaakZoekObject
      ? this.zaakZoekObject.statusToelichting
      : (this.zaak.status?.toelichting ?? "");
  }

  private getDeelZaakToelichting(): string {
    if (this.zaak.gerelateerdeZaken?.length) {
      const hoofdzaakID = this.zaak.gerelateerdeZaken.find(
        (gerelateerdeZaak) =>
          gerelateerdeZaak.relatieType === ZaakRelatietype.HOOFDZAAK,
      )?.identificatie;
      return this.translateService.instant("msg.zaak.relatie", {
        identificatie: hoofdzaakID,
      });
    }
    return "";
  }

  private getHoofdzaakToelichting(): string {
    if (this.zaak.gerelateerdeZaken?.length) {
      const deelzaken = this.zaak.gerelateerdeZaken.filter(
        (gerelateerdeZaak) =>
          gerelateerdeZaak.relatieType === ZaakRelatietype.DEELZAAK,
      );
      const toelichting =
        deelzaken.length === 1 ? "msg.zaak.relatie" : "msg.zaak.relaties";
      const args =
        deelzaken.length === 1
          ? { identificatie: deelzaken[0].identificatie }
          : { aantal: deelzaken.length };
      return this.translateService.instant(toelichting, args);
    }
    return "";
  }

  private getRedenVerlenging() {
    return this.zaakZoekObject
      ? this.zaakZoekObject.redenVerlenging
      : this.zaak.redenVerlenging;
  }
}

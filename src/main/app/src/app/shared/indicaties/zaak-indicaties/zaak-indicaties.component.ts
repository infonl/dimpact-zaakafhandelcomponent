/*
 * SPDX-FileCopyrightText: 2021-2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { CommonModule } from "@angular/common";
import { Component, computed, inject, input } from "@angular/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { ZaakZoekObject } from "../../../zoeken/model/zaken/zaak-zoek-object";
import { MaterialModule } from "../../material/material.module";
import { IndicatieItem } from "../../model/indicatie-item";
import { GeneratedType } from "../../utils/generated-types";
import { IndicatiesComponent } from "../indicaties.component";

@Component({
  selector: "zac-zaak-indicaties",
  templateUrl: "../indicaties.component.html",
  styleUrls: ["../indicaties.component.less"],
  standalone: true,
  imports: [CommonModule, MaterialModule, TranslateModule],
})
export class ZaakIndicatiesComponent extends IndicatiesComponent {
  private readonly translateService = inject(TranslateService);

  readonly zaakZoekObject = input<ZaakZoekObject>();
  readonly zaak = input<GeneratedType<"RestZaak">>();

  protected readonly indicaties = computed(() => this.createIndicaties());

  private createIndicaties(): IndicatieItem[] {
    const indicatieItems: IndicatieItem[] = [];
    const indicaties =
      this.zaak()?.indicaties ?? this.zaakZoekObject()?.indicaties ?? [];
    indicaties.forEach((indicatie) => {
      switch (indicatie) {
        case "OPSCHORTING":
          indicatieItems.push(
            new IndicatieItem(
              indicatie,
              "pause",
              `${this.translateService.instant("reden")}: ${this.getRedenOpschorting()}`,
            ).temporary(),
          );
          break;
        case "HEROPEND":
          indicatieItems.push(
            new IndicatieItem(
              indicatie,
              "restart_alt",
              this.getStatusToelichting(),
            ).temporary(),
          );
          break;
        case "HOOFDZAAK":
          indicatieItems.push(
            new IndicatieItem(
              indicatie,
              "account_tree",
              this.getHoofdzaakToelichting(),
            ),
          );
          break;
        case "DEELZAAK":
          indicatieItems.push(
            new IndicatieItem(
              indicatie,
              "account_tree",
              this.getDeelZaakToelichting(),
            ).alternate(),
          );
          break;
        case "VERLENGD":
          indicatieItems.push(
            new IndicatieItem(
              indicatie,
              "update",
              `${this.translateService.instant("reden")}: ${this.getRedenVerlenging()}`,
            ),
          );
          break;
        case "ONTVANGSTBEVESTIGING_NIET_VERSTUURD":
          indicatieItems.push(new IndicatieItem(indicatie, "unsubscribe"));
          break;
      }
    });

    return indicatieItems;
  }

  private getRedenOpschorting() {
    return (
      this.zaakZoekObject()?.redenOpschorting ??
      this.zaak()?.redenOpschorting ??
      ""
    );
  }

  private getStatusToelichting() {
    return (
      this.zaakZoekObject()?.statusToelichting ??
      this.zaak()?.status?.toelichting ??
      ""
    );
  }

  private getDeelZaakToelichting(): string {
    const gerelateerdeZaken = this.zaak()?.gerelateerdeZaken;
    if (!gerelateerdeZaken?.length) {
      return "";
    }

    const hoofdzaakID = gerelateerdeZaken.find(
      ({ relatieType }) => relatieType === "HOOFDZAAK",
    )?.identificatie;

    return this.translateService.instant("msg.zaak.relatie", {
      identificatie: hoofdzaakID,
    });
  }

  private getHoofdzaakToelichting(): string {
    const gerelateerdeZaken = this.zaak()?.gerelateerdeZaken;
    if (!gerelateerdeZaken?.length) {
      return "";
    }

    const deelzaken = gerelateerdeZaken.filter(
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
    return (
      this.zaakZoekObject()?.redenVerlenging ?? this.zaak()?.redenVerlenging
    );
  }
}

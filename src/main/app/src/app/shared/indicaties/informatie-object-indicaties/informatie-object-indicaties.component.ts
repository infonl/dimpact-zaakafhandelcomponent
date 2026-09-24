/*
 * SPDX-FileCopyrightText: 2021-2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { CommonModule } from "@angular/common";
import { Component, computed, inject, input } from "@angular/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { DocumentZoekObject } from "../../../zoeken/model/documenten/document-zoek-object";
import { MaterialModule } from "../../material/material.module";
import { IndicatieItem } from "../../model/indicatie-item";
import { DatumPipe } from "../../pipes/datum.pipe";
import { GeneratedType } from "../../utils/generated-types";
import { IndicatiesComponent } from "../indicaties.component";

@Component({
  selector: "zac-informatie-object-indicaties",
  imports: [MaterialModule, TranslateModule, CommonModule],
  templateUrl: "../indicaties.component.html",
  styleUrls: ["../indicaties.component.less"],
})
export class InformatieObjectIndicatiesComponent extends IndicatiesComponent {
  private readonly translateService = inject(TranslateService);

  datumPipe = new DatumPipe("nl");

  readonly document = input<GeneratedType<"RestEnkelvoudigInformatieobject">>();
  readonly documentZoekObject = input<DocumentZoekObject>();

  protected readonly indicaties = computed(() => this.createIndicaties());

  private createIndicaties(): IndicatieItem[] {
    const indicatieItems: IndicatieItem[] = [];
    const documentZoekObject = this.documentZoekObject();
    const indicaties = documentZoekObject
      ? documentZoekObject.indicaties
      : this.document()?.indicaties;

    indicaties?.forEach((indicatie) => {
      switch (indicatie) {
        case "VERGRENDELD":
          indicatieItems.push(
            new IndicatieItem(
              indicatie,
              "lock",
              this.getVergrendeldToelichting(),
            ).temporary(),
          );
          break;
        case "ONDERTEKEND":
          indicatieItems.push(
            new IndicatieItem(
              indicatie,
              "fact_check",
              this.getOndertekeningToelichting(),
            ),
          );
          break;
        case "BESLUIT":
          indicatieItems.push(
            new IndicatieItem(
              indicatie,
              "gavel",
              this.translateService.instant("msg.document.besluit"),
            ),
          );
          break;
        case "GEBRUIKSRECHT":
          indicatieItems.push(
            new IndicatieItem(indicatie, "privacy_tip", "").temporary(),
          );
          break;
        case "VERZONDEN":
          indicatieItems.push(
            new IndicatieItem(
              indicatie,
              "local_post_office",
              this.getVerzondenToelichting()?.toString() ?? "",
            ),
          );
          break;
        default:
          console.warn("Indicatie " + indicatie + " is niet gedefinieerd.");
      }
    });

    return indicatieItems;
  }

  private getOndertekeningToelichting(): string {
    const documentZoekObject = this.documentZoekObject();
    if (documentZoekObject) {
      return (
        documentZoekObject.ondertekeningSoort +
        "-" +
        this.datumPipe.transform(documentZoekObject.ondertekeningDatum)
      );
    } else {
      const document = this.document();
      return (
        document?.ondertekening?.soort +
        "-" +
        this.datumPipe.transform(document?.ondertekening?.datum)
      );
    }
  }

  private getVerzondenToelichting() {
    const documentZoekObject = this.documentZoekObject();
    if (documentZoekObject) {
      return this.datumPipe.transform(documentZoekObject.verzenddatum);
    } else {
      return this.datumPipe.transform(this.document()?.verzenddatum);
    }
  }

  private getVergrendeldToelichting(): string {
    const documentZoekObject = this.documentZoekObject();
    if (documentZoekObject) {
      return this.translateService.instant("msg.document.vergrendeld", {
        gebruiker: documentZoekObject.vergrendeldDoor,
      });
    } else {
      return this.translateService.instant("msg.document.vergrendeld", {
        gebruiker: this.document()?.gelockedDoor?.naam,
      });
    }
  }
}

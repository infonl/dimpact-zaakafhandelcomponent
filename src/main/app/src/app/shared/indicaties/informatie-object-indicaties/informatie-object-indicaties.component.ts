/*
 * SPDX-FileCopyrightText: 2021-2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { CommonModule } from "@angular/common";
import { Component, Input, OnChanges, SimpleChanges } from "@angular/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { DocumentZoekObject } from "../../../zoeken/model/documenten/document-zoek-object";
import { MaterialModule } from "../../material/material.module";
import { IndicatieItem } from "../../model/indicatie-item";
import { DatumPipe } from "../../pipes/datum.pipe";
import { PipesModule } from "../../pipes/pipes.module";
import { GeneratedType } from "../../utils/generated-types";
import { IndicatiesComponent } from "../indicaties.component";

@Component({
  standalone: true,
  selector: "zac-informatie-object-indicaties",
  imports: [MaterialModule, TranslateModule, PipesModule, CommonModule],
  templateUrl: "../indicaties.component.html",
  styleUrls: ["../indicaties.component.less"],
})
export class InformatieObjectIndicatiesComponent
  extends IndicatiesComponent
  implements OnChanges
{
  datumPipe = new DatumPipe("nl");

  @Input() document: GeneratedType<"RestEnkelvoudigInformatieobject">;
  @Input() documentZoekObject: DocumentZoekObject;

  constructor(private translateService: TranslateService) {
    super();
  }

  ngOnChanges(changes: SimpleChanges): void {
    this.document = changes.document?.currentValue;
    this.documentZoekObject = changes.documentZoekObject?.currentValue;
    this.loadIndicaties();
  }

  private loadIndicaties(): void {
    this.indicaties = [];
    const indicaties = this.documentZoekObject
      ? this.documentZoekObject.indicaties
      : this.document.indicaties;

    indicaties?.forEach((indicatie) => {
      switch (indicatie) {
        case "VERGRENDELD":
          this.indicaties.push(
            new IndicatieItem(
              indicatie,
              "lock",
              this.getVergrendeldToelichting(),
            ).temporary(),
          );
          break;
        case "ONDERTEKEND":
          this.indicaties.push(
            new IndicatieItem(
              indicatie,
              "fact_check",
              this.getOndertekeningToelichting(),
            ),
          );
          break;
        case "BESLUIT":
          this.indicaties.push(
            new IndicatieItem(
              indicatie,
              "gavel",
              this.translateService.instant("msg.document.besluit"),
            ),
          );
          break;
        case "GEBRUIKSRECHT":
          this.indicaties.push(
            new IndicatieItem(indicatie, "privacy_tip", "").temporary(),
          );
          break;
        case "VERZONDEN":
          this.indicaties.push(
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
  }

  private getOndertekeningToelichting(): string {
    if (this.documentZoekObject) {
      return (
        this.documentZoekObject.ondertekeningSoort +
        "-" +
        this.datumPipe.transform(this.documentZoekObject.ondertekeningDatum)
      );
    } else {
      return (
        this.document.ondertekening.soort +
        "-" +
        this.datumPipe.transform(this.document.ondertekening.datum)
      );
    }
  }

  private getVerzondenToelichting() {
    if (this.documentZoekObject) {
      return this.datumPipe.transform(this.documentZoekObject.verzenddatum);
    } else {
      return this.datumPipe.transform(this.document.verzenddatum);
    }
  }

  private getVergrendeldToelichting(): string {
    if (this.documentZoekObject) {
      return this.translateService.instant("msg.document.vergrendeld", {
        gebruiker: this.documentZoekObject.vergrendeldDoor,
      });
    } else {
      return this.translateService.instant("msg.document.vergrendeld", {
        gebruiker: this.document.gelockedDoor.naam,
      });
    }
  }
}

/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { MeldingKleinEvenement } from "./model/melding-klein-evenement";
import { ZaakFormulierBuilder } from "./zaak-formulier-builder";

@Injectable({
  providedIn: "root",
})
export class ZaakFormulierenService {
  constructor(private translate: TranslateService) {}

  public getFormulierBuilder(zaaktype: string): ZaakFormulierBuilder {
    switch (zaaktype) {
      case "melding-klein-evenement":
        return new ZaakFormulierBuilder(
          new MeldingKleinEvenement(this.translate),
        );
      default:
        throw new Error(`Onbekend zaakformulier: ${zaaktype}`);
    }
  }
}

/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { DividerFormFieldBuilder } from "../../shared/material-form-builder/form-components/divider/divider-form-field-builder";
import { MedewerkerGroepFieldBuilder } from "../../shared/material-form-builder/form-components/medewerker-groep/medewerker-groep-field-builder";
import { GeneratedType } from "../../shared/utils/generated-types";
import { Taak } from "../../taken/model/taak";
import { TaakStatus } from "../../taken/model/taak-status.enum";
import { AbstractTaakFormulier } from "./abstract-taak-formulier";

export class TaakFormulierBuilder {
  protected readonly _formulier: AbstractTaakFormulier;

  constructor(formulier: AbstractTaakFormulier) {
    this._formulier = formulier;
  }

  startForm(
    planItem: GeneratedType<"RESTPlanItem">,
    zaak: GeneratedType<"RestZaak">,
  ): TaakFormulierBuilder {
    this._formulier.tabellen = planItem.tabellen;
    this._formulier.zaak = zaak;
    this._formulier.taakNaam = planItem.naam;
    this._formulier.humanTaskData = {
      planItemInstanceId: planItem.id,
      fataledatum: planItem.fataleDatum,
    };

    this._formulier.initStartForm();
    const groep: GeneratedType<"RestGroup"> | undefined = planItem
      ? { id: planItem.groepId!, naam: planItem.naam! }
      : undefined;

    this._formulier.form.push(
      [new DividerFormFieldBuilder().build()],
      [
        new MedewerkerGroepFieldBuilder(groep)
          .id(AbstractTaakFormulier.TAAK_TOEKENNING)
          .label("actie.taak.toewijzing")
          .groepLabel("actie.taak.toekennen.groep")
          .groepRequired()
          .setZaaktypeUuid(zaak.zaaktype.uuid)
          .medewerkerLabel("actie.taak.toekennen.medewerker")
          .build(),
      ],
    );
    return this;
  }

  behandelForm(
    taak: Taak,
    zaak: GeneratedType<"RestZaak">,
  ): TaakFormulierBuilder {
    this._formulier.zaak = zaak;
    this._formulier.taak = taak;
    this._formulier.tabellen = taak.tabellen;
    this._formulier.dataElementen = taak.taakdata;
    this._formulier.initBehandelForm(
      taak.status === TaakStatus.Afgerond || !taak.rechten.wijzigen,
    );
    return this;
  }

  build(): AbstractTaakFormulier {
    return this._formulier;
  }
}

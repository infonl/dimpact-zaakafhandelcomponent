/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgClass } from "@angular/common";
import { Component, effect, inject, input, untracked } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { MatSlideToggleModule } from "@angular/material/slide-toggle";
import { TranslateModule } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { UtilService } from "../../../../core/service/util.service";
import { IdentityService } from "../../../../identity/identity.service";
import { I18nKeyPipe } from "../../../../shared/pipes/i18n-key.pipe";
import { ReferentieTabelService } from "../../../referentie-tabel.service";
import { ZaakafhandelParametersService } from "../../../zaakafhandel-parameters.service";
import {
  HumanTaskForm,
  HumanTasksForm,
  syncReferentieTabellen,
} from "./human-tasks-form";

@Component({
  selector: "zac-parameters-step-humantasks",
  templateUrl: "./parameters-step-humantasks.component.html",
  styleUrl: "./parameters-step-humantasks.component.less",
  standalone: true,
  imports: [
    NgClass,
    ReactiveFormsModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    TranslateModule,
    I18nKeyPipe,
  ],
})
export class ParametersStepHumantasksComponent {
  readonly form = input.required<HumanTasksForm>();
  readonly zaaktypeOmschrijving = input.required<string>();

  private readonly zaakafhandelParametersService = inject(
    ZaakafhandelParametersService,
  );
  private readonly referentieTabelService = inject(ReferentieTabelService);
  private readonly identityService = inject(IdentityService);
  private readonly utilService = inject(UtilService);

  protected readonly formulierDefinitiesQuery = injectQuery(() =>
    this.zaakafhandelParametersService.listFormulierDefinities(),
  );
  protected readonly referentieTabellenQuery = injectQuery(() =>
    this.referentieTabelService.listReferentieTabellenQuery(),
  );
  protected readonly groepenQuery = injectQuery(() => {
    const omschrijving = this.zaaktypeOmschrijving();
    return {
      ...this.identityService.listBehandelaarGroupsForZaaktypeQuery(
        omschrijving,
      ),
      enabled: Boolean(omschrijving),
    };
  });

  protected readonly compareObject = (a: unknown, b: unknown) =>
    this.utilService.compare(a, b);

  constructor() {
    effect(() => {
      if (!this.formulierDefinitiesQuery.data()) return;
      if (!this.referentieTabellenQuery.data()) return;

      for (const humanTaskForm of untracked(this.form).controls) {
        this.syncReferentieTabellen(humanTaskForm);
      }
    });
  }

  protected veldDefinities(humanTaskForm: HumanTaskForm) {
    return (
      this.formulierDefinitiesQuery
        .data()
        ?.find(
          ({ id }) => id === humanTaskForm.controls.formulierDefinitieId.value,
        )?.veldDefinities ?? []
    );
  }

  protected syncReferentieTabellen(humanTaskForm: HumanTaskForm) {
    syncReferentieTabellen(humanTaskForm, {
      formulierDefinities: this.formulierDefinitiesQuery.data() ?? [],
      referentieTabellen: this.referentieTabellenQuery.data() ?? [],
    });
  }
}

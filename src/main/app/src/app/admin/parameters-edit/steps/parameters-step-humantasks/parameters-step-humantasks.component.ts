/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgClass } from "@angular/common";
import {
  Component,
  computed,
  effect,
  inject,
  input,
  untracked,
} from "@angular/core";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatIconModule } from "@angular/material/icon";
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { UtilService } from "../../../../core/service/util.service";
import { IdentityService } from "../../../../identity/identity.service";
import { ZacInput } from "../../../../shared/form/input/input";
import { ZacSelect } from "../../../../shared/form/select/select";
import { ZacToggle } from "../../../../shared/form/toggle/toggle";
import { I18nKeyPipe } from "../../../../shared/pipes/i18n-key.pipe";
import { toI18nKey } from "../../../../shared/utils/i18n-key";
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
    MatExpansionModule,
    MatIconModule,
    TranslatePipe,
    I18nKeyPipe,
    ZacInput,
    ZacSelect,
    ZacToggle,
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
      ...this.identityService.listBehandelaarGroupsForZaaktype(omschrijving),
      enabled: Boolean(omschrijving),
    };
  });

  protected readonly formulierDefinitieIds = computed(() =>
    (this.formulierDefinitiesQuery.data() ?? []).flatMap(({ id }) =>
      id ? [id] : [],
    ),
  );
  protected readonly groepIds = computed(() =>
    (this.groepenQuery.data() ?? []).map(({ id }) => id),
  );

  protected readonly formulierDefinitieLabel = (id: string | null) =>
    toI18nKey(`formulier-definitie.${id}`);
  protected readonly groepNaam = (id: string | null) =>
    this.groepenQuery.data()?.find((groep) => groep.id === id)?.naam ?? "";
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

    effect((onCleanup) => {
      const subscriptions = this.form().controls.map((humanTaskForm) =>
        humanTaskForm.controls.formulierDefinitieId.valueChanges.subscribe(() =>
          this.syncReferentieTabellen(humanTaskForm),
        ),
      );
      onCleanup(() =>
        subscriptions.forEach((subscription) => subscription.unsubscribe()),
      );
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

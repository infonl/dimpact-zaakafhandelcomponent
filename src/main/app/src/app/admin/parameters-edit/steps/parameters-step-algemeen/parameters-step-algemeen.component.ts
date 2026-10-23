/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  computed,
  effect,
  inject,
  input,
  untracked,
} from "@angular/core";
import { toObservable, toSignal } from "@angular/core/rxjs-interop";
import { ReactiveFormsModule } from "@angular/forms";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { startWith, switchMap } from "rxjs";
import { IdentityService } from "../../../../identity/identity.service";
import { ZacInput } from "../../../../shared/form/input/input";
import { ZacSelect } from "../../../../shared/form/select/select";
import { StaticTextComponent } from "../../../../shared/static-text/static-text.component";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { AlgemeenForm } from "./algemeen-form";

type Zaaktype = Pick<
  GeneratedType<"RestZaaktype">,
  "uuid" | "identificatie" | "doel" | "omschrijving" | "hasServicenorm"
>;

@Component({
  selector: "zac-parameters-step-algemeen",
  templateUrl: "./parameters-step-algemeen.component.html",
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatIconModule,
    TranslatePipe,
    StaticTextComponent,
    ZacInput,
    ZacSelect,
  ],
})
export class ParametersStepAlgemeenComponent {
  readonly form = input.required<AlgemeenForm>();
  readonly zaaktype = input.required<Zaaktype>();
  readonly isZaakspecifiekAutoriseerbaar = input(false);

  private readonly identityService = inject(IdentityService);

  private readonly groepenQuery = injectQuery(() => {
    const omschrijving = this.zaaktype().omschrijving ?? "";
    return {
      ...this.identityService.listBehandelaarGroupsForZaaktypeQuery(
        omschrijving,
      ),
      enabled: Boolean(omschrijving),
    };
  });

  private readonly defaultGroepId = toSignal(
    toObservable(this.form).pipe(
      switchMap(({ controls: { defaultGroepId } }) =>
        defaultGroepId.valueChanges.pipe(startWith(defaultGroepId.value)),
      ),
    ),
  );

  private readonly medewerkersQuery = injectQuery(() => {
    const groepId = this.defaultGroepId() ?? "";
    return {
      ...this.identityService.listUsersInGroupQuery(groepId),
      enabled: Boolean(groepId),
    };
  });

  protected readonly groepIds = computed(() =>
    (this.groepenQuery.data() ?? []).map(({ id }) => id),
  );
  protected readonly medewerkerIds = computed(() =>
    (this.medewerkersQuery.data() ?? []).map(({ id }) => id),
  );

  protected readonly groepNaam = (id: string | null) =>
    this.groepenQuery.data()?.find((groep) => groep.id === id)?.naam ?? "";
  protected readonly medewerkerNaam = (id: string | null) =>
    this.medewerkersQuery.data()?.find((medewerker) => medewerker.id === id)
      ?.naam ?? "";

  constructor() {
    effect(() => {
      const groepen = this.groepenQuery.data();
      if (!groepen) return;

      const { defaultGroepId } = untracked(this.form).controls;
      const isOffered = groepen.some(({ id }) => id === defaultGroepId.value);
      if (!isOffered) defaultGroepId.setValue(null);
    });

    effect(() => {
      const medewerkers = this.medewerkersQuery.data();
      if (!medewerkers) return;

      const { defaultBehandelaarId } = untracked(this.form).controls;
      const isInGroep = medewerkers.some(
        ({ id }) => id === defaultBehandelaarId.value,
      );
      if (!isInGroep) defaultBehandelaarId.setValue(null);
    });
  }
}

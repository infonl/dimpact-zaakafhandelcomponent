/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject, input } from "@angular/core";
import { takeUntilDestroyed, toSignal } from "@angular/core/rxjs-interop";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatDividerModule } from "@angular/material/divider";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatHint } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatDrawer, MatSidenavModule } from "@angular/material/sidenav";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { ZacFormActions } from "../../shared/form/form-actions/form-actions.component";
import { ZacSelect } from "../../shared/form/select/select";
import { injectMutation } from "../../shared/http/inject-mutation";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-zaakspecifiek-geautoriseerde-medewerker-toevoegen",
  templateUrl:
    "./zaakspecifiek-geautoriseerde-medewerker-toevoegen.component.html",
  standalone: true,
  imports: [
    MatButtonModule,
    MatDividerModule,
    MatExpansionModule,
    MatHint,
    MatIconModule,
    MatSidenavModule,
    MatToolbarModule,
    ReactiveFormsModule,
    TranslatePipe,
    ZacFormActions,
    ZacSelect,
  ],
})
export class ZaakspecifiekGeautoriseerdeMedewerkerToevoegenComponent {
  private readonly zakenService = inject(ZakenService);
  private readonly identityService = inject(IdentityService);
  private readonly utilService = inject(UtilService);
  private readonly formBuilder = inject(FormBuilder);

  readonly zaak = input.required<GeneratedType<"RestZaak">>();
  readonly sideNav = input.required<MatDrawer>();

  protected readonly form = this.formBuilder.group({
    groep: this.formBuilder.control<GeneratedType<"RestGroup"> | null>(null, [
      Validators.required,
    ]),
    medewerker: this.formBuilder.control<GeneratedType<"RestUser"> | null>(
      { value: null, disabled: true },
      [Validators.required],
    ),
  });

  private readonly groep = toSignal(this.form.controls.groep.valueChanges, {
    initialValue: null,
  });

  protected readonly groupsQuery = injectQuery(() =>
    this.identityService.listBehandelaarGroupsForZaaktypeQuery(
      this.zaak().zaaktype.omschrijving ?? "",
    ),
  );

  protected readonly kandidatenQuery = injectQuery(() => {
    const groep = this.groep();
    return {
      ...this.zakenService.listZaakspecifiekGeautoriseerdeMedewerkerKandidatenQuery(
        this.zaak().uuid,
        groep?.id ?? "",
      ),
      enabled: Boolean(groep),
    };
  });

  protected readonly mutation = injectMutation(
    () =>
      this.zakenService.addZaakspecifiekGeautoriseerdeMedewerker(
        this.zaak().uuid,
      ),
    {
      onSuccess: () => {
        this.utilService.openSnackbar(
          "msg.zaakspecifiek-geautoriseerde-medewerker.toegevoegd",
          { medewerker: this.form.controls.medewerker.value?.naam },
        );
        void this.sideNav().close();
      },
    },
  );

  constructor() {
    this.form.controls.groep.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((groep) => {
        this.form.controls.medewerker.reset();
        if (groep) this.form.controls.medewerker.enable();
        else this.form.controls.medewerker.disable();
      });
  }

  protected toevoegen() {
    const { groep, medewerker } = this.form.getRawValue();
    if (!groep || !medewerker) return;
    this.mutation.mutate({ groepId: groep.id, medewerkerId: medewerker.id });
  }
}

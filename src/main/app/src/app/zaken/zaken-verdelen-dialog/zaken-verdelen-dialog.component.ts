/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import { Component, effect, inject, Inject, OnDestroy, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { Subject, takeUntil } from "rxjs";
import { IdentityService } from "../../identity/identity.service";
import { ZacAutoComplete } from "../../shared/form/auto-complete/auto-complete";
import { FormHelper } from "../../shared/form/helpers";
import { ZacInput } from "../../shared/form/input/input";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "zaken-verdelen-dialog.component.html",
  styleUrls: ["./zaken-verdelen-dialog.component.less"],
  standalone: true,
  imports: [
    NgIf,
    ReactiveFormsModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    TranslateModule,
    ZacAutoComplete,
    ZacInput,
  ],
})
export class ZakenVerdelenDialogComponent implements OnDestroy {
  private readonly destroy$ = new Subject<void>();

  protected readonly loading = signal(false);

  protected readonly form = this.formBuilder.group({
    groep: this.formBuilder.control<GeneratedType<"RestGroup"> | null>(null, [
      Validators.required,
    ]),
    medewerker: this.formBuilder.control<GeneratedType<"RestUser"> | null>(
      null,
    ),
    reden: this.formBuilder.control<string | null>(null, [
      Validators.maxLength(100),
    ]),
  });

  private readonly identityService = inject(IdentityService);

  protected readonly groups = injectQuery(() =>
    this.identityService.listBehandelaarGroupsForZaaktypes(
      this.data.map(({ zaaktypeOmschrijving }) => zaaktypeOmschrijving),
    ),
  );

  private readonly noAuthorisedGroupValidator = () =>
    FormHelper.CustomErrorMessage("msg.error.group.no.authorised.group.for.zaken");

  protected users: GeneratedType<"RestUser">[] = [];

  constructor(
    public readonly dialogRef: MatDialogRef<ZakenVerdelenDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public readonly data: ZaakZoekObject[],
    private readonly zakenService: ZakenService,
    private readonly formBuilder: FormBuilder,
  ) {
    this.form.controls.medewerker.disable();

    effect(() => {
      const groups = this.groups.data();
      if (groups === undefined) return;

      const groepControl = this.form.controls.groep;
      if (groups.length === 0) {
        groepControl.setValidators(this.noAuthorisedGroupValidator);
        groepControl.markAsTouched();
      } else {
        groepControl.setValidators(Validators.required);
      }
      groepControl.updateValueAndValidity();
    });

    this.form.controls.groep.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((group) => {
        this.form.controls.medewerker.setValue(null);
        this.form.controls.medewerker.disable();
        if (!group) return;

        this.identityService.listUsersInGroup(group.id).subscribe((users) => {
          this.form.controls.medewerker.enable();
          this.users = users;
        });
      });
  }

  protected close() {
    this.dialogRef.close(false);
  }

  protected isDisabled() {
    return this.form.invalid || this.loading() || !this.data.length;
  }

  protected verdeel() {
    this.dialogRef.disableClose = true;
    this.loading.set(true);
    this.zakenService
      .verdelenVanuitLijst({
        uuids: this.data.map(({ id }) => id),
        screenEventResourceId: crypto.randomUUID(),
        groepId: this.form.value.groep!.id,
        behandelaarGebruikersnaam: this.form.value.medewerker?.id,
        reden: this.form.value.reden,
      })
      .subscribe(() => {
        this.dialogRef.close(this.form.value);
      });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}

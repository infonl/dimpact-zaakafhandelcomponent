/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject } from "@angular/core";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TaakZoekObject } from "../../zoeken/model/taken/taak-zoek-object";
import { TakenService } from "../taken.service";
import {FormBuilder, Validators} from "@angular/forms";
import {IdentityService} from "../../identity/identity.service";

@Component({
  selector: "zac-taken-verdelen-dialog",
  templateUrl: "./taken-verdelen-dialog.component.html",
  styleUrls: ["./taken-verdelen-dialog.component.less"],
})
export class TakenVerdelenDialogComponent {
  loading = false;

  protected readonly form = this.formBuilder.group({
    groep: this.formBuilder.control<GeneratedType<"RestGroup"> | null>(null, [Validators.required]),
    medewerker: this.formBuilder.control<GeneratedType<"RestUser"> | null>(
        null,
    ),
    reden: this.formBuilder.control<string | null>(null, [
      Validators.maxLength(100),
    ]),
  });

  protected groups = this.identityService.listGroups();
  protected users: GeneratedType<"RestUser">[] = [];

  constructor(
    public dialogRef: MatDialogRef<TakenVerdelenDialogComponent>,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      taken: TaakZoekObject[];
      screenEventResourceId: string;
    },
    private takenService: TakenService,
    private readonly formBuilder: FormBuilder,
    private readonly identityService: IdentityService,
  ) {
    this.form.controls.medewerker.disable();

    this.form.controls.groep.valueChanges.subscribe((group) => {
      this.form.controls.medewerker.setValue(null);
      this.form.controls.medewerker.disable();
      if (!group) return;

      this.identityService.listUsersInGroup(group.id).subscribe((users) => {
        this.form.controls.medewerker.enable();
        this.users = users;
      });
    });
  }

  close(): void {
    this.dialogRef.close(false);
  }

  isDisabled() {
    const { groep, medewerker } = this.form.getRawValue();
    return (
        (!groep && !medewerker) ||
        this.form.invalid ||
        this.loading ||
        !this.data.taken.length
    );
  }

  verdeel(): void {
    this.dialogRef.disableClose = true;
    this.loading = true;
    this.takenService
      .verdelenVanuitLijst({
            taken: this.data.taken.map(({ id, zaakUuid }) => ({
              taakId: id,
              zaakUuid
            })),
        behandelaarGebruikersnaam: this.form.value.medewerker?.id,
        reden: this.form.value.reden ?? "",
        groepId: this.form.value.groep!.id,
        screenEventResourceId: this.data.screenEventResourceId,
       }
      )
      .subscribe(() => {
        this.dialogRef.close(this.form.value);
      });
  }
}

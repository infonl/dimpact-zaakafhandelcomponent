/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatIconModule } from "@angular/material/icon";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";
import { injectMutation } from "@tanstack/angular-query-experimental";
import moment, { Moment } from "moment";
import { lastValueFrom } from "rxjs";
import { FoutAfhandelingService } from "src/app/fout-afhandeling/fout-afhandeling.service";
import { UtilService } from "../../../core/service/util.service";
import { ZacDate } from "../../../shared/form/date/date";
import { ZacFormActions } from "../../../shared/form/form-actions/form-actions.component";
import { ZacInput } from "../../../shared/form/input/input";
import { ZacSelect } from "../../../shared/form/select/select";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { ZakenService } from "../../zaken.service";

type VervalRedenOption = {
  label: string;
  value: GeneratedType<"VervalredenEnum">;
};

@Component({
  selector: "zac-besluit-intrekken-dialog",
  templateUrl: "./besluit-intrekken-dialog.component.html",
  styleUrls: ["./besluit-intrekken-dialog.component.less"],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatToolbarModule,
    MatIconModule,
    MatDialogModule,
    MatDividerModule,
    TranslateModule,
    ZacDate,
    ZacSelect,
    ZacInput,
    ZacFormActions,
  ],
})
export class BesluitIntrekkenDialogComponent {
  private readonly dialogRef = inject(
    MatDialogRef<BesluitIntrekkenDialogComponent>,
  );
  private readonly zakenService = inject(ZakenService);
  private readonly utilService = inject(UtilService);
  private readonly foutAfhandelingService = inject(FoutAfhandelingService);
  private readonly formBuilder = inject(FormBuilder);
  protected readonly besluit =
    inject<GeneratedType<"RestBesluit">>(MAT_DIALOG_DATA);

  protected readonly vervalRedenen: VervalRedenOption[] = [
    {
      label: "besluit.vervalreden.ingetrokken_overheid",
      value: "INGETROKKEN_OVERHEID",
    },
    {
      label: "besluit.vervalreden.ingetrokken_belanghebbende",
      value: "INGETROKKEN_BELANGHEBBENDE",
    },
  ];

  protected readonly documentenVerstuurd = Boolean(
    this.besluit.informatieobjecten?.some(
      ({ verzenddatum }) => verzenddatum != null,
    ),
  );

  protected readonly form = this.formBuilder.group({
    vervaldatum: this.formBuilder.control<Moment | null>(
      null,
      Validators.required,
    ),
    vervalreden: this.formBuilder.control<VervalRedenOption | null>(
      null,
      Validators.required,
    ),
    toelichting: this.formBuilder.control<string | null>(
      null,
      Validators.required,
    ),
  });

  protected readonly mutation = injectMutation(() => ({
    mutationFn: (data: GeneratedType<"RestBesluitWithdrawalData">) =>
      lastValueFrom(this.zakenService.intrekkenBesluit(data)),
    onMutate: () => {
      this.dialogRef.disableClose = true;
    },
    onSuccess: () => {
      this.utilService.openSnackbar("msg.besluit.ingetrokken");
      this.dialogRef.close(true);
    },
    onError: (error) => this.foutAfhandelingService.foutAfhandelen(error),
    onSettled: () => {
      this.dialogRef.disableClose = false;
    },
  }));

  constructor() {
    if (this.besluit.ingangsdatum) {
      this.form.controls.vervaldatum.addValidators(
        Validators.min(
          moment(this.besluit.ingangsdatum).startOf("day").valueOf(),
        ),
      );
      this.form.controls.vervaldatum.updateValueAndValidity({
        emitEvent: false,
      });
    }
  }

  protected close() {
    this.dialogRef.close(false);
  }

  protected intrekken() {
    const { vervaldatum, vervalreden, toelichting } = this.form.getRawValue();
    this.mutation.mutate({
      besluitUuid: this.besluit.uuid,
      vervaldatum: vervaldatum?.toISOString(),
      vervalreden: vervalreden?.value ?? "",
      reden: toelichting ?? "",
    });
  }
}

/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import { Component, inject } from "@angular/core";
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatCheckboxModule } from "@angular/material/checkbox";
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import moment, { Moment } from "moment";
import { FormHelper } from "../../shared/form/helpers";
import { injectMutation } from "../../shared/http/inject-mutation";
import { ZacQueryClient } from "../../shared/http/zac-query-client";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { GeneratedType } from "../../shared/utils/generated-types";

@Component({
  templateUrl: "zaak-brondatum-zetten-dialog.component.html",
  styleUrls: ["./zaak-brondatum-zetten-dialog.component.less"],
  standalone: true,
  imports: [
    NgIf,
    ReactiveFormsModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    MatDialogModule,
    MatCheckboxModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    TranslateModule,
    MaterialFormBuilderModule,
  ],
})
export class ZaakBrondatumZettenDialogComponent {
  private readonly dialogRef = inject(
    MatDialogRef<ZaakBrondatumZettenDialogComponent>,
  );
  public readonly data = inject(MAT_DIALOG_DATA) as {
    zaak: GeneratedType<"RestZaak">;
    planItem?: GeneratedType<"RESTPlanItem">;
  };
  private readonly formBuilder = inject(FormBuilder);
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly translateService = inject(TranslateService);

  protected brondatumLabel?: string | null;

  form = this.formBuilder.group({
    brondatum: this.formBuilder.control<Moment | null>(null, [
      this.brondatumNietVoorVandaag(),
      Validators.required,
      Validators.min(moment().startOf("day").valueOf()),
    ]),
  });

  protected readonly brondatumZettenMutation = injectMutation(
    () =>
      this.zacQueryClient.PUT("/rest/zaken/zaak/{uuid}/brondatum", {
        path: { uuid: this.data.zaak.uuid },
      }),
    {
      onSuccess: () => this.dialogRef.close(true),
      onError: () => this.dialogRef.close(false),
    },
  );

  protected readonly planItemAfhandelenMutation = injectMutation(
    () =>
      this.zacQueryClient.POST("/rest/planitems/doUserEventListenerPlanItem"),
    {
      onSuccess: () => this.dialogRef.close(true),
      onError: () => this.dialogRef.close(false),
    },
  );

  constructor() {
    if (this.data.zaak.resultaat?.resultaattype?.datumKenmerkVerplicht) {
      this.brondatumLabel =
        this.data.zaak.resultaat?.resultaattype.datumKenmerkOmschrijving;
    }
  }

  private brondatumNietVoorVandaag(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const value = control.value;
      if (!moment.isMoment(value) || value.isSameOrAfter(moment(), "day")) {
        return null;
      }
      return FormHelper.CustomErrorMessage(
        "msg.error.date.invalid.datum.brondatum-voor-vandaag",
        {
          label:
            this.brondatumLabel ||
            this.translateService.instant("zaak.brondatum"),
        },
      );
    };
  }

  protected close() {
    this.dialogRef.close();
  }

  protected afhandelen() {
    this.dialogRef.disableClose = true;
    if (!this.data.planItem) {
      this.afsluiten();
      return;
    }

    this.planItemAfhandelen(this.data.planItem);
  }

  private afsluiten() {
    const { value } = this.form;
    if (value.brondatum) {
      this.brondatumZettenMutation.mutate({
        brondatum: value.brondatum.toISOString(),
      });
    }
  }

  private planItemAfhandelen(planItem: GeneratedType<"RESTPlanItem">) {
    const { value } = this.form;

    this.planItemAfhandelenMutation.mutate({
      actie: "BRONDATUM_ZETTEN",
      planItemInstanceId: planItem.id,
      zaakUuid: this.data.zaak.uuid,
      brondatum: value.brondatum?.toISOString(),
    });
  }
}

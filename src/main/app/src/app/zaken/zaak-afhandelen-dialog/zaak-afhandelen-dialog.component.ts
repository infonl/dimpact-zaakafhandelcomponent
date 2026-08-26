/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgFor, NgIf } from "@angular/common";
import { Component, effect, inject } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
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
import { injectQuery } from "@tanstack/angular-query-experimental";
import moment, { Moment } from "moment";
import { firstValueFrom } from "rxjs";
import { injectContactEmail } from "../../klanten/inject-contact-email";
import { MailtemplateService } from "../../mailtemplate/mailtemplate.service";
import { FormHelper } from "../../shared/form/helpers";
import { injectMutation } from "../../shared/http/inject-mutation";
import { ZacQueryClient } from "../../shared/http/zac-query-client";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { CustomValidators } from "../../shared/validators/customValidators";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "zaak-afhandelen-dialog.component.html",
  styleUrls: ["./zaak-afhandelen-dialog.component.less"],
  standalone: true,
  imports: [
    NgIf,
    NgFor,
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
    StaticTextComponent,
    MaterialFormBuilderModule,
  ],
})
export class ZaakAfhandelenDialogComponent {
  private readonly dialogRef = inject(
    MatDialogRef<ZaakAfhandelenDialogComponent>,
  );
  public readonly data = inject(MAT_DIALOG_DATA) as {
    zaak: GeneratedType<"RestZaak">;
    planItem?: GeneratedType<"RESTPlanItem">;
  };
  private readonly formBuilder = inject(FormBuilder);
  private readonly zakenService = inject(ZakenService);
  private readonly mailtemplateService = inject(MailtemplateService);
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly translateService = inject(TranslateService);

  private sendMailDefault: boolean;
  protected readonly contactEmailAddress = injectContactEmail(
    () => this.data.zaak,
  );

  protected brondatumLabel?: string | null;

  form = this.formBuilder.group({
    resultaattype:
      this.formBuilder.control<GeneratedType<"RestResultaattype"> | null>(null),
    toelichting: this.formBuilder.control<string>(""),
    sendMail: this.formBuilder.control<boolean>(false),
    verzender:
      this.formBuilder.control<GeneratedType<"RestZaakAfzender"> | null>(null),
    ontvanger: this.formBuilder.control<string>("", [CustomValidators.email]),
    brondatum: this.formBuilder.control<Moment | null>(null, [
      this.brondatumNietVoorVandaag(),
      Validators.min(moment().startOf("day").valueOf()),
    ]),
  });

  protected readonly resultaattypesQuery = injectQuery(() => ({
    queryKey: ["resultaattypes", this.data.zaak.zaaktype.uuid],
    queryFn: () =>
      firstValueFrom(
        this.zakenService.listResultaattypes(this.data.zaak.zaaktype.uuid),
      ),
  }));

  protected readonly afzendersQuery = injectQuery(() => ({
    queryKey: ["afzenders", this.data.zaak.uuid],
    queryFn: () =>
      firstValueFrom(
        this.zakenService.listAfzendersVoorZaak(this.data.zaak.uuid),
      ),
  }));

  protected readonly mailtemplateQuery = injectQuery(() => ({
    queryKey: ["mailtemplate", this.data.zaak.uuid],
    queryFn: () =>
      firstValueFrom(
        this.mailtemplateService.findMailtemplate(
          "ZAAK_AFGEHANDELD",
          this.data.zaak.uuid,
        ),
      ),
  }));

  protected readonly afsluitenMutation = injectMutation(
    () => this.zakenService.afsluitenMutation(this.data.zaak.uuid),
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
    effect(() => {
      const afzenders = this.afzendersQuery.data();
      this.form.controls.verzender.setValue(
        afzenders?.find((afzender) => afzender.defaultMail) ?? null,
      );
    });

    const zaakafhandelparameters =
      this.data.zaak.zaaktype.zaakafhandelparameters;
    this.sendMailDefault =
      zaakafhandelparameters?.afrondenMail === "BESCHIKBAAR_AAN";

    if (!this.data.zaak.resultaat) {
      this.form.controls.resultaattype.addValidators(Validators.required);
    }
    if (this.sendMailDefault && this.data.planItem) {
      this.form.controls.sendMail.setValue(true);
      this.form.controls.verzender.addValidators(Validators.required);
      this.form.controls.ontvanger.addValidators([
        Validators.required,
        CustomValidators.email,
      ]);
    }

    this.form.controls.sendMail.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        if (value) {
          this.form.controls.verzender.setValidators([Validators.required]);
          this.form.controls.ontvanger.setValidators([
            Validators.required,
            CustomValidators.email,
          ]);
        } else {
          this.form.controls.verzender.clearValidators();
          this.form.controls.ontvanger.clearValidators();
        }
        this.form.controls.verzender.updateValueAndValidity();
        this.form.controls.ontvanger.updateValueAndValidity();
      });

    this.form.controls.resultaattype.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        if (value?.besluitVerplicht && !this.data.zaak.besluiten?.length) {
          this.form.controls.toelichting.disable();
          this.form.controls.sendMail.disable();
          this.form.controls.verzender.disable();
          this.form.controls.ontvanger.disable();
        } else {
          this.form.controls.toelichting.enable();
          this.form.controls.sendMail.enable();
          this.form.controls.verzender.enable();
          this.form.controls.ontvanger.enable();
        }

        if (value?.datumKenmerkVerplicht) {
          this.brondatumLabel = value?.datumKenmerkOmschrijving;
        }
        this.form.controls.brondatum.updateValueAndValidity();
      });
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
    this.afsluitenMutation.mutate({
      reden: value.toelichting,
      resultaattypeUuid: value.resultaattype!.id,
      brondatum: value.brondatum?.toISOString(),
    });
  }

  private planItemAfhandelen(planItem: GeneratedType<"RESTPlanItem">) {
    const { value } = this.form;
    const mailtemplate = this.mailtemplateQuery.data();

    const restMailGegevens =
      value.sendMail && mailtemplate
        ? ({
            verzender: value.verzender?.mail,
            replyTo: value.verzender?.replyTo,
            ontvanger: value.ontvanger,
            onderwerp: mailtemplate.onderwerp,
            body: mailtemplate.body,
            createDocumentFromMail: true,
          } satisfies GeneratedType<"RESTMailGegevens">)
        : undefined;

    this.planItemAfhandelenMutation.mutate({
      actie: "ZAAK_AFHANDELEN",
      planItemInstanceId: planItem.id,
      zaakUuid: this.data.zaak.uuid,
      resultaattypeUuid:
        this.data.zaak.resultaat?.resultaattype?.id ?? value.resultaattype?.id,
      resultaatToelichting: value.toelichting,
      restMailGegevens,
      brondatum: value.brondatum?.toISOString(),
    });
  }

  protected setOntvanger() {
    this.form.controls.ontvanger.setValue(this.contactEmailAddress());
  }

  protected openBesluitVastleggen() {
    this.dialogRef.close("openBesluitVastleggen");
  }
}

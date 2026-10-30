/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AsyncPipe } from "@angular/common";
import { Component, OnDestroy, inject, signal } from "@angular/core";
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
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
import { MatInputModule } from "@angular/material/input";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatRadioModule } from "@angular/material/radio";
import { MatSelectModule } from "@angular/material/select";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslatePipe, TranslateService } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { Observable, Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
import { injectContactEmail } from "../../klanten/inject-contact-email";
import { MailtemplateService } from "../../mailtemplate/mailtemplate.service";
import { PlanItemsService } from "../../plan-items/plan-items.service";
import { trackControl } from "../../shared/form/control-state";
import { injectMutation } from "../../shared/http/inject-mutation";
import { I18nKeyPipe } from "../../shared/pipes/i18n-key.pipe";
import { GeneratedType } from "../../shared/utils/generated-types";
import { CustomValidators } from "../../shared/validators/customValidators";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "intake-afronden-dialog.component.html",
  styleUrls: ["./intake-afronden-dialog.component.less"],
  standalone: true,
  imports: [
    I18nKeyPipe,
    AsyncPipe,
    ReactiveFormsModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    MatDialogModule,
    MatRadioModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatExpansionModule,
    MatProgressSpinnerModule,
    TranslatePipe,
  ],
})
export class IntakeAfrondenDialogComponent implements OnDestroy {
  readonly dialogRef =
    inject<MatDialogRef<IntakeAfrondenDialogComponent>>(MatDialogRef);
  data = inject<{
    zaak: GeneratedType<"RestZaak">;
    planItem: GeneratedType<"RestPlanItem">;
  }>(MAT_DIALOG_DATA);
  private readonly formBuilder = inject(FormBuilder);
  private readonly translateService = inject(TranslateService);
  private readonly planItemsService = inject(PlanItemsService);
  private readonly mailtemplateService = inject(MailtemplateService);
  private readonly zakenService = inject(ZakenService);
  private readonly utilService = inject(UtilService);

  protected readonly zaakOntvankelijkMailQuery = injectQuery(() =>
    this.mailtemplateService.findMailtemplate(
      "ZAAK_ONTVANKELIJK",
      this.data.zaak.uuid,
    ),
  );
  protected readonly zaakNietOntvankelijkMailQuery = injectQuery(() =>
    this.mailtemplateService.findMailtemplate(
      "ZAAK_NIET_ONTVANKELIJK",
      this.data.zaak.uuid,
    ),
  );
  private readonly zaakafhandelparameters =
    this.data.zaak.zaaktype.zaakafhandelparameters;
  readonly mailBeschikbaar =
    this.zaakafhandelparameters?.intakeMail !== "NIET_BESCHIKBAAR";
  readonly sendMailDefault =
    this.zaakafhandelparameters?.intakeMail === "BESCHIKBAAR_AAN";
  protected readonly contactEmailAddress = injectContactEmail(
    () => this.data.zaak,
  );
  readonly formGroup: FormGroup = this.formBuilder.group({
    ontvankelijk: [null, [Validators.required]],
    reden: "",
    sendMail: this.sendMailDefault,
    verzender: [null, this.sendMailDefault ? [Validators.required] : null],
    ontvanger: [
      "",
      this.sendMailDefault
        ? [Validators.required, CustomValidators.email]
        : null,
    ],
  });
  private readonly readForm = trackControl(signal(this.formGroup));
  protected readonly formInvalid = this.readForm((form) => form.invalid);
  protected readonly ontvankelijkDirty = this.readForm(
    (form) => form.get("ontvankelijk")?.dirty,
  );
  protected readonly ontvankelijk = this.readForm(
    (form) => form.get("ontvankelijk")?.value,
  );
  protected readonly sendMail = this.readForm(
    (form) => form.get("sendMail")?.value,
  );
  protected readonly verzender = this.readForm(
    (form) => form.get("verzender")?.value,
  );
  protected readonly redenLength = this.readForm(
    (form) => form.get("reden")?.value.length,
  );
  protected readonly redenError = this.readForm((form) =>
    this.getError(form.get("reden"), "reden-niet-ontvankelijk"),
  );
  protected readonly ontvangerLength = this.readForm(
    (form) => form.get("ontvanger")?.value.length,
  );
  protected readonly ontvangerError = this.readForm((form) =>
    this.getError(form.get("ontvanger"), "ontvanger"),
  );
  readonly afzenders: Observable<GeneratedType<"RestZaakAfzender">[]> =
    this.zakenService.listAfzendersVoorZaak(this.data.zaak.uuid);
  private ngDestroy = new Subject<void>();

  protected readonly afrondenMutation = injectMutation(
    () => this.planItemsService.doUserEventListenerPlanItem(),
    {
      onSuccess: () => this.dialogRef.close(true),
      onError: () => this.dialogRef.close(false),
    },
  );

  constructor() {
    this.zakenService
      .readDefaultAfzenderVoorZaak(this.data.zaak.uuid)
      .subscribe((afzender) => {
        this.formGroup.get("verzender")?.setValue(afzender);
      });
    this.formGroup
      .get("ontvankelijk")
      ?.valueChanges.pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        this.formGroup
          .get("reden")
          ?.setValidators(value ? null : Validators.required);
        this.formGroup.get("reden")?.updateValueAndValidity();
      });
    this.formGroup
      .get("sendMail")
      ?.valueChanges.pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        this.formGroup
          .get("verzender")
          ?.setValidators(value ? [Validators.required] : null);
        this.formGroup.get("verzender")?.updateValueAndValidity();
        this.formGroup
          .get("ontvanger")
          ?.setValidators(
            value ? [Validators.required, CustomValidators.email] : null,
          );
        this.formGroup.get("ontvanger")?.updateValueAndValidity();
      });
  }

  protected getError(fc: AbstractControl | null, label: string) {
    return CustomValidators.getErrorMessage(fc, label, this.translateService);
  }

  protected setOntvanger() {
    this.formGroup.get("ontvanger")?.setValue(this.contactEmailAddress());
  }

  protected close(): void {
    this.dialogRef.close();
  }

  protected afronden(): void {
    this.dialogRef.disableClose = true;
    const values = this.formGroup.value;
    const mailtemplate = values.ontvankelijk
      ? this.zaakOntvankelijkMailQuery.data()
      : this.zaakNietOntvankelijkMailQuery.data();

    this.afrondenMutation.mutate({
      actie: "INTAKE_AFRONDEN",
      planItemInstanceId: this.data.planItem.id,
      zaakUuid: this.data.zaak.uuid,
      isZaakOntvankelijk: values.ontvankelijk,
      resultaatToelichting: values.reden,
      restMailGegevens:
        values.sendMail && mailtemplate
          ? {
              verzender: values.verzender.mail,
              replyTo: values.verzender.replyTo,
              ontvanger: values.ontvanger,
              onderwerp: mailtemplate.onderwerp,
              body: mailtemplate.body,
              shouldCreateDocumentFromMail: true,
              vertrouwelijkheidaanduiding: "OPENBAAR",
            }
          : null,
    });
  }

  ngOnDestroy(): void {
    this.ngDestroy.next();
    this.ngDestroy.complete();
  }

  protected compareObject = (a: unknown, b: unknown) =>
    this.utilService.compare(a, b);
}

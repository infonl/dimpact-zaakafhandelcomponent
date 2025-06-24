/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject, OnDestroy } from "@angular/core";
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  Validators,
} from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { TranslateService } from "@ngx-translate/core";
import { Observable, Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { Mail } from "../../admin/model/mail";
import { Mailtemplate } from "../../admin/model/mailtemplate";
import { UtilService } from "../../core/service/util.service";
import { KlantenService } from "../../klanten/klanten.service";
import { MailtemplateService } from "../../mailtemplate/mailtemplate.service";
import { PlanItemsService } from "../../plan-items/plan-items.service";
import { ActionIcon } from "../../shared/edit/action-icon";
import { GeneratedType } from "../../shared/utils/generated-types";
import { CustomValidators } from "../../shared/validators/customValidators";
import { Zaak } from "../model/zaak";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "intake-afronden-dialog.component.html",
  styleUrls: ["./intake-afronden-dialog.component.less"],
})
export class IntakeAfrondenDialogComponent implements OnDestroy {
  loading = false;
  zaakOntvankelijkMail?: Mailtemplate;
  zaakNietOntvankelijkMail?: Mailtemplate;
  mailBeschikbaar = false;
  sendMailDefault = false;
  initiatorEmail?: string;
  initiatorToevoegenIcon = new ActionIcon(
    "person",
    "actie.initiator.email.toevoegen",
    new Subject<void>(),
  );
  formGroup: FormGroup;
  afzenders: Observable<GeneratedType<"RESTZaakAfzender">[]>;
  private ngDestroy = new Subject<void>();

  constructor(
    public dialogRef: MatDialogRef<IntakeAfrondenDialogComponent>,
    @Inject(MAT_DIALOG_DATA)
    public data: { zaak: Zaak; planItem: GeneratedType<"RESTPlanItem"> },
    private formBuilder: FormBuilder,
    private translateService: TranslateService,
    private planItemsService: PlanItemsService,
    private mailtemplateService: MailtemplateService,
    private klantenService: KlantenService,
    private zakenService: ZakenService,
    private utilService: UtilService,
  ) {
    this.afzenders = this.zakenService.listAfzendersVoorZaak(
      this.data.zaak.uuid,
    );
    this.mailtemplateService
      .findMailtemplate(Mail.ZAAK_ONTVANKELIJK, this.data.zaak.uuid)
      .subscribe((mailtemplate) => {
        this.zaakOntvankelijkMail = mailtemplate;
      });
    this.mailtemplateService
      .findMailtemplate(Mail.ZAAK_NIET_ONTVANKELIJK, this.data.zaak.uuid)
      .subscribe((mailtemplate) => {
        this.zaakNietOntvankelijkMail = mailtemplate;
      });

    const zap = this.data.zaak.zaaktype.zaakafhandelparameters;
    this.mailBeschikbaar = zap.intakeMail !== "NIET_BESCHIKBAAR";
    this.sendMailDefault = zap.intakeMail === "BESCHIKBAAR_AAN";

    if (
      this.data.zaak.initiatorIdentificatieType &&
      this.data.zaak.initiatorIdentificatie
    ) {
      this.klantenService
        .ophalenContactGegevens(this.data.zaak.initiatorIdentificatie)
        .subscribe((gegevens) => {
          if (gegevens.emailadres) {
            this.initiatorEmail = gegevens.emailadres;
          }
        });
    }

    this.formGroup = this.formBuilder.group({
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

  protected getError(fc: AbstractControl, label: string) {
    return CustomValidators.getErrorMessage(fc, label, this.translateService);
  }

  protected setInitatorEmail() {
    this.formGroup.get("ontvanger")?.setValue(this.initiatorEmail);
  }

  protected close(): void {
    this.dialogRef.close();
  }

  protected afronden(): void {
    this.dialogRef.disableClose = true;
    this.loading = true;
    const values = this.formGroup.value;
    const mailtemplate = values.ontvankelijk
      ? this.zaakOntvankelijkMail
      : this.zaakNietOntvankelijkMail;

    console.log("afronden intake", values, mailtemplate);

    this.planItemsService
      .doUserEventListenerPlanItem({
        actie: "INTAKE_AFRONDEN",
        planItemInstanceId: this.data.planItem.id,
        zaakUuid: this.data.zaak.uuid,
        zaakOntvankelijk: values.ontvankelijk,
        resultaatToelichting: values.reden,
        restMailGegevens:
          values.sendMail && mailtemplate
            ? {
                verzender: values.verzender.mail,
                replyTo: values.verzender.replyTo,
                ontvanger: values.ontvanger,
                onderwerp: mailtemplate.onderwerp,
                body: mailtemplate.body,
                createDocumentFromMail: true,
              }
            : null,
      })
      .subscribe({
        next: () => {
          this.dialogRef.close(true);
        },
        error: () => this.dialogRef.close(false),
      });
  }

  ngOnDestroy(): void {
    this.ngDestroy.next();
    this.ngDestroy.complete();
  }

  protected compareObject = (a: unknown, b: unknown) =>
    this.utilService.compare(a, b);
}

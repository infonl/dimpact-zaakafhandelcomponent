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
import { UtilService } from "../../core/service/util.service";
import { KlantenService } from "../../klanten/klanten.service";
import { MailtemplateService } from "../../mailtemplate/mailtemplate.service";
import { PlanItemsService } from "../../plan-items/plan-items.service";
import { ActionIcon } from "../../shared/edit/action-icon";
import { GeneratedType } from "../../shared/utils/generated-types";
import { CustomValidators } from "../../shared/validators/customValidators";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "zaak-afhandelen-dialog.component.html",
  styleUrls: ["./zaak-afhandelen-dialog.component.less"],
})
export class ZaakAfhandelenDialogComponent implements OnDestroy {
  loading = false;
  mailBeschikbaar = false;
  sendMailDefault = false;
  formGroup: FormGroup;
  besluitVastleggen = false;
  mailtemplate?: GeneratedType<"RESTMailtemplate">;
  planItem: GeneratedType<"RESTPlanItem">;
  initiatorEmail?: string;
  initiatorToevoegenIcon = new ActionIcon(
    "person",
    "actie.initiator.email.toevoegen",
    new Subject<void>(),
  );
  resultaattypes: Observable<GeneratedType<"RestResultaattype">[]>;
  afzenders: Observable<GeneratedType<"RESTZaakAfzender">[]>;
  private ngDestroy = new Subject<void>();

  constructor(
    public dialogRef: MatDialogRef<ZaakAfhandelenDialogComponent>,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      zaak: GeneratedType<"RestZaak">;
      planItem: GeneratedType<"RESTPlanItem">;
    },
    private formBuilder: FormBuilder,
    private translateService: TranslateService,
    private zakenService: ZakenService,
    private planItemsService: PlanItemsService,
    private mailtemplateService: MailtemplateService,
    private klantenService: KlantenService,
    private utilService: UtilService,
  ) {
    this.planItem = data.planItem;
    this.resultaattypes = this.zakenService.listResultaattypes(
      this.data.zaak.zaaktype.uuid,
    );
    this.afzenders = this.zakenService.listAfzendersVoorZaak(
      this.data.zaak.uuid,
    );
    this.mailtemplateService
      .findMailtemplate("ZAAK_AFGEHANDELD", this.data.zaak.uuid)
      .subscribe((mailtemplate) => {
        this.mailtemplate = mailtemplate;
      });
    const zap = this.data.zaak.zaaktype.zaakafhandelparameters;
    this.mailBeschikbaar = zap?.afrondenMail !== "NIET_BESCHIKBAAR";
    this.sendMailDefault = zap?.afrondenMail === "BESCHIKBAAR_AAN";

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
      resultaattype: [
        null,
        this.data.zaak.resultaat ? null : [Validators.required],
      ],
      toelichting: "",
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
      .get("sendMail")
      ?.valueChanges.pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        this.formGroup
          ?.get("verzender")
          ?.setValidators(value ? [Validators.required] : null);
        this.formGroup?.get("verzender")?.updateValueAndValidity();
        this.formGroup
          ?.get("ontvanger")
          ?.setValidators(
            value ? [Validators.required, CustomValidators.email] : null,
          );
        this.formGroup?.get("ontvanger")?.updateValueAndValidity();
      });

    this.formGroup
      ?.get("resultaattype")
      ?.valueChanges.pipe(takeUntil(this.ngDestroy))
      .subscribe((value: GeneratedType<"RestResultaattype">) => {
        this.besluitVastleggen = !!value.besluitVerplicht;
        if (this.besluitVastleggen) {
          this.formGroup?.get("toelichting")?.disable();
          this.formGroup?.get("sendMail")?.disable();
          this.formGroup?.get("verzender")?.disable();
          this.formGroup?.get("ontvanger")?.disable();
        } else {
          this.formGroup?.get("toelichting")?.enable();
          this.formGroup?.get("sendMail")?.enable();
          this.formGroup?.get("verzender")?.enable();
          this.formGroup?.get("ontvanger")?.enable();
        }
      });
  }

  protected close(): void {
    this.dialogRef.close();
  }

  protected afhandelen(): void {
    this.dialogRef.disableClose = true;
    this.loading = true;
    const values = this.formGroup.value;

    this.planItemsService
      .doUserEventListenerPlanItem({
        actie: "ZAAK_AFHANDELEN",
        planItemInstanceId: this.planItem.id,
        zaakUuid: this.data.zaak.uuid,
        resultaattypeUuid:
          this.data.zaak.resultaat?.resultaattype?.id ??
          values.resultaattype.id,
        resultaatToelichting: values.toelichting,
        restMailGegevens:
          values.sendMail && this.mailtemplate
            ? {
                verzender: values.verzender.mail,
                replyTo: values.verzender.replyTo,
                ontvanger: values.ontvanger,
                onderwerp: this.mailtemplate.onderwerp,
                body: this.mailtemplate.body,
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

  protected setInitiatorEmail() {
    this.formGroup.get("ontvanger")?.setValue(this.initiatorEmail);
  }

  protected getError(fc: AbstractControl, label: string) {
    return CustomValidators.getErrorMessage(fc, label, this.translateService);
  }

  ngOnDestroy(): void {
    this.ngDestroy.next();
    this.ngDestroy.complete();
  }

  protected openBesluitVastleggen(): void {
    this.dialogRef.close("openBesluitVastleggen");
  }

  protected compareObject = (a: unknown, b: unknown) =>
    this.utilService.compare(a, b);
}

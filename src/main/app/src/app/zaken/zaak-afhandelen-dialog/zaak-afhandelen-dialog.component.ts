/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormBuilder, Validators } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { TranslateService } from "@ngx-translate/core";
import { Moment } from "moment";
import { Observable } from "rxjs";
import { KlantenService } from "../../klanten/klanten.service";
import { MailtemplateService } from "../../mailtemplate/mailtemplate.service";
import { PlanItemsService } from "../../plan-items/plan-items.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { CustomValidators } from "../../shared/validators/customValidators";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "zaak-afhandelen-dialog.component.html",
  styleUrls: ["./zaak-afhandelen-dialog.component.less"],
})
export class ZaakAfhandelenDialogComponent {
  loading = false;
  mailBeschikbaar = false;
  sendMailDefault = false;
  besluitVastleggen = false;
  mailtemplate?: GeneratedType<"RESTMailtemplate">;
  planItem: GeneratedType<"RESTPlanItem">;
  initiatorEmail?: string;

  resultaattypes: Observable<GeneratedType<"RestResultaattype">[]>;
  afzenders: Observable<GeneratedType<"RESTZaakAfzender">[]>;

  formGroup = this.formBuilder.group({
    resultaattype:
      this.formBuilder.control<GeneratedType<"RestResultaattype"> | null>(null),
    toelichting: this.formBuilder.control<string>(""),
    sendMail: this.formBuilder.control<boolean>(false),
    verzender:
      this.formBuilder.control<GeneratedType<"RESTZaakAfzender"> | null>(null),
    ontvanger: this.formBuilder.control<string>("", [CustomValidators.email]),
    brondatumEigenschap: this.formBuilder.control<Moment | null>(null),
  });

  constructor(
    public readonly dialogRef: MatDialogRef<ZaakAfhandelenDialogComponent>,
    @Inject(MAT_DIALOG_DATA)
    public readonly data: {
      zaak: GeneratedType<"RestZaak">;
      planItem: GeneratedType<"RESTPlanItem">;
    },
    private readonly formBuilder: FormBuilder,
    private readonly translateService: TranslateService,
    private readonly zakenService: ZakenService,
    private readonly planItemsService: PlanItemsService,
    private readonly mailtemplateService: MailtemplateService,
    private readonly klantenService: KlantenService,
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
    const zaakafhandelparameters =
      this.data.zaak.zaaktype.zaakafhandelparameters;
    this.mailBeschikbaar =
      zaakafhandelparameters?.afrondenMail !== "NIET_BESCHIKBAAR";
    this.sendMailDefault =
      zaakafhandelparameters?.afrondenMail === "BESCHIKBAAR_AAN";

    if (
      this.data.zaak.initiatorIdentificatieType &&
      this.data.zaak.initiatorIdentificatie
    ) {
      this.klantenService
        .ophalenContactGegevens(this.data.zaak.initiatorIdentificatie)
        .subscribe(({ emailadres }) => {
          if (!emailadres) return;
          this.initiatorEmail = emailadres;
        });
    }

    if (this.data.zaak.resultaat) {
      this.formGroup.controls.resultaattype.addValidators(Validators.required);
    }
    if (this.sendMailDefault) {
      this.formGroup.controls.verzender.addValidators(Validators.required);
      this.formGroup.controls.ontvanger.addValidators([Validators.required]);
    }

    this.zakenService
      .readDefaultAfzenderVoorZaak(this.data.zaak.uuid)
      .subscribe((afzender) => {
        this.formGroup.controls.verzender.setValue(afzender);
      });

    this.formGroup.controls.sendMail.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        this.formGroup.controls.verzender.setValidators(
          value ? [Validators.required] : null,
        );
        this.formGroup.controls.ontvanger.setValidators(
          value ? [Validators.required, CustomValidators.email] : null,
        );
        this.formGroup.updateValueAndValidity();
      });

    this.formGroup.controls.resultaattype.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        this.besluitVastleggen = value?.besluitVerplicht ?? false;
        if (this.besluitVastleggen) {
          this.formGroup.controls.toelichting.disable();
          this.formGroup.controls.sendMail.disable();
          this.formGroup.controls.verzender.disable();
          this.formGroup.controls.ontvanger.disable();
          return;
        }

        this.formGroup.controls.toelichting.enable();
        this.formGroup.controls.sendMail.enable();
        this.formGroup.controls.verzender.enable();
        this.formGroup.controls.ontvanger.enable();
      });
  }

  protected close() {
    this.dialogRef.close();
  }

  protected afhandelen() {
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
          values.resultaattype?.id,
        resultaatToelichting: values.toelichting,
        restMailGegevens:
          values.sendMail && this.mailtemplate
            ? {
                verzender: values.verzender?.mail,
                replyTo: values.verzender?.replyTo,
                ontvanger: values.ontvanger ?? undefined,
                onderwerp: this.mailtemplate.onderwerp,
                body: this.mailtemplate.body,
                createDocumentFromMail: true,
              }
            : null,
        brondatumEigenschap: values.brondatumEigenschap?.toISOString() ?? null,
      })
      .subscribe({
        next: () => {
          this.dialogRef.close(true);
        },
        error: () => this.dialogRef.close(false),
      });
  }

  protected setInitiatorEmail() {
    this.formGroup.controls.ontvanger.setValue(this.initiatorEmail ?? null);
  }

  protected afzenderOptionDisplayValue(
    afzender: GeneratedType<"RESTZaakAfzender">,
  ) {
    const suffix = afzender.suffix
      ? ` ${this.translateService.instant(afzender.suffix)}`
      : "";
    return `${afzender.mail}${suffix}`;
  }

  protected openBesluitVastleggen() {
    this.dialogRef.close("openBesluitVastleggen");
  }
}

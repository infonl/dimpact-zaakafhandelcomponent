/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Inject, OnDestroy, OnInit } from "@angular/core";
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
import { ZaakAfzender } from "../../admin/model/zaakafzender";
import { KlantenService } from "../../klanten/klanten.service";
import { MailtemplateService } from "../../mailtemplate/mailtemplate.service";
import { PlanItem } from "../../plan-items/model/plan-item";
import { UserEventListenerActie } from "../../plan-items/model/user-event-listener-actie-enum";
import { UserEventListenerData } from "../../plan-items/model/user-event-listener-data";
import { PlanItemsService } from "../../plan-items/plan-items.service";
import { ActionIcon } from "../../shared/edit/action-icon";
import { GeneratedType } from "../../shared/utils/generated-types";
import { CustomValidators } from "../../shared/validators/customValidators";
import { Zaak } from "../model/zaak";
import { ZaakStatusmailOptie } from "../model/zaak-statusmail-optie";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "zaak-afhandelen-dialog.component.html",
  styleUrls: ["./zaak-afhandelen-dialog.component.less"],
})
export class ZaakAfhandelenDialogComponent implements OnInit, OnDestroy {
  loading = false;
  mailBeschikbaar = false;
  sendMailDefault = false;
  formGroup: FormGroup;
  besluitVastleggen = false;
  mailtemplate: Mailtemplate;
  zaak: Zaak;
  planItem: PlanItem;
  initiatorEmail: string;
  initiatorToevoegenIcon: ActionIcon = new ActionIcon(
    "person",
    "actie.initiator.email.toevoegen",
    new Subject<void>(),
  );
  resultaattypes: Observable<GeneratedType<"RestResultaattype">[]>;
  afzenders: Observable<ZaakAfzender[]>;
  private ngDestroy = new Subject<void>();

  constructor(
    public dialogRef: MatDialogRef<ZaakAfhandelenDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { zaak: Zaak; planItem: PlanItem },
    private formBuilder: FormBuilder,
    private translateService: TranslateService,
    private zakenService: ZakenService,
    private planItemsService: PlanItemsService,
    private mailtemplateService: MailtemplateService,
    private klantenService: KlantenService,
  ) {
    this.zaak = data.zaak;
    this.planItem = data.planItem;
    this.resultaattypes = this.zakenService.listResultaattypes(
      this.data.zaak.zaaktype.uuid,
    );
    this.afzenders = this.zakenService.listAfzendersVoorZaak(
      this.data.zaak.uuid,
    );
    this.mailtemplateService
      .findMailtemplate(Mail.ZAAK_AFGEHANDELD, this.data.zaak.uuid)
      .subscribe((mailtemplate) => {
        this.mailtemplate = mailtemplate;
      });
  }

  ngOnInit(): void {
    const zap = this.zaak.zaaktype.zaakafhandelparameters;
    this.mailBeschikbaar =
      zap.afrondenMail !== ZaakStatusmailOptie.NIET_BESCHIKBAAR;
    this.sendMailDefault =
      zap.afrondenMail === ZaakStatusmailOptie.BESCHIKBAAR_AAN;

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
      resultaattype: [null, this.zaak.resultaat ? null : [Validators.required]],
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
        this.formGroup.get("verzender").setValue(afzender);
      });
    this.formGroup
      .get("sendMail")
      .valueChanges.pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        this.formGroup
          .get("verzender")
          .setValidators(value ? [Validators.required] : null);
        this.formGroup.get("verzender").updateValueAndValidity();
        this.formGroup
          .get("ontvanger")
          .setValidators(
            value ? [Validators.required, CustomValidators.email] : null,
          );
        this.formGroup.get("ontvanger").updateValueAndValidity();
      });
    this.formGroup
      .get("resultaattype")
      .valueChanges.pipe(takeUntil(this.ngDestroy))
      .subscribe((value: GeneratedType<"RestResultaattype">) => {
        this.besluitVastleggen = value.besluitVerplicht;
        if (this.besluitVastleggen) {
          this.formGroup.get("toelichting").disable();
          this.formGroup.get("sendMail").disable();
          this.formGroup.get("verzender").disable();
          this.formGroup.get("ontvanger").disable();
        } else {
          this.formGroup.get("toelichting").enable();
          this.formGroup.get("sendMail").enable();
          this.formGroup.get("verzender").enable();
          this.formGroup.get("ontvanger").enable();
        }
      });
  }

  close(): void {
    this.dialogRef.close();
  }

  afhandelen(): void {
    this.dialogRef.disableClose = true;
    this.loading = true;
    const values = this.formGroup.value;
    const userEventListenerData = new UserEventListenerData(
      UserEventListenerActie.ZaakAfhandelen,
      this.planItem.id,
      this.zaak.uuid,
    );
    userEventListenerData.resultaattypeUuid = this.zaak.resultaat
      ? this.zaak.resultaat.resultaattype.id
      : values.resultaattype.id;
    userEventListenerData.resultaatToelichting = values.toelichting;

    if (values.sendMail && this.mailtemplate) {
      const restMailGegevens: GeneratedType<"RESTMailGegevens"> = {
        verzender: values.verzender.mail,
        replyTo: values.verzender.replyTo,
        ontvanger: values.ontvanger,
        onderwerp: this.mailtemplate.onderwerp,
        body: this.mailtemplate.body,
        createDocumentFromMail: true,
      };

      Object.assign(userEventListenerData, { restMailGegevens });
    }

    this.planItemsService
      .doUserEventListenerPlanItem(userEventListenerData)
      .subscribe({
        next: () => {
          this.dialogRef.close(true);
        },
        error: () => this.dialogRef.close(false),
      });
  }

  setInitatorEmail() {
    this.formGroup.get("ontvanger").setValue(this.initiatorEmail);
  }

  getError(fc: AbstractControl, label: string) {
    return CustomValidators.getErrorMessage(fc, label, this.translateService);
  }

  ngOnDestroy(): void {
    this.ngDestroy.next();
    this.ngDestroy.complete();
  }

  openBesluitVastleggen(): void {
    this.dialogRef.close("openBesluitVastleggen");
  }

  compareObject(object1: any, object2: any): boolean {
    if (typeof object1 === "string") {
      return object1 === object2;
    }
    if (object1 && object2) {
      if (object1.hasOwnProperty("key")) {
        return object1.key === object2.key;
      } else if (object1.hasOwnProperty("id")) {
        return object1.id === object2.id;
      } else if (object1.hasOwnProperty("naam")) {
        return object1.naam === object2.naam;
      } else if (object1.hasOwnProperty("name")) {
        return object1.name === object2.name;
      }
      return object1 === object2;
    }
    return false;
  }
}

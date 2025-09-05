/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { FormBuilder, Validators } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import { UtilService } from "../../core/service/util.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { KlantenService } from "../../klanten/klanten.service";
import { MailtemplateService } from "../../mailtemplate/mailtemplate.service";
import { DocumentenLijstFieldBuilder } from "../../shared/material-form-builder/form-components/documenten-lijst/documenten-lijst-field-builder";
import { AbstractFormField } from "../../shared/material-form-builder/model/abstract-form-field";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../../zaken/zaken.service";
import { MailService } from "../mail.service";

@Component({
  selector: "zac-ontvangstbevestiging",
  templateUrl: "./ontvangstbevestiging.component.html",
})
export class OntvangstbevestigingComponent implements OnInit {
  @Input({ required: true }) sideNav!: MatDrawer;
  @Input({ required: true }) zaak!: GeneratedType<"RestZaak">;
  @Output() ontvangstBevestigd = new EventEmitter<boolean>();

  protected afzenders: GeneratedType<"RestZaakAfzender">[] = [];
  protected variables: GeneratedType<"MailTemplateVariables">[] = [];
  protected bijlagenFormField!: AbstractFormField;
  protected contactGegevens: GeneratedType<"RestContactGegevens"> | null = null;

  protected readonly form = this.formBuilder.group({
    verzender:
      this.formBuilder.control<GeneratedType<"RestZaakAfzender"> | null>(null, [
        Validators.required,
      ]),
    ontvanger: this.formBuilder.control<string | null>(null, [
      Validators.required,
      Validators.maxLength(200),
      Validators.email,
    ]),
    onderwerp: this.formBuilder.control<string | null>(null, [
      Validators.required,
      Validators.maxLength(100),
    ]),
    body: this.formBuilder.control<string | null>(null, [Validators.required]),
  });

  constructor(
    private readonly zakenService: ZakenService,
    private readonly informatieObjectenService: InformatieObjectenService,
    private readonly mailService: MailService,
    private readonly mailtemplateService: MailtemplateService,
    private readonly utilService: UtilService,
    private readonly klantenService: KlantenService,
    private readonly formBuilder: FormBuilder,
  ) {}

  ngOnInit() {
    const documenten =
      this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
        zaakUUID: this.zaak.uuid,
      });

    this.bijlagenFormField = new DocumentenLijstFieldBuilder()
      .id("bijlagen")
      .label("bijlagen")
      .documenten(documenten)
      .build();

    this.zakenService
      .listAfzendersVoorZaak(this.zaak.uuid)
      .subscribe((afzenders) => {
        this.afzenders = afzenders;
      });

    this.mailtemplateService
      .findMailtemplate("TAAK_ONTVANGSTBEVESTIGING", this.zaak.uuid)
      .subscribe((mailtemplate) => {
        this.form.controls.onderwerp.setValue(mailtemplate?.onderwerp);
        this.form.controls.body.setValue(mailtemplate?.body);
        this.variables = mailtemplate?.variabelen ?? [];
      });

    if (!this.zaak.initiatorIdentificatie?.bsnNummer) return;

    this.klantenService
      .ophalenContactGegevens(this.zaak.initiatorIdentificatie.bsnNummer)
      .subscribe((gegevens) => {
        this.contactGegevens = gegevens;
      });
  }

  setOntvanger() {
    this.form.controls.ontvanger.setValue(
      this.contactGegevens?.emailadres ?? null,
    );
  }

  submit() {
    const { value } = this.form;
    this.mailService
      .sendAcknowledgeReceipt(this.zaak.uuid, {
        ...value,
        verzender: value.verzender?.mail,
        replyTo: value.verzender?.replyTo,
        bijlagen:
          (this.bijlagenFormField.formControl.value as string) ?? undefined,
        createDocumentFromMail: true,
      })
      .subscribe(() => {
        this.utilService.openSnackbar("msg.email.verstuurd");
        this.ontvangstBevestigd.emit(true);
      });
  }
}

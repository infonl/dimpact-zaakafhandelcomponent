/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
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
import { DocumentenLijstFormField } from "../../shared/material-form-builder/form-components/documenten-lijst/documenten-lijst-form-field";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../../zaken/zaken.service";
import { MailService } from "../mail.service";

@Component({
  selector: "zac-mail-create",
  templateUrl: "./mail-create.component.html",
  styleUrls: ["./mail-create.component.less"],
})
export class MailCreateComponent implements OnInit {
  @Input({ required: true }) zaak!: GeneratedType<"RestZaak">;
  @Input({ required: true }) sideNav!: MatDrawer;
  @Output() mailVerstuurd = new EventEmitter<boolean>();

  bijlagenFormField!: DocumentenLijstFormField; // Assigned in the `ngOnInit` method

  protected form = this.formBuilder.group({
    verzender:
      this.formBuilder.control<GeneratedType<"RestZaakAfzender"> | null>(null, [
        Validators.required,
      ]),
    ontvanger: this.formBuilder.control("", [
      Validators.required,
      Validators.email,
      Validators.maxLength(200),
    ]),
    onderwerp: this.formBuilder.control("", [
      Validators.required,
      Validators.maxLength(100),
    ]),
    body: this.formBuilder.control("", [Validators.required]),
  });

  protected verzenderOptions: GeneratedType<"RestZaakAfzender">[] = [];
  protected contactGegevens: GeneratedType<"RestContactGegevens"> | null = null;
  protected variabelen: string[] = [];

  constructor(
    private readonly zakenService: ZakenService,
    private readonly informatieObjectenService: InformatieObjectenService,
    private readonly mailService: MailService,
    private readonly mailtemplateService: MailtemplateService,
    private readonly klantenService: KlantenService,
    private readonly utilService: UtilService,
    private readonly formBuilder: FormBuilder,
  ) {}

  ngOnInit() {
    this.zakenService
      .listAfzendersVoorZaak(this.zaak.uuid)
      .subscribe((afzenders) => {
        this.verzenderOptions = afzenders;
      });

    this.mailtemplateService
      .findMailtemplate("ZAAK_ALGEMEEN", this.zaak.uuid)
      .subscribe((mailTemplate) => {
        this.form.controls.onderwerp.setValue(mailTemplate.onderwerp ?? null);
        this.form.controls.body.setValue(mailTemplate.body ?? null);
        console.log("Mail template:", mailTemplate);
        this.variabelen = mailTemplate.variabelen ?? [];
      });

    this.zakenService
      .readDefaultAfzenderVoorZaak(this.zaak.uuid)
      .subscribe((defaultVerzenderVoorZaak) => {
        this.form.controls.verzender.setValue(defaultVerzenderVoorZaak);
      });

    const documenten =
      this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
        zaakUUID: this.zaak.uuid,
      });

    this.bijlagenFormField = new DocumentenLijstFieldBuilder()
      .id("bijlagen")
      .label("bijlagen")
      .documenten(documenten)
      .openInNieuweTab()
      .build();

    if (
      !this.zaak.initiatorIdentificatie?.type ||
      !this.zaak.initiatorIdentificatie?.bsnNummer
    )
      return;

    this.klantenService
      .ophalenContactGegevens(this.zaak.initiatorIdentificatie.bsnNummer)
      .subscribe((contactGegevens) => {
        if (!contactGegevens.emailadres) return;
        this.contactGegevens = contactGegevens;
      });
  }

  onFormSubmit() {
    const { value } = this.form;

    this.mailService
      .sendMail(this.zaak.uuid, {
        verzender: value.verzender?.mail ?? undefined,
        replyTo: value.verzender?.replyTo ?? undefined,
        ontvanger: value.ontvanger ?? undefined,
        onderwerp: value.onderwerp ?? "",
        body: value.body ?? "",
        bijlagen: this.bijlagenFormField?.formControl.value ?? "",
        createDocumentFromMail: true,
      })
      .subscribe({
        next: () => {
          this.utilService.openSnackbar("msg.email.verstuurd");
          this.mailVerstuurd.emit(true);
        },
        error: () => {
          this.mailVerstuurd.emit(false);
        },
      });
  }

  protected setOntvanger() {
    this.form.controls.ontvanger.setValue(
      this.contactGegevens?.emailadres ?? null,
    );
  }
}

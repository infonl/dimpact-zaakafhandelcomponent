/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import { Component, input, OnInit, output } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatDividerModule } from "@angular/material/divider";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatDrawer } from "@angular/material/sidenav";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";
import { injectMutation } from "@tanstack/angular-query-experimental";
import { UtilService } from "../../core/service/util.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { KlantenService } from "../../klanten/klanten.service";
import { MailtemplateService } from "../../mailtemplate/mailtemplate.service";
import { DocumentenLijstFormField } from "../../shared/material-form-builder/form-components/documenten-lijst/documenten-lijst-form-field";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../../zaken/zaken.service";
import { MailService } from "../mail.service";

@Component({
  selector: "zac-mail-create",
  templateUrl: "./mail-create.component.html",
  styleUrls: ["./mail-create.component.less"],
  standalone: true,
  imports: [
    NgIf,
    ReactiveFormsModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    MatFormFieldModule,
    TranslateModule,
    MaterialFormBuilderModule,
  ],
})
export class MailCreateComponent implements OnInit {
  protected readonly zaak = input.required<GeneratedType<"RestZaak">>();
  protected readonly sideNav = input.required<MatDrawer>();

  protected readonly mailVerstuurd = output<boolean>();

  protected readonly sendMailMutation = injectMutation(() => ({
    ...this.mailService.sendMail(this.zaak().uuid),
    onSuccess: () => {
      this.utilService.openSnackbar("msg.email.verstuurd");
      this.mailVerstuurd.emit(true);
    },
    onError: () => {
      this.mailVerstuurd.emit(false);
    },
  }));

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
    bijlagen: this.formBuilder.control<
      GeneratedType<"RestEnkelvoudigInformatieobject">[]
    >([], []),
  });

  protected verzenderOptions: GeneratedType<"RestZaakAfzender">[] = [];
  protected contactEmailAddress: string | null = null;
  protected variabelen: string[] = [];
  protected documents: GeneratedType<"RestEnkelvoudigInformatieobject">[] = [];

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
      .listAfzendersVoorZaak(this.zaak().uuid)
      .subscribe((afzenders) => {
        this.verzenderOptions = afzenders;
      });

    this.mailtemplateService
      .findMailtemplate("ZAAK_ALGEMEEN", this.zaak().uuid)
      .subscribe((mailTemplate) => {
        this.form.patchValue({
          onderwerp: mailTemplate.onderwerp,
          body: mailTemplate.body,
        });
        this.variabelen = mailTemplate.variabelen ?? [];
      });

    this.zakenService
      .readDefaultAfzenderVoorZaak(this.zaak().uuid)
      .subscribe((defaultVerzenderVoorZaak) => {
        this.form.controls.verzender.setValue(defaultVerzenderVoorZaak);
      });

    this.informatieObjectenService
      .listEnkelvoudigInformatieobjecten({
        zaakUUID: this.zaak().uuid,
      })
      .subscribe((documents) => {
        this.documents = documents;
      });

    const emailAddress = this.zaak().zaakSpecificContactDetails?.emailAddress;
    if (emailAddress) {
      this.contactEmailAddress = emailAddress;
      return;
    }

    const temporaryPersonId =
      this.zaak().initiatorIdentificatie?.temporaryPersonId;
    if (!temporaryPersonId) return;

    this.klantenService
      .getContactDetailsForPerson(temporaryPersonId)
      .subscribe((gegevens) => {
        this.contactEmailAddress = gegevens?.emailadres ?? null;
      });
  }

  onFormSubmit() {
    const { value } = this.form;

    this.sendMailMutation.mutate({
      ...value,
      verzender: value.verzender?.mail,
      replyTo: value.verzender?.replyTo,
      onderwerp: value.onderwerp ?? "",
      body: value.body ?? "",
      bijlagen: value.bijlagen?.map(({ uuid }) => uuid).join(";"),
      createDocumentFromMail: true,
    });
  }

  protected setOntvanger() {
    this.form.controls.ontvanger.setValue(this.contactEmailAddress ?? null);
  }
}

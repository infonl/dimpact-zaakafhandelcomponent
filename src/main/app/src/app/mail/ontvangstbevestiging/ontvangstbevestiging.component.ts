/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import { Component, inject, input, OnInit, output } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatDividerModule } from "@angular/material/divider";
import { MatIconModule } from "@angular/material/icon";
import { MatDrawer } from "@angular/material/sidenav";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";
import { injectMutation } from "@tanstack/angular-query-experimental";
import { UtilService } from "../../core/service/util.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { KlantenService } from "../../klanten/klanten.service";
import { MailtemplateService } from "../../mailtemplate/mailtemplate.service";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../../zaken/zaken.service";
import { MailService } from "../mail.service";

@Component({
  selector: "zac-ontvangstbevestiging",
  templateUrl: "./ontvangstbevestiging.component.html",
  standalone: true,
  imports: [
    NgIf,
    ReactiveFormsModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    TranslateModule,
    MaterialFormBuilderModule,
  ],
})
export class OntvangstbevestigingComponent implements OnInit {
  private readonly zakenService = inject(ZakenService);
  private readonly informatieObjectenService = inject(
    InformatieObjectenService,
  );
  private readonly mailService = inject(MailService);
  private readonly mailtemplateService = inject(MailtemplateService);
  private readonly utilService = inject(UtilService);
  private readonly klantenService = inject(KlantenService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly sideNav = input.required<MatDrawer>();
  protected readonly zaak = input.required<GeneratedType<"RestZaak">>();

  protected readonly ontvangstBevestigd = output<boolean>();

  protected afzenders: GeneratedType<"RestZaakAfzender">[] = [];
  protected variables: GeneratedType<"MailTemplateVariables">[] = [];
  protected contactEmailAddress: string | null = null;
  protected documents: GeneratedType<"RestEnkelvoudigInformatieobject">[] = [];

  protected readonly sendAcknowledgeReceiptMutation = injectMutation(() => ({
    ...this.mailService.sendAcknowledgeReceipt(this.zaak().uuid),
    onSuccess: () => {
      this.utilService.openSnackbar("msg.email.verstuurd");
      this.ontvangstBevestigd.emit(true);
    },
  }));

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
    bijlagen: this.formBuilder.control<
      GeneratedType<"RestEnkelvoudigInformatieobject">[]
    >([]),
  });

  ngOnInit() {
    this.informatieObjectenService
      .listEnkelvoudigInformatieobjecten({
        zaakUUID: this.zaak().uuid,
      })
      .subscribe((documents) => {
        this.documents = documents;
      });

    this.zakenService
      .listAfzendersVoorZaak(this.zaak().uuid)
      .subscribe((afzenders) => {
        this.afzenders = afzenders;
      });

    this.zakenService
      .readDefaultAfzenderVoorZaak(this.zaak().uuid)
      .subscribe((defaultVerzenderVoorZaak) => {
        this.form.controls.verzender.setValue(defaultVerzenderVoorZaak);
      });

    this.mailtemplateService
      .findMailtemplate("TAAK_ONTVANGSTBEVESTIGING", this.zaak().uuid)
      .subscribe((mailtemplate) => {
        this.form.controls.onderwerp.setValue(mailtemplate?.onderwerp);
        this.form.controls.body.setValue(mailtemplate?.body);
        this.variables = mailtemplate?.variabelen ?? [];
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

  protected setOntvanger() {
    this.form.controls.ontvanger.setValue(this.contactEmailAddress ?? null);
  }

  protected submit() {
    const { value } = this.form;
    this.sendAcknowledgeReceiptMutation.mutate({
      ...value,
      verzender: value.verzender?.mail,
      replyTo: value.verzender?.replyTo,
      bijlagen: value.bijlagen?.map(({ uuid }) => uuid).join(";"),
      createDocumentFromMail: true,
    });
  }
}

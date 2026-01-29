/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject, input, OnInit, output } from "@angular/core";
import { FormBuilder, Validators } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import { injectMutation } from "@tanstack/angular-query-experimental";
import { UtilService } from "../../core/service/util.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { KlantenService } from "../../klanten/klanten.service";
import { MailtemplateService } from "../../mailtemplate/mailtemplate.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../../zaken/zaken.service";
import { MailService } from "../mail.service";

@Component({
  selector: "zac-ontvangstbevestiging",
  templateUrl: "./ontvangstbevestiging.component.html",
  standalone: false,
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
  protected contactGegevens: GeneratedType<"RestContactDetails"> | null = null;
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

    this.mailtemplateService
      .findMailtemplate("TAAK_ONTVANGSTBEVESTIGING", this.zaak().uuid)
      .subscribe((mailtemplate) => {
        this.form.controls.onderwerp.setValue(mailtemplate?.onderwerp);
        this.form.controls.body.setValue(mailtemplate?.body);
        this.variables = mailtemplate?.variabelen ?? [];
      });

    this.zakenService
        .readDefaultAfzenderVoorZaak(this.zaak().uuid)
        .subscribe((defaultVerzenderVoorZaak) => {
          this.form.controls.verzender.setValue(defaultVerzenderVoorZaak);
        });

    const bsnNummer = this.zaak().initiatorIdentificatie?.bsnNummer;
    if (!bsnNummer) return;

    this.klantenService
      .getContactDetailsForPerson(bsnNummer)
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
    this.sendAcknowledgeReceiptMutation.mutate({
      ...value,
      verzender: value.verzender?.mail,
      replyTo: value.verzender?.replyTo,
      bijlagen: value.bijlagen?.map(({ uuid }) => uuid).join(";"),
      createDocumentFromMail: true,
    });
  }
}

/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, computed, inject, input } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatDividerModule } from "@angular/material/divider";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatError } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatMenuModule } from "@angular/material/menu";
import { MatRadioModule } from "@angular/material/radio";
import { MatTableModule } from "@angular/material/table";
import { TranslateDirective, TranslatePipe } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { TypedMatCellDefDirective } from "../../../../shared/directives/typed-mat-cell-def.directive";
import { trackControl } from "../../../../shared/form/control-state";
import { ZacSelect } from "../../../../shared/form/select/select";
import { ZacToggle } from "../../../../shared/form/toggle/toggle";
import { I18nKeyPipe } from "../../../../shared/pipes/i18n-key.pipe";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { toI18nKey } from "../../../../shared/utils/i18n-key";
import { MailtemplateBeheerService } from "../../../mailtemplate-beheer.service";
import { getBeschikbareMailtemplateKoppelingen } from "../../../model/mail-utils";
import { ReferentieTabelService } from "../../../referentie-tabel.service";
import { ZaakafhandelParametersService } from "../../../zaakafhandel-parameters.service";
import {
  createZaakAfzenderForm,
  MailForm,
  ZaakAfzenderForm,
} from "./mail-form";

@Component({
  selector: "zac-parameters-step-mail",
  templateUrl: "./parameters-step-mail.component.html",
  styleUrl: "./parameters-step-mail.component.less",
  standalone: true,
  imports: [
    MatButtonModule,
    MatCardModule,
    MatDividerModule,
    MatExpansionModule,
    MatError,
    MatIconModule,
    MatMenuModule,
    MatRadioModule,
    MatTableModule,
    TypedMatCellDefDirective,
    TranslateDirective,
    TranslatePipe,
    I18nKeyPipe,
    ZacSelect,
    ZacToggle,
  ],
})
export class ParametersStepMailComponent {
  readonly form = input.required<MailForm>();

  private readonly mailtemplateBeheerService = inject(
    MailtemplateBeheerService,
  );
  private readonly referentieTabelService = inject(ReferentieTabelService);
  private readonly zaakafhandelParametersService = inject(
    ZaakafhandelParametersService,
  );

  private readonly mailtemplatesQuery = injectQuery(() =>
    this.mailtemplateBeheerService.listKoppelbareMailtemplates(),
  );
  private readonly afzendersQuery = injectQuery(() =>
    this.referentieTabelService.listAfzenders(),
  );
  protected readonly replyTosQuery = injectQuery(() =>
    this.zaakafhandelParametersService.listReplyTos(),
  );

  protected readonly mailtemplateKoppelingen =
    getBeschikbareMailtemplateKoppelingen();

  protected readonly mailOpties: {
    label: string;
    value: GeneratedType<"ZaakafhandelparametersStatusMailOption">;
  }[] = [
    { label: "statusmail.optie.beschikbaar-aan", value: "BESCHIKBAAR_AAN" },
    { label: "statusmail.optie.beschikbaar-uit", value: "BESCHIKBAAR_UIT" },
    { label: "statusmail.optie.niet-beschikbaar", value: "NIET_BESCHIKBAAR" },
  ];

  protected readonly mailOptieValues = this.mailOpties.map(
    ({ value }) => value,
  );
  protected readonly mailOptieLabel = (
    value: GeneratedType<"ZaakafhandelparametersStatusMailOption">,
  ) =>
    this.mailOpties.find((mailOptie) => mailOptie.value === value)?.label ?? "";

  protected readonly mailtemplatesPerMail = computed(() =>
    Object.fromEntries(
      this.mailtemplateKoppelingen.map((mail) => [
        mail,
        this.mailtemplates(mail),
      ]),
    ),
  );

  protected readonly replyToMails = computed(() =>
    (this.replyTosQuery.data() ?? []).map(({ mail }) => mail ?? ""),
  );

  private readonly readForm = trackControl(this.form);
  protected readonly enabled = this.readForm(({ controls }) => ({
    intakeMail: controls.intakeMail.enabled,
    afrondenMail: controls.afrondenMail.enabled,
    automaticEmailConfirmation: controls.automaticEmailConfirmation.enabled,
  }));
  protected readonly isOntvangstbevestigingEnabled = this.readForm(
    ({ controls }) =>
      controls.automaticEmailConfirmation.controls.isEnabled.value,
  );
  protected readonly hasNoDefaultAfzender = this.readForm(({ controls }) =>
    controls.zaakAfzenders.hasError("noDefaultAfzender"),
  );
  protected readonly zaakAfzenders = this.readForm(({ controls }) =>
    controls.zaakAfzenders.controls
      .map((zaakAfzenderForm) => ({
        ...zaakAfzenderForm.getRawValue(),
        form: zaakAfzenderForm,
      }))
      .sort((a, b) =>
        a.isSpeciaal !== b.isSpeciaal
          ? a.isSpeciaal
            ? -1
            : 1
          : a.mail.localeCompare(b.mail),
      ),
  );
  protected readonly trackZaakAfzender = (
    _: number,
    { form }: { form: ZaakAfzenderForm },
  ) => form;

  protected readonly addableAfzenders = computed(() => {
    const mails = (this.zaakAfzenders() ?? []).map(({ mail }) => mail);
    return (this.afzendersQuery.data() ?? [])
      .filter((afzender) => !mails.includes(afzender))
      .sort((a, b) => a.localeCompare(b));
  });

  protected readonly ontvangstbevestigingTemplateNames = computed(() =>
    this.mailtemplates("TAAK_ONTVANGSTBEVESTIGING").map(
      ({ mailTemplateNaam }) => mailTemplateNaam,
    ),
  );

  protected readonly ontvangstbevestigingAfzenders = computed(() =>
    (this.replyTosQuery.data() ?? [])
      .filter(({ isSpeciaal, mail }) => !(isSpeciaal && mail === "MEDEWERKER"))
      .map(({ mail }) => mail ?? ""),
  );

  protected readonly compareMailtemplate = (
    a?: GeneratedType<"RestMailtemplate"> | null,
    b?: GeneratedType<"RestMailtemplate"> | null,
  ) => a?.id === b?.id;

  protected readonly replyToDisplayValue = (mail: string | null) => {
    const replyTo = this.replyTosQuery
      .data()
      ?.find((candidate) => candidate.mail === mail);
    return replyTo?.isSpeciaal
      ? toI18nKey("gegevens.mail.afzender." + replyTo.mail)
      : (mail ?? "");
  };

  protected mailtemplates(mail: GeneratedType<"Mail">) {
    return (this.mailtemplatesQuery.data() ?? []).filter(
      (mailtemplate) => mailtemplate.mail === mail,
    );
  }

  protected chooseDefaultAfzender(zaakAfzenderForm: ZaakAfzenderForm) {
    for (const candidate of this.form().controls.zaakAfzenders.controls) {
      candidate.controls.isDefaultMail.setValue(candidate === zaakAfzenderForm);
    }
  }

  protected addZaakAfzender(mail: string) {
    const { zaakAfzenders } = this.form().controls;
    zaakAfzenders.push(
      createZaakAfzenderForm({ mail, isSpeciaal: false, isDefaultMail: false }),
    );
    zaakAfzenders.markAsTouched();
  }

  protected removeZaakAfzender(zaakAfzenderForm: ZaakAfzenderForm) {
    const { zaakAfzenders } = this.form().controls;
    zaakAfzenders.removeAt(zaakAfzenders.controls.indexOf(zaakAfzenderForm));
    zaakAfzenders.markAsTouched();
  }
}

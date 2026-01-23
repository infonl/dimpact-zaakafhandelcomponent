/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { FormGroup, Validators } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import { TranslateService } from "@ngx-translate/core";
import { Subject } from "rxjs";
import { Mail } from "../../admin/model/mail";
import { UtilService } from "../../core/service/util.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { InformatieobjectZoekParameters } from "../../informatie-objecten/model/informatieobject-zoek-parameters";
import { KlantenService } from "../../klanten/klanten.service";
import { MailtemplateService } from "../../mailtemplate/mailtemplate.service";
import { ActionIcon } from "../../shared/edit/action-icon";
import { DocumentenLijstFieldBuilder } from "../../shared/material-form-builder/form-components/documenten-lijst/documenten-lijst-field-builder";
import { HtmlEditorFormFieldBuilder } from "../../shared/material-form-builder/form-components/html-editor/html-editor-form-field-builder";
import { InputFormFieldBuilder } from "../../shared/material-form-builder/form-components/input/input-form-field-builder";
import { SelectFormFieldBuilder } from "../../shared/material-form-builder/form-components/select/select-form-field-builder";
import { AbstractFormField } from "../../shared/material-form-builder/model/abstract-form-field";
import { FormConfig } from "../../shared/material-form-builder/model/form-config";
import { FormConfigBuilder } from "../../shared/material-form-builder/model/form-config-builder";
import { GeneratedType } from "../../shared/utils/generated-types";
import { CustomValidators } from "../../shared/validators/customValidators";
import { TakenService } from "../../taken/taken.service";
import { Zaak } from "../../zaken/model/zaak";
import { ZakenService } from "../../zaken/zaken.service";
import { MailService } from "../mail.service";

@Component({
  selector: "zac-ontvangstbevestiging",
  templateUrl: "./ontvangstbevestiging.component.html",
  styleUrls: ["./ontvangstbevestiging.component.less"],
})
export class OntvangstbevestigingComponent implements OnInit {
  formConfig: FormConfig;
  fields: Array<AbstractFormField[]>;
  @Input() sideNav: MatDrawer;
  @Input() zaak: Zaak;
  @Output() ontvangstBevestigd = new EventEmitter<boolean>();

  constructor(
    private zakenService: ZakenService,
    private informatieObjectenService: InformatieObjectenService,
    private mailService: MailService,
    private mailtemplateService: MailtemplateService,
    public takenService: TakenService,
    public utilService: UtilService,
    public translateService: TranslateService,
    private klantenService: KlantenService,
  ) {}

  ngOnInit(): void {
    this.formConfig = new FormConfigBuilder()
      .saveText("actie.versturen")
      .cancelText("actie.annuleren")
      .build();

    const zoekparameters = new InformatieobjectZoekParameters();
    zoekparameters.zaakUUID = this.zaak.uuid;
    const documenten =
      this.informatieObjectenService.listEnkelvoudigInformatieobjecten(
        zoekparameters,
      );
    const mailtemplate = this.mailtemplateService.findMailtemplate(
      Mail.TAAK_ONTVANGSTBEVESTIGING,
      this.zaak.uuid,
    );

    const verzender = new SelectFormFieldBuilder()
      .id("verzender")
      .label("verzender")
      .options(this.zakenService.listAfzendersVoorZaak(this.zaak.uuid))
      .optionLabel("mail")
      .optionSuffix("suffix")
      .value$(this.zakenService.readDefaultAfzenderVoorZaak(this.zaak.uuid))
      .validators(Validators.required)
      .build();
    const ontvanger = new InputFormFieldBuilder()
      .id("ontvanger")
      .label("ontvanger")
      .validators(Validators.required, CustomValidators.email)
      .maxlength(200)
      .build();
    const onderwerp = new HtmlEditorFormFieldBuilder()
      .id("onderwerp")
      .label("onderwerp")
      .validators(Validators.required)
      .mailtemplateOnderwerp(mailtemplate)
      .emptyToolbar()
      .maxlength(100)
      .build();
    const body = new HtmlEditorFormFieldBuilder()
      .id("body")
      .label("body")
      .validators(Validators.required)
      .mailtemplateBody(mailtemplate)
      .build();
    const bijlagen = new DocumentenLijstFieldBuilder()
      .id("bijlagen")
      .label("bijlagen")
      .documenten(documenten)
      .build();

    if (
      this.zaak.initiatorIdentificatieType &&
      this.zaak.initiatorIdentificatie
    ) {
      this.klantenService
        .ophalenContactGegevens(this.zaak.initiatorIdentificatie)
        .subscribe((gegevens) => {
          if (gegevens.emailadres) {
            const initiatorToevoegenIcon = new ActionIcon(
              "person",
              "actie.initiator.email.toevoegen",
              new Subject<void>(),
            );
            if (Array.isArray(ontvanger.icons)) {
              ontvanger.icons.push(initiatorToevoegenIcon);
            } else {
              ontvanger.icons = [initiatorToevoegenIcon];
            }
            initiatorToevoegenIcon.iconClicked.subscribe(() => {
              ontvanger.value(gegevens.emailadres);
            });
          }
        });
    }

    this.fields = [[verzender], [ontvanger], [onderwerp], [body], [bijlagen]];
  }

  onFormSubmit(formGroup: FormGroup): void {
    if (formGroup) {
      const mailGegevens: GeneratedType<"RESTMailGegevens"> = {
        verzender: formGroup.controls["verzender"].value.mail,
        replyTo: formGroup.controls["verzender"].value.replyTo,
        ontvanger: formGroup.controls["ontvanger"].value,
        onderwerp: formGroup.controls["onderwerp"].value,
        body: formGroup.controls["body"].value,
        bijlagen: formGroup.controls["bijlagen"].value,
        createDocumentFromMail: true,
      };

      this.mailService
        .sendAcknowledgeReceipt(this.zaak.uuid, mailGegevens)
        .subscribe(() => {
          this.utilService.openSnackbar("msg.email.verstuurd");
          this.ontvangstBevestigd.emit(true);
        });
    } else {
      this.ontvangstBevestigd.emit(false);
    }
  }
}

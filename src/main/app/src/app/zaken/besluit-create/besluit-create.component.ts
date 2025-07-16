/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  ViewChild,
} from "@angular/core";
import { FormGroup, Validators } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import { TranslateService } from "@ngx-translate/core";
import moment from "moment";
import { Subject, Subscription } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { DividerFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/divider/divider-form-field-builder";
import { MessageFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/message/message-form-field-builder";
import { MessageLevel } from "src/app/shared/material-form-builder/form-components/message/message-level.enum";
import { ParagraphFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/paragraph/paragraph-form-field-builder";
import { FormComponent } from "src/app/shared/material-form-builder/form/form/form.component";
import { UtilService } from "../../core/service/util.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { DateFormFieldBuilder } from "../../shared/material-form-builder/form-components/date/date-form-field-builder";
import { DocumentenLijstFieldBuilder } from "../../shared/material-form-builder/form-components/documenten-lijst/documenten-lijst-field-builder";
import { SelectFormFieldBuilder } from "../../shared/material-form-builder/form-components/select/select-form-field-builder";
import { TextareaFormFieldBuilder } from "../../shared/material-form-builder/form-components/textarea/textarea-form-field-builder";
import { AbstractFormField } from "../../shared/material-form-builder/model/abstract-form-field";
import { FormConfigBuilder } from "../../shared/material-form-builder/model/form-config-builder";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-besluit-create",
  templateUrl: "./besluit-create.component.html",
  styleUrls: ["./besluit-create.component.less"],
})
export class BesluitCreateComponent implements OnInit, OnDestroy {
  formConfig = new FormConfigBuilder()
    .saveText("actie.aanmaken")
    .cancelText("actie.annuleren")
    .build();
  @Input({ required: true }) zaak!: GeneratedType<"RestZaak">;
  @Input({ required: true }) sideNav!: MatDrawer;
  @Output() besluitVastgelegd = new EventEmitter<boolean>();
  @ViewChild(FormComponent) formComponent!: FormComponent;

  fields: Array<AbstractFormField[]> = [];

  private subscription?: Subscription;
  private ngDestroy = new Subject<void>();

  constructor(
    private readonly zakenService: ZakenService,
    private readonly utilService: UtilService,
    private readonly informatieObjectenService: InformatieObjectenService,
    private readonly translate: TranslateService,
  ) {}

  ngOnInit() {
    const resultaattypeField = new SelectFormFieldBuilder(
      this.zaak.resultaat?.resultaattype,
    )
      .id("resultaattype")
      .label("resultaat")
      .validators(Validators.required)
      .optionLabel("naam")
      .options(this.zakenService.listResultaattypes(this.zaak.zaaktype.uuid))
      .build();
    const besluittypeField = new SelectFormFieldBuilder<
      GeneratedType<"RestDecisionType">
    >()
      .id("besluittype")
      .label("besluit")
      .validators(Validators.required)
      .optionLabel("naam")
      .options(this.zakenService.listBesluittypes(this.zaak.zaaktype.uuid))
      .build();
    const toelichtingField = new TextareaFormFieldBuilder()
      .id("toelichting")
      .label("toelichting")
      .maxlength(1000)
      .build();
    const ingangsdatumField = new DateFormFieldBuilder(moment())
      .id("ingangsdatum")
      .label("ingangsdatum")
      .validators(Validators.required)
      .build();
    const vervaldatumField = new DateFormFieldBuilder()
      .id("vervaldatum")
      .label("vervaldatum")
      .minDate(new Date(String(ingangsdatumField.formControl.value)))
      .build();
    const documentenField = new DocumentenLijstFieldBuilder()
      .id("documenten")
      .label("documenten")
      .build();

    this.fields = [
      [resultaattypeField],
      [besluittypeField],
      [ingangsdatumField],
      [vervaldatumField],
      [toelichtingField],
      [documentenField],
    ];

    resultaattypeField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        vervaldatumField.required = value?.vervaldatumBesluitVerplicht ?? false;
      });

    ingangsdatumField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        if (!value) return;
        vervaldatumField.minDate = new Date(String(value));
      });

    besluittypeField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        if (!value) return;

        documentenField.updateDocumenten(
          this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
            zaakUUID: this.zaak.uuid,
            besluittypeUUID: value.id,
          }),
        );

        this.updatePublicationsFormPart(value);
      });
  }

  updatePublicationsFormPart({
    publication,
  }: GeneratedType<"RestDecisionType">): void {
    this.fields = this.fields.filter((fieldGroup) =>
      fieldGroup.every(
        (group) =>
          ![
            "divider",
            "publicationParagraph",
            "publicationDate",
            "messageField",
            "lastResponseDate",
          ].includes(group.id),
      ),
    );

    this.subscription?.unsubscribe();

    if (publication.enabled) {
      const divider = new DividerFormFieldBuilder().id("divider").build();

      const publicationParagraph = new ParagraphFormFieldBuilder()
        .id("publicationParagraph")
        .text(`besluit.publicatie.indicatie.koptitel`)
        .build();

      const publicationDateField = new DateFormFieldBuilder(moment())
        .id("publicationDate")
        .label("publicatiedatum")
        .build();

      const publicationMessageField = new MessageFormFieldBuilder()
        .id("messageField")
        .text(
          this.translate.instant(
            `besluit.publicatie.indicatie.onderschrift${(publication.publicationTermDays ?? 0) > 1 ? ".meervoud" : ""}`,
            {
              publicationTermDays: publication.publicationTermDays,
            },
          ),
        )
        .level(MessageLevel.INFO)
        .build();

      const lastResponseDate = moment().add(
        (
          this.getFormField("besluittype")?.formControl.value as
            | GeneratedType<"RestDecisionType">
            | undefined
        )?.publication.responseTermDays,
        "days",
      );

      const lastResponseDateField = new DateFormFieldBuilder(lastResponseDate)
        .id("lastResponseDate")
        .label("uiterlijkereactiedatum")
        .minDate(lastResponseDate.toDate())
        .build();

      this.fields.push(
        [divider],
        [publicationParagraph],
        [publicationDateField],
        [publicationMessageField],
        [lastResponseDateField],
      );

      this.subscription = publicationDateField.formControl.valueChanges
        .pipe(takeUntil(this.ngDestroy))
        .subscribe((value) => {
          if (!value) return;
          const adjustedLastResponseDate = moment(value)
            .clone()
            .add(
              (
                this.getFormField("besluittype")?.formControl.value as
                  | GeneratedType<"RestDecisionType">
                  | undefined
              )?.publication.responseTermDays,
              "days",
            );

          lastResponseDateField.formControl.setValue(adjustedLastResponseDate);
          lastResponseDateField.minDate = adjustedLastResponseDate.toDate();
        });
    }

    this.formComponent.refreshFormfields(this.fields);
  }

  private getFormField(id: string) {
    const field = this.fields.find((fields) =>
      fields.find((group) => group.id === id),
    );

    return field?.[0];
  }

  onFormSubmit(formGroup: FormGroup) {
    if (formGroup) {
      const data: GeneratedType<"RestDecisionCreateData"> = {
        zaakUuid: this.zaak.uuid,
        resultaattypeUuid: (
          formGroup.controls["resultaattype"]
            .value as GeneratedType<"RestResultaattype">
        ).id,
        besluittypeUuid: (
          formGroup.controls["besluittype"]
            .value as GeneratedType<"RestDecisionType">
        ).id,
        ...(formGroup.controls["besluittype"].value.publication.enabled
          ? {
              publicationDate: formGroup.controls["publicationDate"].value,
              lastResponseDate: formGroup.controls["lastResponseDate"].value,
            }
          : {}),
        toelichting: formGroup.controls["toelichting"].value,
        ingangsdatum: formGroup.controls["ingangsdatum"].value,
        vervaldatum: formGroup.controls["vervaldatum"].value,
        informatieobjecten: formGroup.controls["documenten"].value
          ? formGroup.controls["documenten"].value.split(";")
          : [],
      };

      this.zakenService.createBesluit(data).subscribe(() => {
        this.utilService.openSnackbar("msg.besluit.vastgelegd");
        this.besluitVastgelegd.emit(true);
      });
    } else {
      this.besluitVastgelegd.emit(false);
    }
  }

  ngOnDestroy() {
    this.ngDestroy.next();
    this.ngDestroy.complete();
  }
}

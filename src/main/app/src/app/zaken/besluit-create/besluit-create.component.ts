/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
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
import moment, { Moment } from "moment";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { DividerFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/divider/divider-form-field-builder";
import { ParagraphFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/paragraph/paragraph-form-field-builder";
import { FormComponent } from "src/app/shared/material-form-builder/form/form/form.component";
import { UtilService } from "../../core/service/util.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { InformatieobjectZoekParameters } from "../../informatie-objecten/model/informatieobject-zoek-parameters";
import { DateFormField } from "../../shared/material-form-builder/form-components/date/date-form-field";
import { DateFormFieldBuilder } from "../../shared/material-form-builder/form-components/date/date-form-field-builder";
import { DocumentenLijstFieldBuilder } from "../../shared/material-form-builder/form-components/documenten-lijst/documenten-lijst-field-builder";
import { SelectFormFieldBuilder } from "../../shared/material-form-builder/form-components/select/select-form-field-builder";
import { TextareaFormFieldBuilder } from "../../shared/material-form-builder/form-components/textarea/textarea-form-field-builder";
import { AbstractFormField } from "../../shared/material-form-builder/model/abstract-form-field";
import { FormConfig } from "../../shared/material-form-builder/model/form-config";
import { FormConfigBuilder } from "../../shared/material-form-builder/model/form-config-builder";
import { GeneratedType } from "../../shared/utils/generated-types";
import { Zaak } from "../model/zaak";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-besluit-create",
  templateUrl: "./besluit-create.component.html",
  styleUrls: ["./besluit-create.component.less"],
})
export class BesluitCreateComponent implements OnInit, OnDestroy {
  formConfig: FormConfig;
  @Input() zaak: Zaak;
  @Input() sideNav: MatDrawer;
  @Output() besluitVastgelegd = new EventEmitter<boolean>();
  @ViewChild(FormComponent) formComponent!: FormComponent;

  fields: Array<AbstractFormField[]>;

  besluittypeField: AbstractFormField;
  divider: AbstractFormField;
  publicationParagraph: AbstractFormField;
  publicationDateField: AbstractFormField;
  lastResponseDateField: AbstractFormField;

  private ngDestroy = new Subject<void>();

  constructor(
    private zakenService: ZakenService,
    public utilService: UtilService,
    private informatieObjectenService: InformatieObjectenService,
    protected translate: TranslateService,
  ) {}

  ngOnInit(): void {
    this.formConfig = new FormConfigBuilder()
      .saveText("actie.aanmaken")
      .cancelText("actie.annuleren")
      .build();

    const resultaattypeField = new SelectFormFieldBuilder(
      this.zaak.resultaat?.resultaattype,
    )
      .id("resultaattype")
      .label("resultaat")
      .validators(Validators.required)
      .optionLabel("naam")
      .options(this.zakenService.listResultaattypes(this.zaak.zaaktype.uuid))
      .build();
    this.besluittypeField = new SelectFormFieldBuilder()
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
      .minDate(ingangsdatumField.formControl.value)
      .build();
    const documentenField = new DocumentenLijstFieldBuilder()
      .id("documenten")
      .label("documenten")
      .build();

    this.fields = [
      [resultaattypeField],
      [this.besluittypeField],
      [ingangsdatumField],
      [vervaldatumField],
      [toelichtingField],
      [documentenField],
    ];

    resultaattypeField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        if (value) {
          vervaldatumField.required = (
            value as GeneratedType<"RestResultaattype">
          ).vervaldatumBesluitVerplicht;
        }
      });

    ingangsdatumField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        (vervaldatumField as DateFormField).minDate = value;
      });

    this.besluittypeField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        if (value) {
          const zoekparameters = new InformatieobjectZoekParameters();
          zoekparameters.zaakUUID = this.zaak.uuid;
          zoekparameters.besluittypeUUID = value.id;
          documentenField.updateDocumenten(
            this.informatieObjectenService.listEnkelvoudigInformatieobjecten(
              zoekparameters,
            ),
          );

          this.updatePublicationsFormPart(value);
        }
      });
  }

  updatePublicationsFormPart({
    publication,
  }: GeneratedType<"RestBesluittype">): void {
    if (!publication.enabled) return;

    this.fields = this.fields.filter(
      (fieldGroup) =>
        !fieldGroup.includes(this.divider) &&
        !fieldGroup.includes(this.publicationParagraph) &&
        !fieldGroup.includes(this.publicationDateField),
    );

    this.divider = new DividerFormFieldBuilder().id("divider").build();

    this.publicationParagraph = new ParagraphFormFieldBuilder()
      .text(
        this.translate.instant(`besluit.publicatie.indicatie`, {
          publicationTermDays: publication.publicationTermDays,
          responseTermDays: publication.responseTermDays,
        }),
      )
      .build();

    this.publicationDateField = new DateFormFieldBuilder(moment())
      .id("publicationDate")
      .label("publicatiedatum")
      .build();

    const lastResponseDate: Moment = moment().add(
      this.besluittypeField.formControl.value.publication.responseTermDays,
      "days",
    );

    this.lastResponseDateField = new DateFormFieldBuilder(lastResponseDate)
      .id("lastResponseDate")
      .label("uiterlijkereactiedatum")
      .minDate(lastResponseDate.toDate())
      .build();

    this.formComponent.refreshFormfields(this.fields);

    this.fields.push(
      [this.divider],
      [this.publicationParagraph],
      [this.publicationDateField],
      [this.lastResponseDateField],
    );

    this.publicationDateField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value: Moment | null) => {
        if (value) {
          const adjustedLastResponseDate: Moment = value
            .clone()
            .add(
              this.besluittypeField.formControl.value.publication
                .responseTermDays,
              "days",
            );

          this.lastResponseDateField.formControl.setValue(
            adjustedLastResponseDate,
          );
          (this.lastResponseDateField as DateFormField).minDate =
            adjustedLastResponseDate.toDate();
        }
      });
  }

  onFormSubmit(formGroup: FormGroup): void {
    if (formGroup) {
      const gegevens: GeneratedType<"RestBesluitVastleggenGegevens"> = {
        zaakUuid: this.zaak.uuid,
        resultaattypeUuid: (
          formGroup.controls["resultaattype"]
            .value as GeneratedType<"RestResultaattype">
        ).id,
        besluittypeUuid: (
          formGroup.controls["besluittype"]
            .value as GeneratedType<"RestBesluittype">
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

      this.zakenService.createBesluit(gegevens).subscribe(() => {
        this.utilService.openSnackbar("msg.besluit.vastgelegd");
        this.besluitVastgelegd.emit(true);
      });
    } else {
      this.besluitVastgelegd.emit(false);
    }
  }

  ngOnDestroy(): void {
    this.ngDestroy.next();
    this.ngDestroy.complete();
  }
}

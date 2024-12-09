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
import moment, { Moment } from "moment";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
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
import { FormComponent } from "src/app/shared/material-form-builder/form/form/form.component";
import { DividerFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/divider/divider-form-field-builder";
import { ParagraphFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/paragraph/paragraph-form-field-builder";
import { TranslateService } from "@ngx-translate/core";

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

  besluitTypeField: AbstractFormField;
  divider: AbstractFormField;
  publicationParagraph: AbstractFormField;
  publicationDateField: AbstractFormField;
  publicationReactionDateField: AbstractFormField;

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

    const resultaatTypeField = new SelectFormFieldBuilder(
      this.zaak.resultaat?.resultaattype,
    )
      .id("resultaattype")
      .label("resultaat")
      .validators(Validators.required)
      .optionLabel("naam")
      .options(this.zakenService.listResultaattypes(this.zaak.zaaktype.uuid))
      .build();
    this.besluitTypeField = new SelectFormFieldBuilder()
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
    const ingangsDatumField = new DateFormFieldBuilder(moment())
      .id("ingangsdatum")
      .label("ingangsdatum")
      .validators(Validators.required)
      .build();
    const vervaldatumField = new DateFormFieldBuilder()
      .id("vervaldatum")
      .label("vervaldatum")
      .minDate(ingangsDatumField.formControl.value)
      .build();
    const documentenField = new DocumentenLijstFieldBuilder()
      .id("documenten")
      .label("documenten")
      .build();

    this.fields = [
      [resultaatTypeField],
      [this.besluitTypeField],
      [ingangsDatumField],
      [vervaldatumField],
      [toelichtingField],
      [documentenField],
    ];

    resultaatTypeField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        if (value) {
          vervaldatumField.required = (
            value as GeneratedType<"RestResultaattype">
          ).vervaldatumBesluitVerplicht;
        }
      });

    this.besluitTypeField.formControl.valueChanges
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

    ingangsDatumField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        (vervaldatumField as DateFormField).minDate = value;
      });
  }

  updatePublicationsFormPart({ publicatieIndicatie }): void {
    if (!publicatieIndicatie.active) return;

    this.fields = this.fields.filter(
      (fieldGroup) =>
        !fieldGroup.includes(this.divider) &&
        !fieldGroup.includes(this.publicationParagraph) &&
        !fieldGroup.includes(this.publicationDateField),
    );

    this.divider = new DividerFormFieldBuilder().id("divider").build();

    this.publicationParagraph = new ParagraphFormFieldBuilder()
      .text(
        this.translate.instant(
          `besluit.publicatie.indicatie ${publicatieIndicatie.publicationTerm} ${publicatieIndicatie.responseTerm}`,
          {
            publicationTerm: publicatieIndicatie.publicationTerm,
            responseTerm: publicatieIndicatie.responseTerm,
          },
        ),
      )
      .build();

    this.publicationDateField = new DateFormFieldBuilder(moment())
      .id("publicationDate")
      .label("publicatiedatum")
      .build();

    this.fields.push(
      [this.divider],
      [this.publicationParagraph],
      [this.publicationDateField],
    );

    this.updateLastResponseDateField(moment());

    this.publicationDateField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        if (value) {
          this.updateLastResponseDateField(value);
        }
      });
  }

  updateLastResponseDateField(publicationDate: Moment): void {
    this.fields = this.fields.filter(
      (fieldGroup) => !fieldGroup.includes(this.publicationReactionDateField),
    );

    const lastResponseDate: Moment = publicationDate
      .clone()
      .add(
        this.besluitTypeField.formControl.value.publicatieIndicatie
          .responseTerm,
        "days",
      );

    this.publicationReactionDateField = new DateFormFieldBuilder(
      lastResponseDate,
    )
      .id("lastResponseDate")
      .label("uiterlijkereactiedatum")
      .minDate(lastResponseDate.toDate())
      .build();

    this.fields.push([this.publicationReactionDateField]);

    this.formComponent.refreshFormfields(this.fields);
  }

  onFormSubmit(formGroup: FormGroup): void {
    if (formGroup) {
      const gegevens: GeneratedType<"RestBesluitVastleggenGegevens"> = {
        zaakUuid: this.zaak.uuid,
        resultaattypeUuid: formGroup.controls["resultaattype"].value.id,
        toelichting: formGroup.controls["toelichting"].value,
        ingangsdatum: formGroup.controls["ingangsdatum"].value,
        vervaldatum: formGroup.controls["vervaldatum"].value,
        informatieobjecten: formGroup.controls["documenten"].value
          ? formGroup.controls["documenten"].value.split(";")
          : [],
        besluittypeUuid: formGroup.controls["besluittype"].value.id,
        ...(formGroup.controls["besluittype"].value.publicatieIndicatie.active
          ? {
              publicationDate: formGroup.controls["publicationDate"].value,
              lastResponseDate: formGroup.controls["lastResponseDate"].value,
            }
          : {}),
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

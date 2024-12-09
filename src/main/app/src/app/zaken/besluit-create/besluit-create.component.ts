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

          this.handleUpdatedBesluitType(value);
        }
      });

    ingangsDatumField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        (vervaldatumField as DateFormField).minDate = value;
      });
  }

  handleUpdatedBesluitType({ publicatieIndicatie }): void {
    console.log(publicatieIndicatie);

    if (!publicatieIndicatie.active) return;

    this.fields = this.fields.filter(
      (fieldGroup) =>
        !fieldGroup.includes(this.divider) &&
        !fieldGroup.includes(this.publicationParagraph) &&
        !fieldGroup.includes(this.publicationDateField) &&
        !fieldGroup.includes(this.publicationReactionDateField),
    );

    this.divider = new DividerFormFieldBuilder().id("divider").build();

    this.publicationParagraph = new ParagraphFormFieldBuilder()
      .text(
        this.translate.instant(
          `besluit.publicatie.indicatie ${publicatieIndicatie.publicatietermijn} ${publicatieIndicatie.reactietermijn}`,
          {
            publicatietermijn: publicatieIndicatie.publicatietermijn,
            reactietermijn: publicatieIndicatie.reactietermijn,
          },
        ),
      )
      .build();

    this.publicationDateField = new DateFormFieldBuilder(moment())
      .id("publicatiedatum")
      .label("publicatiedatum")
      .build();

    this.publicationReactionDateField = new DateFormFieldBuilder(
      moment().add(publicatieIndicatie.reactietermijn, "days"),
    )
      .id("uiterlijkereactiedatum")
      .label("uiterlijkereactiedatum")
      .minDate(
        moment().add(publicatieIndicatie.reactietermijn, "days").toDate(),
      )
      .build();

    this.fields.push(
      [this.divider],
      [this.publicationParagraph],
      [this.publicationDateField],
      [this.publicationReactionDateField],
    );

    this.formComponent.refreshFormfields(this.fields);

    this.publicationDateField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        if (value) {
          console.log("moment entered: ", moment.toLocaleString());
          this.updatePublicationReactionDateField(value);
        }
      });
  }

  updatePublicationReactionDateField(publicationDate: Moment): void {
    this.fields = this.fields.filter(
      (fieldGroup) => !fieldGroup.includes(this.publicationReactionDateField),
    );

    const reactionDate: Moment = publicationDate
      .clone()
      .add(
        this.besluitTypeField.formControl.value.publicatieIndicatie
          .reactietermijn,
        "days",
      );

    this.publicationReactionDateField = new DateFormFieldBuilder(reactionDate)
      .id("uiterlijkereactiedatum")
      .label("uiterlijkereactiedatum")
      .minDate(reactionDate.toDate())
      .build();

    this.fields.push([this.publicationReactionDateField]);

    this.formComponent.refreshFormfields(this.fields);
  }

  onFormSubmit(formGroup: FormGroup): void {
    if (formGroup) {
      const gegevens: GeneratedType<"RestBesluitVastleggenGegevens"> = {
        zaakUuid: this.zaak.uuid,
        resultaattypeUuid: formGroup.controls["resultaattype"].value.id,
        besluittypeUuid: formGroup.controls["besluittype"].value.id,
        toelichting: formGroup.controls["toelichting"].value,
        ingangsdatum: formGroup.controls["ingangsdatum"].value,
        vervaldatum: formGroup.controls["vervaldatum"].value,
        informatieobjecten: formGroup.controls["documenten"].value
          ? formGroup.controls["documenten"].value.split(";")
          : [],
      };

      console.log(
        "formGroup.controls[besluittype].value.publicatieIndicatie.actief: ",
        formGroup.controls["besluittype"].value.publicatieIndicatie.actief,
        {
          ...(formGroup.controls["besluittype"].value.publicatieIndicatie.actief
            ? {
                publicatie: {
                  publicatiedatum: formGroup.controls["publicatiedatum"],
                  uiterlijkereactiedatum:
                    formGroup.controls["uiterlijkereactiedatum"],
                },
              }
            : {}),
        },
      );

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

/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild,
} from "@angular/core";
import { FormGroup, Validators } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import { TranslateService } from "@ngx-translate/core";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { ParagraphFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/paragraph/paragraph-form-field-builder";
import { UtilService } from "../../core/service/util.service";
import { DateFormFieldBuilder } from "../../shared/material-form-builder/form-components/date/date-form-field-builder";
import { DocumentenLijstFieldBuilder } from "../../shared/material-form-builder/form-components/documenten-lijst/documenten-lijst-field-builder";
import { DocumentenLijstFormField } from "../../shared/material-form-builder/form-components/documenten-lijst/documenten-lijst-form-field";
import { TextareaFormFieldBuilder } from "../../shared/material-form-builder/form-components/textarea/textarea-form-field-builder";
import { FormComponent } from "../../shared/material-form-builder/form/form/form.component";
import { AbstractFormField } from "../../shared/material-form-builder/model/abstract-form-field";
import { FormConfig } from "../../shared/material-form-builder/model/form-config";
import { FormConfigBuilder } from "../../shared/material-form-builder/model/form-config-builder";
import { Zaak } from "../../zaken/model/zaak";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { DocumentVerzendGegevens } from "../model/document-verzend-gegevens";

@Component({
  selector: "zac-informatie-verzenden",
  templateUrl: "./informatie-object-verzenden.component.html",
  styleUrls: ["./informatie-object-verzenden.component.less"],
})
export class InformatieObjectVerzendenComponent
  implements OnInit, OnChanges, OnDestroy
{
  @Input() zaak: Zaak;
  @Input() sideNav: MatDrawer;
  @Output() documentSent = new EventEmitter<void>();

  @ViewChild(FormComponent) form: FormComponent;

  fields: Array<AbstractFormField[]>;
  formConfig: FormConfig;
  private documentSelectFormField: DocumentenLijstFormField;
  private destroy$ = new Subject<void>(); // Subject for handling unsubscription

  constructor(
    private translate: TranslateService,
    private informatieObjectenService: InformatieObjectenService,
    public utilService: UtilService,
  ) {}

  ngOnInit(): void {
    this.formConfig = new FormConfigBuilder()
      .saveText("actie.verzenden")
      .cancelText("actie.annuleren")
      .build();

    this.documentSelectFormField = new DocumentenLijstFieldBuilder()
      .id("documenten")
      .label("documenten")
      .removeColumn("status")
      .validators(Validators.required)
      .documenten(
        this.informatieObjectenService.listInformatieobjectenVoorVerzenden(
          this.zaak.uuid,
        ),
      )
      .build();

    const paragraph = new ParagraphFormFieldBuilder()
      .text(this.translate.instant("msg.document.verzenden.post.uitleg"))
      .build();

    const verzendDatum = new DateFormFieldBuilder(new Date())
      .id("verzenddatum")
      .validators(Validators.required)
      .label("verzenddatum")
      .build();

    const toelichtingField = new TextareaFormFieldBuilder()
      .id("toelichting")
      .label("toelichting")
      .validators(Validators.required)
      .maxlength(1000)
      .build();

    this.fields = [
      [paragraph],
      [this.documentSelectFormField],
      [verzendDatum],
      [toelichtingField],
    ];
  }

  onFormSubmit(formGroup: FormGroup): void {
    if (formGroup) {
      const gegevens = new DocumentVerzendGegevens();
      gegevens.verzenddatum = formGroup.controls["verzenddatum"].value;
      gegevens.informatieobjecten = formGroup.controls["documenten"].value
        ? formGroup.controls["documenten"].value.split(";")
        : [];
      gegevens.zaakUuid = this.zaak.uuid;
      gegevens.toelichting = formGroup.controls["toelichting"].value;

      this.informatieObjectenService
        .verzenden(gegevens)
        .pipe(takeUntil(this.destroy$))
        .subscribe(() => {
          this.utilService.openSnackbar(
            gegevens.informatieobjecten.length > 1
              ? "msg.documenten.verzenden.uitgevoerd"
              : "msg.document.verzenden.uitgevoerd",
          );
          this.documentSent.emit();
          //
          // On the above emit, the parent closes (and destroys) the sidebar and so this form.
          // The form gets reloaded/remounted again upon opening the sidebar, and so having this form in a nice pristine state.
          // Explicitly resetting the form is not needed.
        });
    } else {
      this.sideNav.close();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.zaak.previousValue) {
      this.documentSelectFormField.updateDocumenten(
        this.informatieObjectenService.listInformatieobjectenVoorVerzenden(
          this.zaak.uuid,
        ),
      );
    }
  }

  ngOnDestroy(): void {
    // Trigger completion of all subscriptions
    this.destroy$.next();
    this.destroy$.complete();
  }
}

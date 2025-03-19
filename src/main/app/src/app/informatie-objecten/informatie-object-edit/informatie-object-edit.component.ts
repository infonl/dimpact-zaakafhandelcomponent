/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024-2025 Lifely
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
import { Subscription, tap } from "rxjs";
import { FileInputFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/file-input/file-input-form-field-builder";
import { ParagraphFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/paragraph/paragraph-form-field-builder";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "src/app/shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { DateFormFieldBuilder } from "../../shared/material-form-builder/form-components/date/date-form-field-builder";
import { InputFormFieldBuilder } from "../../shared/material-form-builder/form-components/input/input-form-field-builder";
import { SelectFormFieldBuilder } from "../../shared/material-form-builder/form-components/select/select-form-field-builder";
import { FormComponent } from "../../shared/material-form-builder/form/form/form.component";
import { AbstractFormField } from "../../shared/material-form-builder/model/abstract-form-field";
import { FormConfig } from "../../shared/material-form-builder/model/form-config";
import { FormConfigBuilder } from "../../shared/material-form-builder/model/form-config-builder";
import { OrderUtil } from "../../shared/order/order-util";
import { GeneratedType } from "../../shared/utils/generated-types";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { InformatieobjectStatus } from "../model/informatieobject-status.enum";
import { Vertrouwelijkheidaanduiding } from "../model/vertrouwelijkheidaanduiding.enum";

@Component({
  selector: "zac-informatie-object-edit",
  templateUrl: "./informatie-object-edit.component.html",
  styleUrls: ["./informatie-object-edit.component.less"],
})
export class InformatieObjectEditComponent
  implements OnInit, OnDestroy, OnChanges
{
  @Input()
  infoObject!: GeneratedType<"RestEnkelvoudigInformatieObjectVersieGegevens">;
  @Input() sideNav!: MatDrawer;
  @Input() zaakUuid?: string;
  @Output() document = new EventEmitter<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >();

  @ViewChild(FormComponent) form!: FormComponent;

  fields: Array<AbstractFormField[]> = [];
  formConfig!: FormConfig;
  private ingelogdeMedewerker!: GeneratedType<"RestLoggedInUser">;

  private subscriptions$: Subscription[] = [];

  constructor(
    private informatieObjectenService: InformatieObjectenService,
    public utilService: UtilService,
    private configuratieService: ConfiguratieService,
    private translateService: TranslateService,
    private identityService: IdentityService,
    private vertrouwelijkaanduidingToTranslationKeyPipe: VertrouwelijkaanduidingToTranslationKeyPipe,
  ) {}

  ngOnInit() {
    this.formConfig = new FormConfigBuilder()
      .saveText("actie.toevoegen")
      .cancelText("actie.annuleren")
      .requireUserChanges()
      .build();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes.infoObject.currentValue) {
      return;
    }

    this.getIngelogdeMedewerker();
    this.initializeFormFields();
  }

  private initializeFormFields() {
    const vertrouwelijkheidsAanduidingen = this.utilService.getEnumAsSelectList(
      "vertrouwelijkheidaanduiding",
      Vertrouwelijkheidaanduiding,
    );
    const informatieobjectStatussen =
      this.utilService.getEnumAsSelectListExceptFor(
        "informatieobject.status",
        InformatieobjectStatus,
        [InformatieobjectStatus.GEARCHIVEERD],
      );

    const inhoudField = new FileInputFormFieldBuilder()
      .id("bestand")
      .label("bestandsnaam")
      .maxFileSizeMB(this.configuratieService.readMaxFileSizeMB())
      .additionalAllowedFileTypes(
        this.configuratieService.readAdditionalAllowedFileTypes(),
      )
      .build();

    const titel = new InputFormFieldBuilder(this.infoObject.titel)
      .id("titel")
      .label("titel")
      .validators(Validators.required)
      .maxlength(100)
      .build();

    const beschrijving = new InputFormFieldBuilder(this.infoObject.beschrijving)
      .id("beschrijving")
      .label("beschrijving")
      .maxlength(100)
      .build();

    const taal = new SelectFormFieldBuilder(this.infoObject.taal)
      .id("taal")
      .label("taal")
      .optionLabel("naam")
      .options(this.configuratieService.listTalen())
      .validators(Validators.required)
      .build();

    const status = new SelectFormFieldBuilder(
      this.infoObject.status
        ? {
            label: this.translateService.instant(
              "informatieobject.status." + this.infoObject.status,
            ),
            value: this.infoObject.status,
          }
        : null,
    )
      .id("status")
      .label("status")
      .validators(Validators.required)
      .optionLabel("label")
      .options(informatieobjectStatussen)
      .build();

    const verzenddatum = new DateFormFieldBuilder(this.infoObject.verzenddatum)
      .id("verzenddatum")
      .label("verzenddatum")
      .build();

    const ontvangstDatum = new DateFormFieldBuilder(
      this.infoObject.ontvangstdatum,
    )
      .id("ontvangstdatum")
      .label("ontvangstdatum")
      .hint("msg.document.ontvangstdatum.hint")
      .build();

    const types = this.informatieObjectenService
      .listInformatieobjecttypesForZaak(this.zaakUuid)
      .pipe(
        tap((x) => {
          informatieobjectType.formControl.setValue(
            x.find((y) => y.uuid === this.infoObject.informatieobjectTypeUUID),
          );
        }),
      );

    const informatieobjectType = new SelectFormFieldBuilder()
      .id("informatieobjectTypeUUID")
      .label("informatieobjectType")
      .options(types)
      .optionLabel("omschrijving")
      .validators(Validators.required)
      .settings({ translateLabels: false, capitalizeFirstLetter: true })
      .build();

    const auteur = new InputFormFieldBuilder(this.ingelogdeMedewerker.naam)
      .id("auteur")
      .label("auteur")
      .validators(Validators.required)
      .build();

    const vertrouwelijk = new SelectFormFieldBuilder({
      label: this.translateService.instant(
        this.vertrouwelijkaanduidingToTranslationKeyPipe.transform(
          this.infoObject.vertrouwelijkheidaanduiding ?? "",
        ),
      ),
      value: this.infoObject.vertrouwelijkheidaanduiding,
    })
      .id("vertrouwelijkheidaanduiding")
      .label("vertrouwelijkheidaanduiding")
      .optionLabel("label")
      .options(vertrouwelijkheidsAanduidingen)
      .optionsOrder(OrderUtil.orderAsIs())
      .validators(Validators.required)
      .build();

    const toelichting = new InputFormFieldBuilder()
      .id("toelichting")
      .label("toelichting")
      .maxlength(200)
      .build();

    this.subscriptions$.push(
      inhoudField.formControl.valueChanges.subscribe((file: File) => {
        titel.formControl.setValue(file?.name?.replace(/\.[^/.]+$/, "") || "");
        titel.formControl.markAsDirty();
      }),
    );

    const emptyField = new ParagraphFormFieldBuilder().text("").build();

    this.fields = [
      [inhoudField],
      [titel],
      [beschrijving],
      [informatieobjectType, vertrouwelijk],
      [status, emptyField],
      [auteur, taal],
      [ontvangstDatum, verzenddatum],
      [toelichting],
    ];

    this.subscriptions$.push(
      ontvangstDatum.formControl.valueChanges.subscribe((value) => {
        if (value && verzenddatum.formControl.enabled) {
          status.formControl.setValue(
            informatieobjectStatussen.find(
              (option) =>
                option.value ===
                this.utilService.getEnumKeyByValue(
                  InformatieobjectStatus,
                  InformatieobjectStatus.DEFINITIEF,
                ),
            ),
          );
          status.formControl.disable();
          verzenddatum.formControl.disable();
        } else if (!value && verzenddatum.formControl.disabled) {
          status.formControl.enable();
          verzenddatum.formControl.enable();
        }
      }),
    );

    this.subscriptions$.push(
      verzenddatum.formControl.valueChanges.subscribe((value) => {
        if (value && ontvangstDatum.formControl.enabled) {
          ontvangstDatum.formControl.disable();
        } else if (!value && ontvangstDatum.formControl.disabled) {
          ontvangstDatum.formControl.enable();
        }
      }),
    );

    if (ontvangstDatum.formControl.value) {
      verzenddatum.formControl.disable();
      status.formControl.disable();
    }
    if (verzenddatum.formControl.value) {
      ontvangstDatum.formControl.disable();
    }
  }

  ngOnDestroy() {
    for (const subscription of this.subscriptions$) {
      subscription.unsubscribe();
    }
  }

  onFormSubmit(formGroup: FormGroup): void {
    if (!formGroup) {
      this.sideNav.close();
      return;
    }
    const nieuweVersie: Partial<
      GeneratedType<"RestEnkelvoudigInformatieObjectVersieGegevens">
    > = {
      uuid: this.infoObject.uuid,
    };

    Object.keys(formGroup.controls).forEach((key) => {
      const control = formGroup.controls[key];
      const value = control.value;

      switch (key) {
        case "status":
          nieuweVersie[key] = InformatieobjectStatus[value.value.toUpperCase()];
          break;
        case "vertrouwelijkheidaanduiding":
          nieuweVersie[key] = value.value;
          break;
        case "bestand":
          if (value) {
            nieuweVersie["bestandsnaam"] = value.name;
            nieuweVersie["file"] = value;
            nieuweVersie["formaat"] = value.type;
          }
          break;
        case "informatieobjectTypeUUID":
          nieuweVersie[key] = value.uuid;
          break;
        default:
          nieuweVersie[key] = value;
          break;
      }
    });

    this.informatieObjectenService
      .updateEnkelvoudigInformatieobject(
        nieuweVersie.uuid!,
        this.zaakUuid!,
        nieuweVersie as GeneratedType<"RestEnkelvoudigInformatieObjectVersieGegevens">,
      )
      .subscribe((document) => {
        this.document.emit(document);
        this.utilService.openSnackbar("msg.document.nieuwe.versie.toegevoegd");
        this.ngOnInit();
        this.sideNav.close();
        this.form.reset();
      });
  }

  private getIngelogdeMedewerker() {
    this.identityService.readLoggedInUser().subscribe((ingelogdeMedewerker) => {
      this.ingelogdeMedewerker = ingelogdeMedewerker!;
    });
  }
}

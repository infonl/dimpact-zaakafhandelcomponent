/*
 * SPDX-FileCopyrightText: 2022 Atos
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
import { Subscription } from "rxjs";
import { FileInputFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/file-input/file-input-form-field-builder";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "src/app/shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { User } from "../../identity/model/user";
import { DateFormFieldBuilder } from "../../shared/material-form-builder/form-components/date/date-form-field-builder";
import { InputFormFieldBuilder } from "../../shared/material-form-builder/form-components/input/input-form-field-builder";
import { SelectFormFieldBuilder } from "../../shared/material-form-builder/form-components/select/select-form-field-builder";
import { FormComponent } from "../../shared/material-form-builder/form/form/form.component";
import { AbstractFormField } from "../../shared/material-form-builder/model/abstract-form-field";
import { FormConfig } from "../../shared/material-form-builder/model/form-config";
import { FormConfigBuilder } from "../../shared/material-form-builder/model/form-config-builder";
import { NavigationService } from "../../shared/navigation/navigation.service";
import { OrderUtil } from "../../shared/order/order-util";
import { ZakenService } from "../../zaken/zaken.service";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { EnkelvoudigInformatieObjectVersieGegevens } from "../model/enkelvoudig-informatie-object-versie-gegevens";
import { EnkelvoudigInformatieobject } from "../model/enkelvoudig-informatieobject";
import { InformatieobjectStatus } from "../model/informatieobject-status.enum";
import { Informatieobjecttype } from "../model/informatieobjecttype";
import { Vertrouwelijkheidaanduiding } from "../model/vertrouwelijkheidaanduiding.enum";

@Component({
  selector: "zac-informatie-object-edit",
  templateUrl: "./informatie-object-edit.component.html",
  styleUrls: ["./informatie-object-edit.component.less"],
})
export class InformatieObjectEditComponent implements OnInit, OnDestroy {
  @Input() infoObject: EnkelvoudigInformatieObjectVersieGegevens;
  @Input() sideNav: MatDrawer;
  @Input() zaakUuid: string;
  @Output() document = new EventEmitter<EnkelvoudigInformatieobject>();

  @ViewChild(FormComponent) form: FormComponent;

  fields: Array<AbstractFormField[]>;
  informatieobjecttypes: Informatieobjecttype[];
  formConfig: FormConfig;
  ingelogdeMedewerker: User;

  private subscriptions$: Subscription[] = [];

  constructor(
    private zakenService: ZakenService,
    private informatieObjectenService: InformatieObjectenService,
    private navigation: NavigationService,
    public utilService: UtilService,
    private configuratieService: ConfiguratieService,
    private translateService: TranslateService,
    private identityService: IdentityService,
    private vertrouwelijkaanduidingToTranslationKeyPipe: VertrouwelijkaanduidingToTranslationKeyPipe,
  ) {}

  ngOnInit(): void {
    this.formConfig = new FormConfigBuilder()
      .saveText("actie.toevoegen")
      .cancelText("actie.annuleren")
      .build();
    this.getIngelogdeMedewerker();

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
      .build();

    const beschrijving = new InputFormFieldBuilder(this.infoObject.beschrijving)
      .id("beschrijving")
      .label("beschrijving")
      .build();

    const taal = new SelectFormFieldBuilder({
      naam: this.translateService.instant(this.infoObject.taal.naam),
      value: this.infoObject.taal,
    })
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

    const auteur = new InputFormFieldBuilder(this.ingelogdeMedewerker.naam)
      .id("auteur")
      .label("auteur")
      .validators(Validators.required)
      .build();

    const vertrouwelijk = new SelectFormFieldBuilder({
      label: this.translateService.instant(
        this.vertrouwelijkaanduidingToTranslationKeyPipe.transform(
          this.infoObject.vertrouwelijkheidaanduiding,
        ),
      ),
      value: this.infoObject.vertrouwelijkheidaanduiding.toUpperCase(),
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
      .build();

    let vorigeBestandsnaam = null;
    inhoudField.fileUploaded.subscribe((bestandsnaam) => {
      const titelCtrl = titel.formControl;
      titelCtrl.setValue(bestandsnaam.replace(/\.[^/.]+$/, ""));
      vorigeBestandsnaam = "" + titelCtrl.value;
    });

    this.fields = [
      [inhoudField],
      [titel],
      [beschrijving],
      [status, vertrouwelijk],
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

  ngOnDestroy(): void {
    for (const subscription of this.subscriptions$) {
      subscription.unsubscribe();
    }
  }

  onFormSubmit(formGroup: FormGroup): void {
    if (formGroup) {
      const nieuweVersie = new EnkelvoudigInformatieObjectVersieGegevens();
      nieuweVersie.uuid = this.infoObject.uuid;
      Object.keys(formGroup.controls).forEach((key) => {
        const control = formGroup.controls[key];
        const value = control.value;
        console.log(key, value);
        if (value instanceof moment) {
          nieuweVersie[key] = value; // conversie niet nodig, ISO-8601 in UTC gaat goed met java ZonedDateTime.parse
        } else if (key === "taal") {
          nieuweVersie[key] = value.value;
        } else if (key === "status") {
          nieuweVersie[key] = InformatieobjectStatus[value.value.toUpperCase()];
        } else if (key === "vertrouwelijkheidaanduiding") {
          nieuweVersie[key] = value.value;
        } else if (key === "bestand") {
          nieuweVersie["bestandsnaam"] = value.name;
          nieuweVersie["file"] = value;
          nieuweVersie["formaat"] = value.type;
        } else {
          nieuweVersie[key] = value;
        }
      });

      this.informatieObjectenService
        .updateEnkelvoudigInformatieobject(
          nieuweVersie.uuid,
          this.zaakUuid,
          nieuweVersie,
        )
        .subscribe((document) => {
          this.document.emit(document);
          this.utilService.openSnackbar(
            "msg.document.nieuwe.versie.toegevoegd",
          );
          this.ngOnInit();
          this.sideNav.close();
          this.form.reset();
        });
    } else {
      this.sideNav.close();
    }
  }

  private getIngelogdeMedewerker() {
    this.identityService.readLoggedInUser().subscribe((ingelogdeMedewerker) => {
      this.ingelogdeMedewerker = ingelogdeMedewerker;
    });
  }
}

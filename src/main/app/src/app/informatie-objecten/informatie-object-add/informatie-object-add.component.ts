/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  Output,
  ViewChild,
} from "@angular/core";
import { FormGroup, Validators } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import { TranslateService } from "@ngx-translate/core";
import moment from "moment";
import { BehaviorSubject, Subscription, combineLatest, map, tap } from "rxjs";
import { LoggedInUser } from "src/app/identity/model/logged-in-user";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { CheckboxFormFieldBuilder } from "../../shared/material-form-builder/form-components/checkbox/checkbox-form-field-builder";
import { DateFormFieldBuilder } from "../../shared/material-form-builder/form-components/date/date-form-field-builder";
import { FileFormFieldBuilder } from "../../shared/material-form-builder/form-components/file/file-form-field-builder";
import { InputFormFieldBuilder } from "../../shared/material-form-builder/form-components/input/input-form-field-builder";
import { SelectFormField } from "../../shared/material-form-builder/form-components/select/select-form-field";
import { SelectFormFieldBuilder } from "../../shared/material-form-builder/form-components/select/select-form-field-builder";
import { FormComponent } from "../../shared/material-form-builder/form/form/form.component";
import { FormConfig } from "../../shared/material-form-builder/model/form-config";
import { FormConfigBuilder } from "../../shared/material-form-builder/model/form-config-builder";
import { OrderUtil } from "../../shared/order/order-util";
import { Taak } from "../../taken/model/taak";
import { Zaak } from "../../zaken/model/zaak";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { EnkelvoudigInformatieobject } from "../model/enkelvoudig-informatieobject";
import { InformatieobjectStatus } from "../model/informatieobject-status.enum";
import { Vertrouwelijkheidaanduiding } from "../model/vertrouwelijkheidaanduiding.enum";
import { FileInputFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/file-input/file-input-form-field-builder";

@Component({
  selector: "zac-informatie-object-add",
  templateUrl: "./informatie-object-add.component.html",
  styleUrls: ["./informatie-object-add.component.less"],
})
export class InformatieObjectAddComponent implements AfterViewInit, OnDestroy {
  @Input() zaak: Zaak;
  @Input() taak: Taak;
  @Input() sideNav: MatDrawer;
  @Output() document = new EventEmitter<EnkelvoudigInformatieobject>();

  @ViewChild(FormComponent) form: FormComponent;

  constructor(
    private informatieObjectenService: InformatieObjectenService,
    public utilService: UtilService,
    private configuratieService: ConfiguratieService,
    private translateService: TranslateService,
    private identityService: IdentityService,
  ) {}

  formConfig: FormConfig;
  loggedInUser$ = this.identityService.readLoggedInUser();
  // first iteration is always 0
  formIterations$ = new BehaviorSubject([0]);
  // last iteration is always active

  readonly activeIteration$ = this.formIterations$.pipe(
    map((iterations) => iterations.slice(-1)[0]),
  );

  fields$ = combineLatest([this.loggedInUser$]).pipe(
    map(([loggedInUser]) => this.getInputs({ loggedInUser })),
    tap((inputs) => this.setSubscriptions(inputs)),
    map((inputs) => this.getFormLayout(inputs)),
  );

  private informatieobjectStatussen: { label: string; value: string }[];
  private status: SelectFormField;
  private subscriptions: Subscription[] = [];

  private getInputs(deps: { loggedInUser: LoggedInUser }) {
    const { loggedInUser } = deps;
    this.formConfig = new FormConfigBuilder()
      .saveText("actie.toevoegen")
      .cancelText("actie.annuleren")
      .build();

    const vertrouwelijkheidsAanduidingen = this.utilService.getEnumAsSelectList(
      "vertrouwelijkheidaanduiding",
      Vertrouwelijkheidaanduiding,
    );
    this.informatieobjectStatussen =
      this.utilService.getEnumAsSelectListExceptFor(
        "informatieobject.status",
        InformatieobjectStatus,
        [InformatieobjectStatus.GEARCHIVEERD],
      );

    const titel = new InputFormFieldBuilder()
      .id("titel")
      .label("titel")
      .validators(Validators.required)
      .maxlength(100)
      .build();

    const beschrijving = new InputFormFieldBuilder()
      .id("beschrijving")
      .label("beschrijving")
      .maxlength(100)
      .build();

    const inhoudField = new FileInputFormFieldBuilder()
      .id("bestand")
      .label("bestandsnaam")
      .validators(Validators.required)
      .maxFileSizeMB(this.configuratieService.readMaxFileSizeMB())
      .additionalAllowedFileTypes(
        this.configuratieService.readAdditionalAllowedFileTypes(),
      )
      .build();

    const beginRegistratie = new DateFormFieldBuilder(moment())
      .id("creatiedatum")
      .label("creatiedatum")
      .validators(Validators.required)
      .build();

    const taal = new SelectFormFieldBuilder(
      this.configuratieService.readDefaultTaal(),
    )
      .id("taal")
      .label("taal")
      .optionLabel("naam")
      .options(this.configuratieService.listTalen())
      .value$(this.configuratieService.readDefaultTaal())
      .validators(Validators.required)
      .build();

    this.status = new SelectFormFieldBuilder(
      this.isAfgehandeld() ? this.getStatusDefinitief() : null,
    )
      .id("status")
      .label("status")
      .validators(Validators.required)
      .optionLabel("label")
      .options(this.informatieobjectStatussen)
      .build();

    const informatieobjectType = new SelectFormFieldBuilder()
      .id("informatieobjectTypeUUID")
      .label("informatieobjectType")
      .options(
        this.zaak
          ? this.informatieObjectenService.listInformatieobjecttypesForZaak(
              this.zaak.uuid,
            )
          : this.informatieObjectenService.listInformatieobjecttypesForZaak(
              this.taak.zaakUuid,
            ),
      )
      .optionLabel("omschrijving")
      .validators(Validators.required)
      .settings({ translateLabels: false, capitalizeFirstLetter: true })
      .build();

    const auteur = new InputFormFieldBuilder(loggedInUser.naam)
      .id("auteur")
      .label("auteur")
      .validators(Validators.required, Validators.pattern("\\S.*"))
      .maxlength(50)
      .build();

    const vertrouwelijk = new SelectFormFieldBuilder()
      .id("vertrouwelijkheidaanduiding")
      .label("vertrouwelijkheidaanduiding")
      .optionLabel("label")
      .options(vertrouwelijkheidsAanduidingen)
      .optionsOrder(OrderUtil.orderAsIs())
      .validators(Validators.required)
      .build();

    const ontvangstDatum = new DateFormFieldBuilder()
      .id("ontvangstdatum")
      .label("ontvangstdatum")
      .hint("msg.document.ontvangstdatum.hint", "start")
      .build();

    const verzendDatum = new DateFormFieldBuilder()
      .id("verzenddatum")
      .label("verzenddatum")
      .build();

    const nogmaals = new CheckboxFormFieldBuilder()
      .id("nogmaals")
      .label(this.translateService.instant("actie.document.toevoegen.nogmaals"))
      .build();

    return {
      inhoudField,
      titel,
      beschrijving,
      vertrouwelijkheidsAanduidingen,
      informatieobjectType,
      vertrouwelijk,
      beginRegistratie,
      auteur,
      taal,
      ontvangstDatum,
      verzendDatum,
      nogmaals,
    };
  }

  private getFormLayout({
    inhoudField,
    titel,
    beschrijving,
    informatieobjectType,
    vertrouwelijk,
    beginRegistratie,
    auteur,
    taal,
    ontvangstDatum,
    verzendDatum,
    nogmaals,
  }: ReturnType<InformatieObjectAddComponent["getInputs"]>) {
    if (this.zaak) {
      return [
        [inhoudField],
        [titel],
        [beschrijving],
        [informatieobjectType, vertrouwelijk],
        [this.status, beginRegistratie],
        [auteur, taal],
        [ontvangstDatum, verzendDatum],
        [nogmaals],
      ];
    } else if (this.taak) {
      return [
        [inhoudField],
        [titel],
        [informatieobjectType],
        [ontvangstDatum, verzendDatum],
        [nogmaals],
      ];
    }
  }

  private setSubscriptions({
    informatieobjectType,
    vertrouwelijk,
    vertrouwelijkheidsAanduidingen,
    verzendDatum,
    ontvangstDatum,
    inhoudField,
    titel,
  }: ReturnType<InformatieObjectAddComponent["getInputs"]>) {
    this.subscriptions.push(
      informatieobjectType.formControl.valueChanges.subscribe((value) => {
        if (value) {
          vertrouwelijk.formControl.setValue(
            vertrouwelijkheidsAanduidingen.find(
              (option) => option.value === value.vertrouwelijkheidaanduiding,
            ),
          );
        }
      }),
    );

    this.subscriptions.push(
      ontvangstDatum.formControl.valueChanges.subscribe((value) => {
        if (value && verzendDatum.formControl.enabled) {
          this.status.formControl.setValue(this.getStatusDefinitief());
          this.status.formControl.disable();
          verzendDatum.formControl.disable();
        } else if (!value && verzendDatum.formControl.disabled) {
          if (!this.isAfgehandeld()) {
            this.status.formControl.enable();
          }
          verzendDatum.formControl.enable();
        }
      }),
    );

    this.subscriptions.push(
      verzendDatum.formControl.valueChanges.subscribe((value) => {
        if (value && ontvangstDatum.formControl.enabled) {
          ontvangstDatum.formControl.disable();
        } else if (!value && ontvangstDatum.formControl.disabled) {
          ontvangstDatum.formControl.enable();
        }
      }),
    );

    let vorigeBestandsnaam = null;
    this.subscriptions.push(
      inhoudField.fileUploaded.subscribe((bestandsnaam) => {
        const titelCtrl = titel.formControl;
        if (!titelCtrl.value || titelCtrl.value === vorigeBestandsnaam) {
          titelCtrl.setValue(bestandsnaam.replace(/\.[^/.]+$/, ""));
          vorigeBestandsnaam = "" + titelCtrl.value;
        }
      }),
    );
  }

  private isAfgehandeld(): boolean {
    return this.zaak && !this.zaak.isOpen;
  }

  private getStatusDefinitief(): { label: string; value: string } {
    return this.informatieobjectStatussen.find(
      (option) =>
        option.value ===
        this.utilService.getEnumKeyByValue(
          InformatieobjectStatus,
          InformatieobjectStatus.DEFINITIEF,
        ),
    );
  }

  ngAfterViewInit(): void {
    if (this.isAfgehandeld()) {
      this.status.formControl.disable();
    }
  }

  ngOnDestroy(): void {
    for (const subscription of this.subscriptions) {
      subscription.unsubscribe();
    }
  }

  onFormSubmit(formGroup: FormGroup): void {
    if (formGroup) {
      const infoObject = new EnkelvoudigInformatieobject();
      Object.keys(formGroup.controls).forEach((key) => {
        const control = formGroup.controls[key];
        const value = control.value;
        if (value instanceof moment) {
          infoObject[key] = value; // conversie niet nodig, ISO-8601 in UTC gaat goed met java ZonedDateTime.parse
        } else if (key === "informatieobjectTypeUUID") {
          infoObject[key] = value.uuid;
        } else if (key === "taal") {
          infoObject[key] = value.code;
        } else if (key === "status") {
          infoObject[key] = InformatieobjectStatus[value.value];
        } else if (key === "vertrouwelijkheidaanduiding") {
          infoObject[key] = value.value;
        } else if (key === "bestand") {
          infoObject["bestandsomvang"] = value.size;
          infoObject["bestandsnaam"] = value.name;
          infoObject["bestand"] = value;
          infoObject["formaat"] = value.type;
        } else {
          infoObject[key] = value;
        }
      });

      this.informatieObjectenService
        .createEnkelvoudigInformatieobject(
          this.zaak ? this.zaak.uuid : this.taak.zaakUuid,
          this.zaak ? this.zaak.uuid : this.taak.id,
          infoObject,
          !!this.taak,
        )
        .subscribe((document) => {
          this.document.emit(document);
          const iterations = this.formIterations$.getValue();
          if (formGroup.get("nogmaals").value) {
            this.formIterations$.next([
              ...iterations,
              iterations.slice(-1)[0] + 1,
            ]);
          } else {
            this.formIterations$.next([
              ...iterations,
              iterations.slice(-1)[0] + 1,
            ]);
            this.sideNav.close();
          }
        });
    } else {
      this.sideNav.close();
    }
  }
}

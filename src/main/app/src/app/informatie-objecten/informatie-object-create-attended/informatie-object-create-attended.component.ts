/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
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
import { Subscription, combineLatest, map, tap } from "rxjs";
import { LoggedInUser } from "src/app/identity/model/logged-in-user";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { DateFormFieldBuilder } from "../../shared/material-form-builder/form-components/date/date-form-field-builder";
import { InputFormFieldBuilder } from "../../shared/material-form-builder/form-components/input/input-form-field-builder";
import { SelectFormField } from "../../shared/material-form-builder/form-components/select/select-form-field";
// import { SelectFormFieldBuilder } from "../../shared/material-form-builder/form-components/select/select-form-field-builder";
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
import { AutocompleteFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/autocomplete/autocomplete-form-field-builder";

@Component({
  selector: "zac-informatie-object-create-attended",
  templateUrl: "./informatie-object-create-attended.component.html",
  styleUrls: ["./informatie-object-create-attended.component.less"],
})
export class InformatieObjectCreateAttendedComponent implements OnDestroy {
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

  fields$ = combineLatest([this.loggedInUser$]).pipe(
    map(([loggedInUser]) => this.getInputs({ loggedInUser })),
    tap((inputs) => this.setSubscriptions(inputs)),
    map((inputs) => this.getFormLayout(inputs)),
  );

  private informatieobjectStatussen: { label: string; value: string }[];
  private status: SelectFormField;
  private subscriptions: Subscription[] = [];

  private getInputs(deps: { loggedInUser: LoggedInUser }) {
    console.log("LoggedInUser", deps.loggedInUser);
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

    const sjabloonGroep = new AutocompleteFormFieldBuilder()
      .id("sjabloonGroepUUID")
      .label("Sjabloongroep")
      .options(
        this.zaak
          ? this.informatieObjectenService.listInformatieobjecttypesForZaak(
              this.zaak.uuid,
            )
          : this.informatieObjectenService.listInformatieobjecttypesForZaak(
              this.taak.zaakUuid,
            ),
      )
      .optionLabel("Sjabloongroep")
      .validators(Validators.required)
      .settings({ translateLabels: false, capitalizeFirstLetter: true })
      .build();

    const sjabloon = new AutocompleteFormFieldBuilder()
      .id("sjabloonUUID")
      .label("Sjabloon")
      .options(
        this.zaak
          ? this.informatieObjectenService.listInformatieobjecttypesForZaak(
              this.zaak.uuid,
            )
          : this.informatieObjectenService.listInformatieobjecttypesForZaak(
              this.taak.zaakUuid,
            ),
      )
      .optionLabel("sjabloon")
      .validators(Validators.required)
      .settings({ translateLabels: false, capitalizeFirstLetter: true })
      .build();

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

    const informatieobjectType = new AutocompleteFormFieldBuilder() //new SelectFormFieldBuilder()
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
      .settings({ translateLabels: false, capitalizeFirstLetter: true })
      .build();

    const vertrouwelijk = new AutocompleteFormFieldBuilder() // new SelectFormFieldBuilder()
      .id("vertrouwelijkheidaanduiding")
      .label("vertrouwelijkheidaanduiding")
      .optionLabel("label")
      .options(vertrouwelijkheidsAanduidingen)
      .optionsOrder(OrderUtil.orderAsIs())
      .build();

    const beginRegistratie = new DateFormFieldBuilder(moment())
      .id("creatiedatum")
      .label("creatiedatum")
      .validators(Validators.required)
      .build();

    const auteur = new InputFormFieldBuilder(loggedInUser.naam)
      .id("auteur")
      .label("auteur")
      .validators(Validators.required, Validators.pattern("\\S.*"))
      .maxlength(50)
      .build();

    return {
      sjabloonGroep,
      sjabloon,
      titel,
      beschrijving,
      vertrouwelijkheidsAanduidingen,
      informatieobjectType,
      vertrouwelijk,
      beginRegistratie,
      auteur,
    };
  }

  private getFormLayout({
    sjabloonGroep,
    sjabloon,
    titel,
    beschrijving,
    informatieobjectType,
    vertrouwelijk,
    beginRegistratie,
    auteur,
  }: ReturnType<InformatieObjectCreateAttendedComponent["getInputs"]>) {
    if (this.zaak) {
      return [
        [sjabloonGroep, sjabloon],
        [titel],
        [beschrijving],
        [informatieobjectType, vertrouwelijk],
        [beginRegistratie, auteur],
      ];
    } else if (this.taak) {
      return [[titel], [informatieobjectType]];
    }
  }

  private setSubscriptions({
    informatieobjectType,
    vertrouwelijk,
    vertrouwelijkheidsAanduidingen,
  }: ReturnType<InformatieObjectCreateAttendedComponent["getInputs"]>) {
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
        } else if (key === "vertrouwelijkheidaanduiding") {
          infoObject[key] = value.value;
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
          this.sideNav.close();
        });
    } else {
      this.sideNav.close();
    }
  }
}

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
import moment from "moment";
import { Subscription, combineLatest, map, tap } from "rxjs";
import { LoggedInUser } from "src/app/identity/model/logged-in-user";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { DateFormFieldBuilder } from "../../shared/material-form-builder/form-components/date/date-form-field-builder";
import { InputFormFieldBuilder } from "../../shared/material-form-builder/form-components/input/input-form-field-builder";
import { SelectFormField } from "../../shared/material-form-builder/form-components/select/select-form-field";
import { SelectFormFieldBuilder } from "../../shared/material-form-builder/form-components/select/select-form-field-builder";
import { FormComponent } from "../../shared/material-form-builder/form/form/form.component";
import { FormConfig } from "../../shared/material-form-builder/model/form-config";
import { FormConfigBuilder } from "../../shared/material-form-builder/model/form-config-builder";
import { OrderUtil } from "../../shared/order/order-util";
import { Zaak } from "../../zaken/model/zaak";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { EnkelvoudigInformatieobject } from "../model/enkelvoudig-informatieobject";
import { InformatieobjectStatus } from "../model/informatieobject-status.enum";
import { Vertrouwelijkheidaanduiding } from "../model/vertrouwelijkheidaanduiding.enum";
import { AutocompleteFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/autocomplete/autocomplete-form-field-builder";
import { ZakenService } from "src/app/zaken/zaken.service";

@Component({
  selector: "zac-informatie-object-create-attended",
  templateUrl: "./informatie-object-create-attended.component.html",
  styleUrls: ["./informatie-object-create-attended.component.less"],
})
export class InformatieObjectCreateAttendedComponent implements OnDestroy {
  @Input() zaak: Zaak;
  @Input() sideNav: MatDrawer;
  @Output() document = new EventEmitter<EnkelvoudigInformatieobject>();

  @ViewChild(FormComponent) form: FormComponent;

  constructor(
    private zakenService: ZakenService,
    private informatieObjectenService: InformatieObjectenService,
    public utilService: UtilService,
    private identityService: IdentityService,
  ) {}

  formConfig: FormConfig;
  loggedInUser$ = this.identityService.readLoggedInUser();

  fields$ = combineLatest([this.loggedInUser$]).pipe(
    map(([loggedInUser]) => this.getInputs({ loggedInUser })),
    tap((inputs) => this.setSubscriptions(inputs)),
    map((inputs) => this.getFormLayout(inputs)),
  );

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

    console.log("zaak", this.zaak);
    const listZaaktypes$ = console.log(
      "this.zakenService.listZaaktypes()",
      this.zakenService.listZaaktypes(),
      this.zaak,
    );

    const sjabloonGroep = new AutocompleteFormFieldBuilder()
      .id("sjabloonGroepUUID")
      .label("Sjabloongroep")
      .validators(Validators.required)
      .optionLabel("omschrijving")
      .options(this.zakenService.listZaaktypes())
      .build();

    const sjabloon = new AutocompleteFormFieldBuilder()
      .id("sjabloonUUID")
      .label("Sjabloon")
      .validators(Validators.required)
      .optionLabel("omschrijving")
      .options(this.zakenService.listZaaktypes())
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

    const informatieobjectType = new SelectFormFieldBuilder()
      .id("informatieobjectTypeUUID")
      .label("informatieobjectType")
      .options(
        this.informatieObjectenService.listInformatieobjecttypesForZaak(
          this.zaak.uuid,
        ),
      )
      .optionLabel("omschrijving")
      .validators(Validators.required)
      .settings({ translateLabels: false, capitalizeFirstLetter: true })
      .build();

    const vertrouwelijk = new SelectFormFieldBuilder()
      .id("vertrouwelijkheidaanduiding")
      .label("vertrouwelijkheidaanduiding")
      .optionLabel("label")
      .options(vertrouwelijkheidsAanduidingen)
      .optionsOrder(OrderUtil.orderAsIs())
      .validators(Validators.required)
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
    return [
      [sjabloonGroep, sjabloon],
      [titel],
      [beschrijving],
      [informatieobjectType, vertrouwelijk],
      [beginRegistratie],
      [auteur],
    ];
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

  private isAfgehandeld(): boolean {
    return this.zaak && !this.zaak.isOpen;
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

        switch (key) {
          case "informatieobjectTypeUUID":
            infoObject[key] = value.uuid;
            break;
          case "taal":
            infoObject[key] = value.code;
            break;
          case "status":
            infoObject[key] = InformatieobjectStatus[value.value.toUpperCase()];
            break;
          case "vertrouwelijkheidaanduiding":
            infoObject[key] = value.value;
            break;
          case "bestand":
            infoObject["bestandsomvang"] = value.size;
            infoObject["bestandsnaam"] = value.name;
            infoObject["bestand"] = value;
            infoObject["formaat"] = value.type;
            break;
          default:
            if (value instanceof moment) {
              infoObject[key] = value; // conversie niet nodig, ISO-8601 in UTC gaat goed met java ZonedDateTime.parse
              break;
            }

            infoObject[key] = value;
        }
      });
    }
    this.sideNav.close();
  }
}

/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  EventEmitter,
  Input,
  OnInit,
  OnDestroy,
  Output,
  ViewChild,
} from "@angular/core";
import { FormGroup, Validators } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import moment from "moment";
import {
  Subscription,
  Subject,
  combineLatest,
  map,
  tap,
  BehaviorSubject,
  firstValueFrom,
} from "rxjs";
import { filter, takeUntil, first } from "rxjs/operators";
import { LoggedInUser } from "src/app/identity/model/logged-in-user";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { DateFormFieldBuilder } from "../../shared/material-form-builder/form-components/date/date-form-field-builder";
import { InputFormFieldBuilder } from "../../shared/material-form-builder/form-components/input/input-form-field-builder";
import { SelectFormField } from "../../shared/material-form-builder/form-components/select/select-form-field";
import { FormComponent } from "../../shared/material-form-builder/form/form/form.component";
import { FormConfig } from "../../shared/material-form-builder/model/form-config";
import { FormConfigBuilder } from "../../shared/material-form-builder/model/form-config-builder";
import { Zaak } from "../../zaken/model/zaak";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { EnkelvoudigInformatieobject } from "../model/enkelvoudig-informatieobject";
import { InformatieobjectStatus } from "../model/informatieobject-status.enum";
import { Vertrouwelijkheidaanduiding } from "../model/vertrouwelijkheidaanduiding.enum";
import { AutocompleteFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/autocomplete/autocomplete-form-field-builder";
import { ZakenService } from "src/app/zaken/zaken.service";
import { SmartDocumentsService } from "src/app/admin/smart-documents.service";

@Component({
  selector: "zac-informatie-object-create-attended",
  templateUrl: "./informatie-object-create-attended.component.html",
  styleUrls: ["./informatie-object-create-attended.component.less"],
})
export class InformatieObjectCreateAttendedComponent
  implements OnInit, OnDestroy
{
  @Input() zaak: Zaak;
  @Input() sideNav: MatDrawer;
  @Output() document = new EventEmitter<EnkelvoudigInformatieobject>();

  @ViewChild(FormComponent) form: FormComponent;

  sjabloonGroep: any;
  sjabloon: any;
  sjabloonOptions$: BehaviorSubject<any[]> = new BehaviorSubject([]);
  informatieobjectType: any;
  vertrouwelijk: any;

  constructor(
    private smartDocumentsService: SmartDocumentsService,
    private informatieObjectenService: InformatieObjectenService,
    public utilService: UtilService,
    private identityService: IdentityService,
  ) {}

  formConfig: FormConfig;
  loggedInUser$ = this.identityService.readLoggedInUser();
  informatieObjectTypes: any;

  async ngOnInit(): Promise<void> {
    this.informatieObjectTypes = await this.fetchInformatieobjecttypes();
  }

  fields$ = combineLatest([this.loggedInUser$]).pipe(
    map(([loggedInUser]) => this.getInputs({ loggedInUser })),
    tap((inputs) => this.setSubscriptions(inputs)),
    map((inputs) => this.getFormLayout(inputs)),
  );

  private status: SelectFormField;
  private subscriptions: Subscription[] = [];
  private ngDestroy = new Subject<void>();

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

    this.sjabloonGroep = new AutocompleteFormFieldBuilder()
      .id("sjabloonGroepUUID")
      .label("Sjabloongroep")
      .optionLabel("name")
      .validators(Validators.required)
      .options(
        this.smartDocumentsService.getTemplatesMappingFlat(
          this.zaak.zaaktype.uuid,
        ),
      )
      .build();

    this.sjabloon = new AutocompleteFormFieldBuilder()
      .id("sjabloonUUID")
      .label("Sjabloon")
      .optionLabel("name")
      .validators(Validators.required)
      .options(this.sjabloonOptions$)
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

    this.informatieobjectType = new InputFormFieldBuilder()
      .id("informatieobjectTypeUUID")
      .label("informatieobjectType")
      .validators(Validators.required)
      .disabled()
      .build();

    this.vertrouwelijk = new InputFormFieldBuilder()
      .id("vertrouwelijkheidaanduiding")
      .label("vertrouwelijkheidaanduiding")
      .validators(Validators.required)
      .disabled()
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

    this.sjabloonGroep.formControl.valueChanges
      .pipe(
        filter((zt) => typeof zt !== "string"),
        takeUntil(this.ngDestroy),
      )
      .subscribe((selectedTemplateGroup) => {
        this.sjabloon.formControl.setValue(null); // Always reset selected template after group change or clearing
        if (selectedTemplateGroup) {
          this.sjabloonOptions$.next(selectedTemplateGroup.templates);
        } else {
          this.sjabloonOptions$.next([]);
        }
      });

    this.sjabloon.formControl.valueChanges
      .pipe(
        filter((zt) => typeof zt !== "string"),
        takeUntil(this.ngDestroy),
      )
      .subscribe((selectedTemplate) => {
        if (selectedTemplate && selectedTemplate.informatieObjectTypeUUID) {
          this.informatieobjectType.formControl.setValue(
            this.informatieObjectTypes.find(
              (type) => type.uuid === selectedTemplate.informatieObjectTypeUUID,
            )?.omschrijving || null,
          );
          this.vertrouwelijk.formControl.setValue(
            this.informatieObjectTypes.find(
              (type) => type.uuid === selectedTemplate.informatieObjectTypeUUID,
            )?.vertrouwelijkheidaanduiding || null,
          );
        } else {
          this.informatieobjectType.formControl.setValue(null);
          this.vertrouwelijk.formControl.setValue(null);
        }
      });

    return {
      sjabloonGroep: this.sjabloonGroep,
      sjabloon: this.sjabloon,
      titel,
      beschrijving,
      vertrouwelijkheidsAanduidingen,
      informatieobjectType: this.informatieobjectType,
      vertrouwelijk: this.vertrouwelijk,
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

  private async fetchInformatieobjecttypes(): Promise<any> {
    try {
      const informatieobjecttypes = await firstValueFrom(
        this.informatieObjectenService
          .listInformatieobjecttypes(this.zaak.zaaktype.uuid)
          .pipe(first()),
      );
      return informatieobjecttypes;
    } catch (error) {
      console.error("Error fetching informatieobjecttypes:", error);
    }
  }

  private sjabloonGroupSelected(selectedZaaktype: any) {
    console.log("Selected sjabloongorep:", selectedZaaktype);
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
    this.ngDestroy.next();
    this.ngDestroy.complete();

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

        console.log("key", key, value);

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

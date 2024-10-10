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
import { Vertrouwelijkheidaanduiding } from "../model/vertrouwelijkheidaanduiding.enum";
import { AutocompleteFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/autocomplete/autocomplete-form-field-builder";
import { SmartDocumentsService } from "src/app/admin/smart-documents.service";
import { User } from "src/app/identity/model/user";
import { AbstractFormField } from "src/app/shared/material-form-builder/model/abstract-form-field";

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

  fields: Array<AbstractFormField[]>;
  formConfig: FormConfig;
  private ingelogdeMedewerker: User;
  private informatieObjectTypes: any;
  private subscriptions$: Subscription[] = [];
  private sjabloonOptions$: BehaviorSubject<any[]> = new BehaviorSubject([]);

  constructor(
    private smartDocumentsService: SmartDocumentsService,
    private informatieObjectenService: InformatieObjectenService,
    public utilService: UtilService,
    private identityService: IdentityService,
  ) {}

  async ngOnInit(): Promise<void> {
    this.formConfig = new FormConfigBuilder()
      .saveText("actie.toevoegen")
      .cancelText("actie.annuleren")
      .requireUserChanges()
      .build();
    this.getIngelogdeMedewerker();
    this.informatieObjectTypes = await this.fetchInformatieobjecttypes();

    const sjabloonGroep = new AutocompleteFormFieldBuilder()
      .id("sjabloonGroep")
      .label("Sjabloongroep")
      .optionLabel("name")
      .validators(Validators.required)
      .options(
        this.smartDocumentsService.getTemplatesMappingFlat(
          this.zaak.zaaktype.uuid,
        ),
      )
      .build();

    const sjabloon = new AutocompleteFormFieldBuilder()
      .id("sjabloon")
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

    const informatieobjectType = new InputFormFieldBuilder()
      .id("informatieobjectType")
      .label("informatieobjectType")
      .validators(Validators.required)
      .disabled()
      .build();

    const vertrouwelijk = new InputFormFieldBuilder()
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

    const auteur = new InputFormFieldBuilder(this.ingelogdeMedewerker.naam)
      .id("auteur")
      .label("auteur")
      .validators(Validators.required, Validators.pattern("\\S.*"))
      .maxlength(50)
      .build();

    sjabloonGroep.formControl.valueChanges
      .pipe(
        filter((zt) => typeof zt !== "string"),
        takeUntil(this.ngDestroy),
      )
      .subscribe((selectedTemplateGroup) => {
        sjabloon.formControl.setValue(null); // Always reset selected template after group change or clearing
        if (selectedTemplateGroup) {
          this.sjabloonOptions$.next(selectedTemplateGroup.templates);
        } else {
          this.sjabloonOptions$.next([]);
        }
      });

    sjabloon.formControl.valueChanges
      .pipe(
        filter((zt) => typeof zt !== "string"),
        takeUntil(this.ngDestroy),
      )
      .subscribe((selectedTemplate) => {
        if (selectedTemplate && selectedTemplate.informatieObjectTypeUUID) {
          informatieobjectType.formControl.setValue(
            this.informatieObjectTypes.find(
              (type) => type.uuid === selectedTemplate.informatieObjectTypeUUID,
            )?.omschrijving || null,
          );
          vertrouwelijk.formControl.setValue(
            this.informatieObjectTypes.find(
              (type) => type.uuid === selectedTemplate.informatieObjectTypeUUID,
            )?.vertrouwelijkheidaanduiding || null,
          );
        } else {
          informatieobjectType.formControl.setValue(null);
          vertrouwelijk.formControl.setValue(null);
        }
      });

    this.fields = [
      [sjabloonGroep, sjabloon],
      [titel],
      [beschrijving],
      [informatieobjectType, vertrouwelijk],
      [beginRegistratie],
      [auteur],
    ];
  }

  private ngDestroy = new Subject<void>();

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

  ngOnDestroy(): void {
    this.ngDestroy.next();
    this.ngDestroy.complete();
    for (const subscription of this.subscriptions$) {
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
          case "sjabloonGroep":
            infoObject[key] = value.id;
            break;
          case "sjabloon":
            infoObject[key] = value.informatieObjectTypeUUID;
            break;
          case "informatieobjectType":
            break;
          case "vertrouwelijkheidaanduiding":
            break;
          default:
            if (value instanceof moment) {
              infoObject[key] = value; // conversie niet nodig, ISO-8601 in UTC gaat goed met java ZonedDateTime.parse
              break;
            } else {
              infoObject[key] = value;
            }
            break;
        }
      });
      console.log("Object to submit to endpoint", infoObject);
    }
    this.sideNav.close();
  }

  private getIngelogdeMedewerker() {
    this.identityService.readLoggedInUser().subscribe((ingelogdeMedewerker) => {
      this.ingelogdeMedewerker = ingelogdeMedewerker;
    });
  }
}

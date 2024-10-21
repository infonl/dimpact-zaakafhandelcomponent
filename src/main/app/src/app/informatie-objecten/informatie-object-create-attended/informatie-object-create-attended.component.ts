/*
 * SPDX-FileCopyrightText: 2024 Lifely
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
import { MatDialog } from "@angular/material/dialog";
import { MatDrawer } from "@angular/material/sidenav";
import { TranslateService } from "@ngx-translate/core";
import moment from "moment";
import { BehaviorSubject, firstValueFrom, Subscription } from "rxjs";
import { first } from "rxjs/operators";
import { SmartDocumentsService } from "src/app/admin/smart-documents.service";
import { User } from "src/app/identity/model/user";
import { AutocompleteFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/autocomplete/autocomplete-form-field-builder";
import { AbstractFormField } from "src/app/shared/material-form-builder/model/abstract-form-field";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "src/app/shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { DateFormFieldBuilder } from "../../shared/material-form-builder/form-components/date/date-form-field-builder";
import { InputFormFieldBuilder } from "../../shared/material-form-builder/form-components/input/input-form-field-builder";
import { FormComponent } from "../../shared/material-form-builder/form/form/form.component";
import { FormConfig } from "../../shared/material-form-builder/model/form-config";
import { FormConfigBuilder } from "../../shared/material-form-builder/model/form-config-builder";
import {
  NotificationDialogComponent,
  NotificationDialogData,
} from "../../shared/notification-dialog/notification-dialog.component";
import { Zaak } from "../../zaken/model/zaak";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { DocumentCreationData } from "../model/document-creation-data";

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
  @Output() document = new EventEmitter<DocumentCreationData>();

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
    private vertrouwelijkaanduidingToTranslationKeyPipe: VertrouwelijkaanduidingToTranslationKeyPipe,
    private translateService: TranslateService,
    private dialog: MatDialog,
  ) {}

  async ngOnInit(): Promise<void> {
    this.formConfig = new FormConfigBuilder()
      .saveText("actie.toevoegen")
      .cancelText("actie.annuleren")
      .build();
    this.getIngelogdeMedewerker();
    this.informatieObjectTypes = await this.fetchInformatieobjecttypes();

    const templateGroup = new AutocompleteFormFieldBuilder()
      .id("templateGroup")
      .label("Sjabloongroep")
      .optionLabel("name")
      .validators(Validators.required)
      .options(
        this.smartDocumentsService.getTemplatesMappingFlat(
          this.zaak.zaaktype.uuid,
        ),
      )
      .build();

    const template = new AutocompleteFormFieldBuilder()
      .id("template")
      .label("Sjabloon")
      .optionLabel("name")
      .validators(Validators.required)
      .options(this.sjabloonOptions$)
      .build();

    const title = new InputFormFieldBuilder()
      .id("title")
      .label("titel")
      .validators(Validators.required)
      .maxlength(100)
      .build();

    const description = new InputFormFieldBuilder()
      .id("description")
      .label("beschrijving")
      .maxlength(100)
      .build();

    const informationObjectType = new InputFormFieldBuilder()
      .id("informationObjectType")
      .label("informatieobjectType")
      .disabled()
      .build();

    const confidentiality = new InputFormFieldBuilder()
      .id("confidentiality")
      .label("vertrouwelijkheidaanduiding")
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

    this.fields = [
      [templateGroup, template],
      [title],
      [description],
      [informationObjectType, confidentiality],
      [beginRegistratie],
      [auteur],
    ];

    this.subscriptions$.push(
      templateGroup.formControl.valueChanges.subscribe(
        (selectedTemplateGroup) => {
          template.formControl.setValue(null); // Always reset selected template after group change or clearing
          if (selectedTemplateGroup) {
            this.sjabloonOptions$.next(selectedTemplateGroup.templates);
          } else {
            this.sjabloonOptions$.next([]);
          }
        },
      ),
    );

    this.subscriptions$.push(
      template.formControl.valueChanges.subscribe((selectedTemplate) => {
        if (selectedTemplate && selectedTemplate.informatieObjectTypeUUID) {
          const infoObjectType = this.informatieObjectTypes.find(
            (type) => type.uuid === selectedTemplate.informatieObjectTypeUUID,
          );

          if (infoObjectType) {
            informationObjectType.formControl.setValue(
              infoObjectType.omschrijving || null,
            );
            confidentiality.formControl.setValue(
              this.translateService.instant(
                this.vertrouwelijkaanduidingToTranslationKeyPipe.transform(
                  infoObjectType.vertrouwelijkheidaanduiding,
                ) || null,
              ),
            );

            return;
          }
        }
        informationObjectType.formControl.setValue(null);
        confidentiality.formControl.setValue(null);
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

  ngOnDestroy(): void {
    for (const subscription of this.subscriptions$) {
      subscription.unsubscribe();
    }
  }

  onFormSubmit(formGroup: FormGroup): void {
    if (formGroup) {
      const documentCreateData = new DocumentCreationData();
      Object.keys(formGroup.controls).forEach((key) => {
        const control = formGroup.controls[key];
        const value = control.value;

        // Convert form fields to REST end point Body Parameters
        switch (key) {
          case "templateGroup":
            documentCreateData["zaakUuid"] = this.zaak.uuid;
            documentCreateData["smartDocumentsTemplateGroupId"] = value.id;
            break;
          case "template":
            documentCreateData["smartDocumentsTemplateId"] = value.id;
            break;
          case "informationObjectType":
          case "confidentiality":
            // Fields not end point Body Parameters; 'just informational', so leave them out. End point will determine these values itself (again)
            break;
          default:
            documentCreateData[key] = value;
            break;
        }
      });

      // Make REST call to create document
      this.informatieObjectenService
        .createDocumentAttended(documentCreateData)
        .subscribe((documentCreatieResponse) => {
          if (documentCreatieResponse.redirectURL) {
            window.open(documentCreatieResponse.redirectURL);
            this.sideNav.close();
            this.document.emit(documentCreateData);
            this.form.reset();
            this.sideNav.close();
          } else {
            this.dialog.open(NotificationDialogComponent, {
              data: new NotificationDialogData(documentCreatieResponse.message),
            });
          }
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

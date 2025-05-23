/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
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
import { BehaviorSubject, Subscription, firstValueFrom } from "rxjs";
import { first } from "rxjs/operators";
import { SmartDocumentsService } from "src/app/admin/smart-documents.service";
import { AutocompleteFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/autocomplete/autocomplete-form-field-builder";
import { HiddenFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/hidden/hidden-form-field-builder";
import { AbstractFormField } from "src/app/shared/material-form-builder/model/abstract-form-field";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "src/app/shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { Taak } from "src/app/taken/model/taak";
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
import { GeneratedType } from "../../shared/utils/generated-types";
import { InformatieObjectenService } from "../informatie-objecten.service";
import { DocumentCreationData } from "../model/document-creation-data";
import { Informatieobjecttype } from "../model/informatieobjecttype";

@Component({
  selector: "zac-informatie-object-create-attended",
  templateUrl: "./informatie-object-create-attended.component.html",
  styleUrls: ["./informatie-object-create-attended.component.less"],
})
export class InformatieObjectCreateAttendedComponent
  implements OnInit, OnDestroy
{
  @Input() zaak: GeneratedType<"RestZaak">;
  @Input() taak: Taak;
  @Input() sideNav: MatDrawer;
  @Output() document = new EventEmitter<DocumentCreationData>();

  @ViewChild(FormComponent) form: FormComponent;

  fields: Array<AbstractFormField[]>;
  formConfig: FormConfig;
  private ingelogdeMedewerker: GeneratedType<"RestLoggedInUser">;
  private informatieObjectTypes: Informatieobjecttype[] = [];
  private subscriptions$: Subscription[] = [];
  private sjabloonOptions$ = new BehaviorSubject<
    GeneratedType<"RestMappedSmartDocumentsTemplate">[]
  >([]);

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
      .requireUserChanges()
      .build();
    this.getIngelogdeMedewerker();
    this.informatieObjectTypes =
      (await this.fetchInformatieobjecttypes()) ?? [];

    const templateGroup = new AutocompleteFormFieldBuilder<
      GeneratedType<"RestMappedSmartDocumentsTemplateGroup">
    >()
      .id("templateGroup")
      .label("Sjabloongroep")
      .optionLabel("name")
      .validators(Validators.required)
      .options(
        this.smartDocumentsService.getTemplatesMapping(this.zaak.zaaktype.uuid),
      )
      .build();

    const template = new AutocompleteFormFieldBuilder<
      GeneratedType<"RestMappedSmartDocumentsTemplate">
    >()
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
      .id("creationDate")
      .label("creatiedatum")
      .validators(Validators.required)
      .build();

    const author = new InputFormFieldBuilder(this.ingelogdeMedewerker.naam)
      .id("author")
      .label("auteur")
      .validators(Validators.required, Validators.pattern("\\S.*"))
      .maxlength(50)
      .build();

    const taskId = new HiddenFormFieldBuilder(this.taak?.id || null)
      .id("taskId")
      .build();

    this.fields = [
      [templateGroup, template],
      [title],
      [description],
      [informationObjectType, confidentiality],
      [beginRegistratie],
      [author],
      [taskId],
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
                  infoObjectType.vertrouwelijkheidaanduiding as GeneratedType<"VertrouwelijkheidaanduidingEnum">,
                ),
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

  private async fetchInformatieobjecttypes() {
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
            this.document.emit(documentCreateData);
            //
            // On the above emit, the parent closes (and destroys) the sidebar and so this form.
            // The form gets reloaded/remounted again upon opening the sidebar, and so having this form in a nice pristine state.
            // Explicitly resetting the form is not needed.
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

  ngOnDestroy(): void {
    for (const subscription of this.subscriptions$) {
      subscription.unsubscribe();
    }
  }
}

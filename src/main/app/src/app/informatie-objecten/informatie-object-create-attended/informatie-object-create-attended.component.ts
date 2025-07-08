/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from "@angular/core";
import { FormBuilder, Validators } from "@angular/forms";
import { MatDialog } from "@angular/material/dialog";
import { MatDrawer } from "@angular/material/sidenav";
import { TranslateService } from "@ngx-translate/core";
import moment, { Moment } from "moment";
import {Observable, Subject, takeUntil} from "rxjs";
import { SmartDocumentsService } from "src/app/admin/smart-documents.service";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "src/app/shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { Taak } from "src/app/taken/model/taak";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import {
  NotificationDialogComponent,
  NotificationDialogData,
} from "../../shared/notification-dialog/notification-dialog.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { InformatieObjectenService } from "../informatie-objecten.service";

@Component({
  selector: "zac-informatie-object-create-attended",
  templateUrl: "./informatie-object-create-attended.component.html",
  styleUrls: ["./informatie-object-create-attended.component.less"],
})
export class InformatieObjectCreateAttendedComponent implements OnInit, OnDestroy {
  @Input({ required: true }) zaak!: GeneratedType<"RestZaak">;
  @Input({ required: true }) taak!: Taak;
  @Input({ required: true }) sideNav!: MatDrawer;
  @Input({ required: false }) smartDocumentsGroupPath: string[] = [];
  @Input({ required: false }) smartDocumentsTemplateName?: string;
  @Input({ required: false }) smartDocumentsInformatieobjecttypeUuid?: string;
  @Output() document = new EventEmitter<
    GeneratedType<"RestDocumentCreationAttendedData">
  >();

  private readonly destroy$ = new Subject<void>();

  private informatieObjectTypes: GeneratedType<"RestInformatieobjecttype">[] =
    [];

  protected readonly form = this.formBuilder.group({
    templateGroup:
      this.formBuilder.control<GeneratedType<"RestMappedSmartDocumentsTemplateGroup"> | null>(
        null,
        [Validators.required],
      ),
    template:
      this.formBuilder.control<GeneratedType<"RestMappedSmartDocumentsTemplate"> | null>(
        null,
        [Validators.required],
      ),
    title: this.formBuilder.control<string | null>(null, [
      Validators.required,
      Validators.maxLength(100),
    ]),
    description: this.formBuilder.control<string | null>(null, [
      Validators.maxLength(100),
    ]),
    informationObjectType: this.formBuilder.control<string | null>(null),
    confidentiality: this.formBuilder.control<string | null>(null),
    creationDate: this.formBuilder.control<Moment | null>(moment(), [
      Validators.required,
    ]),
    author: this.formBuilder.control<string | null>(null, [
      Validators.required,
      Validators.pattern("\\S.*"),
      Validators.maxLength(50),
    ]),
    taskId: this.formBuilder.control<string | null>(null),
  });

  protected templateGroups: GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[] =
    [];
  protected templates: GeneratedType<"RestMappedSmartDocumentsTemplate">[] = [];

  constructor(
    private readonly smartDocumentsService: SmartDocumentsService,
    private readonly informatieObjectenService: InformatieObjectenService,
    public readonly utilService: UtilService,
    private readonly identityService: IdentityService,
    private readonly vertrouwelijkaanduidingToTranslationKeyPipe: VertrouwelijkaanduidingToTranslationKeyPipe,
    private readonly translateService: TranslateService,
    private readonly dialog: MatDialog,
    private readonly formBuilder: FormBuilder,
  ) {}

  async ngOnInit() {
    this.getIngelogdeMedewerker();
    this.fetchInformatieobjecttypes();

    this.form.controls.template.disable();
    this.form.controls.informationObjectType.disable();
    this.form.controls.confidentiality.disable();

    const templateGroupsFetcher: Observable<typeof this.templateGroups> =
      this.smartDocumentsGroupPath &&
      this.smartDocumentsTemplateName &&
      this.smartDocumentsInformatieobjecttypeUuid
        ? this.smartDocumentsService.getTemplateGroup(
            { path: this.smartDocumentsGroupPath },
            this.smartDocumentsTemplateName,
            this.smartDocumentsInformatieobjecttypeUuid,
          )
        : this.smartDocumentsService.getTemplatesMapping(
            this.zaak.zaaktype.uuid,
          );

    templateGroupsFetcher.subscribe((templateGroups) => {
      this.templateGroups = templateGroups;

      const smartDocumentsTemplateGroup = templateGroups.find(({ name }) =>
        this.smartDocumentsGroupPath.includes(name),
      );
      if (!smartDocumentsTemplateGroup) return;

      this.form.controls.templateGroup.setValue(smartDocumentsTemplateGroup);

      if (templateGroups.length !== 1) return;

      this.form.controls.templateGroup.disable();
    });

    this.form.controls.templateGroup.valueChanges.pipe(takeUntil(this.destroy$)).subscribe((value) => {
      this.templates = value?.templates ?? [];

      if (!value?.templates) {
        this.form.controls.template.setValue(null);
        this.form.controls.template.disable();
        return;
      }

      this.form.controls.template.enable();

      if (value.templates.length !== 1) return;

      this.form.controls.template.setValue(value.templates.at(0) ?? null);
      this.form.controls.template.disable();
    });

    this.form.controls.template.valueChanges.pipe(takeUntil(this.destroy$)).subscribe((value) => {
      if (!value?.informatieObjectTypeUUID) {
        this.form.controls.informationObjectType.setValue(null);
        this.form.controls.confidentiality.setValue(null);
        return;
      }

      const infoObjectType = this.informatieObjectTypes.find(
        (type) => type.uuid === value.informatieObjectTypeUUID,
      );

      if (!infoObjectType) return;

      this.form.controls.informationObjectType.setValue(
        infoObjectType.omschrijving ?? null,
      );
      this.form.controls.confidentiality.setValue(
        this.translateService.instant(
          this.vertrouwelijkaanduidingToTranslationKeyPipe.transform(
            infoObjectType.vertrouwelijkheidaanduiding as GeneratedType<"VertrouwelijkheidaanduidingEnum">,
          ),
        ),
      );
    });
  }

  private fetchInformatieobjecttypes() {
    this.informatieObjectenService
      .listInformatieobjecttypes(this.zaak.zaaktype.uuid)
      .subscribe((types) => {
        this.informatieObjectTypes = types;
      });
  }

  onFormSubmit(formData?: typeof this.form) {
    const values = formData?.getRawValue();

    if (!formData?.valid || !values) {
      void this.sideNav.close();
      return;
    }

    const data: GeneratedType<"RestDocumentCreationAttendedData"> = {
      author: values.author!,
      smartDocumentsTemplateGroupId: values.templateGroup!.id,
      smartDocumentsTemplateId: values.template!.id,
      title: values.title!,
      creationDate: values.creationDate!.toISOString(),
      description: values.description,
      informatieobjecttypeUuid: this.smartDocumentsInformatieobjecttypeUuid,
      smartDocumentsTemplateName: this.smartDocumentsTemplateName,
      smartDocumentsTemplateGroupName: this.smartDocumentsGroupPath.at(-1),
      zaakUuid: this.zaak.uuid,
      taskId: this.taak?.id,
    };

    this.informatieObjectenService
      .createDocumentAttended(data)
      .subscribe(({ redirectURL, message }) => {
        if (!redirectURL) {
          this.dialog.open(NotificationDialogComponent, {
            data: new NotificationDialogData(message!),
          });
          return;
        }

        this.document.emit(data);
        window.open(redirectURL);
      });
  }

  private getIngelogdeMedewerker() {
    this.identityService.readLoggedInUser().subscribe((ingelogdeMedewerker) => {
      this.form.controls.author.setValue(ingelogdeMedewerker.naam);
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}

/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 Dimpact, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  ChangeDetectorRef,
  Component,
  computed,
  OnDestroy,
  OnInit,
  ViewChild,
} from "@angular/core";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import { MatSort } from "@angular/material/sort";
import { MatTableDataSource } from "@angular/material/table";
import { ActivatedRoute } from "@angular/router";
import { FormioForm } from "@formio/angular";
import { TranslateService } from "@ngx-translate/core";
import {
  injectMutation,
  injectQuery,
} from "@tanstack/angular-query-experimental";
import { lastValueFrom } from "rxjs";
import { ZaakDocumentenComponent } from "src/app/zaken/zaak-documenten/zaak-documenten.component";
import { UtilService } from "../../core/service/util.service";
import { ObjectType } from "../../core/websocket/model/object-type";
import { Opcode } from "../../core/websocket/model/opcode";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { mapStringToDocumentenStrings } from "../../documenten/document-utils";
import {
  FormioChangeEvent,
  FormioCustomEvent,
} from "../../formulieren/formio-wrapper/formio-wrapper.component";
import { AbstractTaakFormulier } from "../../formulieren/taken/abstract-taak-formulier";
import { TaakFormulierenService } from "../../formulieren/taken/taak-formulieren.service";
import {
  mapFormGroupToTaskData,
  mapTaskdataToTaskInformation,
} from "../../formulieren/taken/taak.utils";
import { IdentityService } from "../../identity/identity.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { ActionsViewComponent } from "../../shared/abstract-view/actions-view-component";
import { TextIcon } from "../../shared/edit/text-icon";
import { FormField, FormConfig as NewFormConfig } from "../../shared/form/form";
import { PatchBody, PutBody } from "../../shared/http/http-client";
import { InputFormFieldBuilder } from "../../shared/material-form-builder/form-components/input/input-form-field-builder";
import { MedewerkerGroepFieldBuilder } from "../../shared/material-form-builder/form-components/medewerker-groep/medewerker-groep-field-builder";
import { TextareaFormFieldBuilder } from "../../shared/material-form-builder/form-components/textarea/textarea-form-field-builder";
import { FormConfig } from "../../shared/material-form-builder/model/form-config";
import { FormConfigBuilder } from "../../shared/material-form-builder/model/form-config-builder";
import { ButtonMenuItem } from "../../shared/side-nav/menu-item/button-menu-item";
import { HeaderMenuItem } from "../../shared/side-nav/menu-item/header-menu-item";
import { MenuItem } from "../../shared/side-nav/menu-item/menu-item";
import { DateConditionals } from "../../shared/utils/date-conditionals";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../../zaken/zaken.service";
import { TakenService } from "../taken.service";
import { FormioSetupService } from "./formio/formio-setup-service";

@Component({
    templateUrl: "./taak-view.component.html",
    styleUrls: ["./taak-view.component.less"],
    standalone: false
})
export class TaakViewComponent
  extends ActionsViewComponent
  implements OnInit, OnDestroy
{
  @ViewChild("actionsSidenav") actionsSidenav!: MatSidenav;
  @ViewChild("menuSidenav") menuSidenav!: MatSidenav;
  @ViewChild("sideNavContainer") sideNavContainer!: MatSidenavContainer;
  @ViewChild("historieSort") historieSort!: MatSort;
  @ViewChild("zaakDocumentenComponent")
  zaakDocumentenComponent!: ZaakDocumentenComponent;

  protected taak?: GeneratedType<"RestTask">;
  protected zaak?: GeneratedType<"RestZaak">;
  protected formulier?: AbstractTaakFormulier | null = null;
  protected formConfig?: FormConfig | null = null;
  protected formulierDefinitie?: GeneratedType<"RESTFormulierDefinitie">;
  protected formioFormulier?: FormioForm;

  protected smartDocumentsGroupPath: string[] = [];
  protected smartDocumentsTemplateName?: string;
  protected smartDocumentsInformatieobjecttypeUuid?: string;

  protected menu: MenuItem[] = [];
  protected activeSideAction: string | null = null;
  protected documentToMove!: Partial<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >;

  protected historieSrc = new MatTableDataSource<
    GeneratedType<"RestTaskHistoryLine">
  >();
  protected historieColumns = [
    "datum",
    "wijziging",
    "oudeWaarde",
    "nieuweWaarde",
    "toelichting",
  ] as const;

  protected editFormFields = new Map<string, unknown>();
  protected fataledatumIcon: TextIcon | null = null;
  protected initialized = false;

  private taakListener?: WebsocketListener;
  readonly TaakStatusAfgerond =
    "AFGEROND" satisfies GeneratedType<"TaakStatus">;

  protected form = this.formBuilder.group({});
  protected formFields: FormField[] = [];
  protected _formConfig: NewFormConfig = {
    submitLabel: "actie.opslaan.afronden",
    partialSubmitLabel: "actie.opslaan",
    hideCancelButton: true,
  };

  private readonly loggedInUserQuery = injectQuery(() =>
    this.identityService.readLoggedInUser(),
  );

  private readonly updateTaakdataMutation = injectMutation(() => ({
    ...this.takenService.updateTaakdata(),
    onSuccess: () => {
      this.utilService.openSnackbar("msg.taak.opgeslagen");
    },
  }));

  private readonly completeTaakMutation = injectMutation(() => ({
    ...this.takenService.complete(),
    onSuccess: () => {
      this.utilService.openSnackbar("msg.taak.afgerond");
    },
  }));

  protected readonly isPending = computed(
    () =>
      this.updateTaakdataMutation.isPending() ||
      this.completeTaakMutation.isPending(),
  );

  constructor(
    private readonly route: ActivatedRoute,
    private readonly takenService: TakenService,
    private readonly zakenService: ZakenService,
    public readonly utilService: UtilService,
    private readonly websocketService: WebsocketService,
    private readonly taakFormulierenService: TaakFormulierenService,
    private readonly identityService: IdentityService,
    protected readonly translate: TranslateService,
    private readonly formioSetupService: FormioSetupService,
    private readonly changeDetectorRef: ChangeDetectorRef,
    private readonly formBuilder: FormBuilder,
    private readonly informatieObjectenService: InformatieObjectenService,
  ) {
    super();
  }

  ngOnInit() {
    this.route.data.subscribe((data) => {
      this.createZaakFromTaak(data.taak);
      this.init(data.taak);

      this.taakListener = this.websocketService.addListenerWithSnackbar(
        Opcode.ANY,
        ObjectType.TAAK,
        data.taak.id,
        () => {
          this.takenService.readTaak(data.taak.id).subscribe((task) => {
            this.init(task, false);
          });
        },
      );

      this.historieSrc.sortingDataAccessor = (item, property) => {
        switch (property) {
          case "datum":
            return item.datumTijd!;
          default:
            return item[property as keyof typeof item] as string;
        }
      };
      this.historieSrc.sort = this.historieSort;

      this.changeDetectorRef.detectChanges();

      if (data.taak.status === "AFGEROND") return;

      this.fataledatumIcon = new TextIcon(
        DateConditionals.provideFormControlValue(DateConditionals.isExceeded),
        "report_problem",
        "errorTaakVerlopen_icon",
        "msg.datum.overschreden",
        "error",
      );
    });
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.websocketService.removeListener(this.taakListener);
  }

  private initTaakGegevens(taak: GeneratedType<"RestTask">) {
    this.taak = taak;
    this.loadHistorie();
    this.setEditableFormFields();
    this.setupMenu();
  }

  private init(taak: GeneratedType<"RestTask">, readZaak = true) {
    this.initialized = false;

    this.initTaakGegevens(taak);

    // For legacy forms, we need to re-create the form to fix the loading state
    if (!readZaak && !this.formulier) {
      this.initialized = true;
      return;
    }

    this.zakenService.readZaak(taak.zaakUuid).subscribe((zaak) => {
      this.zaak = zaak;
      this.createTaakForm(taak, zaak);
      this.initialized = true;
      this.setupMenu();
    });
  }

  private createTaakForm(
    taak: GeneratedType<"RestTask">,
    zaak: GeneratedType<"RestZaak">,
  ) {
    this.formConfig = null;
    this.formulier = null;
    this.formulierDefinitie = undefined;
    this.formioFormulier = undefined;

    this.changeDetectorRef.detectChanges();

    if (taak.formulierDefinitieId) {
      void this.createHardCodedTaakForm(taak, zaak);
    } else if (taak.formulierDefinitie) {
      this.createConfigurableTaakForm(taak.formulierDefinitie);
    } else if (!this.formioFormulier) {
      this.formioFormulier = taak.formioFormulier ?? undefined;
      if (!this.formioFormulier) return;
      this.formioSetupService.createFormioForm(this.formioFormulier, taak);
    }
  }

  private async createHardCodedTaakForm(
    taak: GeneratedType<"RestTask">,
    zaak: GeneratedType<"RestZaak">,
  ) {
    if (taak.status !== "AFGEROND" && taak.rechten.wijzigen) {
      this.formConfig = new FormConfigBuilder()
        .partialText("actie.opslaan")
        .saveText("actie.opslaan.afronden")
        .build();
    } else {
      this.formConfig = null;
    }

    try {
      const formFields =
        await this.taakFormulierenService.getAngularHandleFormBuilder(
          taak,
          zaak,
        );

      formFields.forEach((formField) => {
        this.form.addControl(
          formField.key,
          formField.control ??
            this.formBuilder.control(taak.taakdata?.[formField.key] ?? null),
        );

        this.formFields.push(formField);
      });

      const explanationControl = this.formBuilder.control(taak.toelichting, [
        Validators.maxLength(1000),
      ]);
      this.form.addControl("toelichting", explanationControl);
      this.formFields.push({ type: "textarea", key: "toelichting" });

      const allAttachments = [
        ...(taak.taakdocumenten ?? []),
        ...mapStringToDocumentenStrings(taak.taakdata?.bijlagen),
      ];
      const attachments = await lastValueFrom(
        this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
          zaakUUID: zaak.uuid,
          informatieobjectUUIDs: allAttachments,
        }),
      );

      const attachmentsControl =
        this.formBuilder.control<
          GeneratedType<"RestEnkelvoudigInformatieobject">[]
        >(attachments); // Make sure the control initially has all the attachments checked

      this.form.addControl("bijlagen", attachmentsControl);
      this.formFields.push({
        type: "documents",
        key: "bijlagen",
        readonly: true,
        options: attachments,
      });
    } catch (e) {
      console.warn(e);
      // Handle form in the old way
      this.formulier = this.taakFormulierenService
        .getFormulierBuilder(
          this.taak
            ?.formulierDefinitieId as GeneratedType<"FormulierDefinitie">,
        )
        .behandelForm(taak, zaak)
        .build();
      if (this.formulier.disablePartialSave && this.formConfig) {
        this.formConfig.partialButtonText = null;
      }
    }

    this.utilService.setTitle("title.taak", {
      taak: this.translate.instant(
        this.isReadonly() ? "title.taak.raadplegen" : "title.taak.behandelen",
        {
          taak: taak.naam,
        },
      ),
    });
  }

  private createConfigurableTaakForm(
    formulierDefinitie: GeneratedType<"RESTFormulierDefinitie">,
  ) {
    this.formulierDefinitie = formulierDefinitie;
    this.utilService.setTitle("title.taak", {
      taak: formulierDefinitie.naam,
    });
  }

  protected isReadonly() {
    return this.taak?.status === "AFGEROND" || !this.taak?.rechten.wijzigen;
  }

  private setEditableFormFields() {
    if (!this.taak) return;
    this.editFormFields.set(
      "medewerker-groep",
      new MedewerkerGroepFieldBuilder(this.taak.groep!, this.taak.behandelaar!)
        .id("medewerker-groep")
        .groepLabel("groep.-kies-")
        .groepRequired()
        .medewerkerLabel("behandelaar.-kies-")
        .setZaaktypeUuid(this.taak.zaaktypeUUID!)
        .build(),
    );
    this.editFormFields.set(
      "toelichting",
      new TextareaFormFieldBuilder(this.taak.toelichting)
        .id("toelichting")
        .label("toelichting")
        .maxlength(1000)
        .build(),
    );
    this.editFormFields.set(
      "reden",
      new InputFormFieldBuilder()
        .id("reden")
        .label("reden")
        .maxlength(80)
        .build(),
    );
  }

  private setupMenu() {
    this.menu = [];
    this.menu.push(new HeaderMenuItem("taak"));

    if (this.taak?.rechten.toevoegenDocument) {
      this.menu.push(
        new ButtonMenuItem(
          "actie.document.toevoegen",
          () => this.actionsSidenav.open(),
          "upload_file",
        ),
      );

      if (
        this.zaak?.zaaktype.zaakafhandelparameters?.smartDocuments
          .enabledGlobally &&
        this.zaak?.zaaktype?.zaakafhandelparameters.smartDocuments
          .enabledForZaaktype
      ) {
        this.menu.push(
          new ButtonMenuItem(
            "actie.document.maken",
            () => this.actionsSidenav.open(),
            "note_add",
          ),
        );
      }
    }
  }

  private loadHistorie() {
    if (!this.taak?.id) return;

    this.takenService
      .listHistorieVoorTaak(this.taak.id)
      .subscribe((historie) => {
        this.historieSrc.data = historie;
      });
  }

  editTaak() {
    this.activeSideAction = "actie.taak.wijzigen";
    this.actionsSidenav.open();
  }

  onHardCodedFormSubmit(formGroup: FormGroup, partial = false) {
    let taskBody:
      | PutBody<"/rest/taken/taakdata">
      | PatchBody<"/rest/taken/complete">;

    try {
      if (!this.formulier) throw new Error("Handling form in Angular way");
      console.info("Handling form in the DEPRECATED way");
      taskBody = this.formulier.getTaak(formGroup);
    } catch {
      console.info("Handling form in Angular way");
      taskBody = {
        ...this.taak!,
        taakdata: {
          ...this.taak!.taakdata,
          ...mapFormGroupToTaskData(formGroup, {
            ignoreKeys: ["bijlagen"],
          }),
        },
        toelichting: formGroup.get("toelichting")?.value,
        taakinformatie: {
          ...this.taak!.taakinformatie,
          ...mapTaskdataToTaskInformation(
            mapFormGroupToTaskData(formGroup, {
              mapControlOptions: {
                documentKey: "titel",
                documentSeparator: ", ",
              },
            }),
            this.taak!,
          ),
        },
      };
    }

    if (!taskBody) return;

    if (partial) {
      this.updateTaakdataMutation.mutate(taskBody, {
        onSuccess: (task) => {
          this.init(task, false);
        },
      });
      return;
    }

    this.completeTaakMutation.mutate(taskBody, {
      onSuccess: (task) => {
        this.init(task, false);
      },
    });
  }

  onConfigurableFormPartial(formState?: Record<string, string>) {
    if (!formState) return;
    if (!this.taak) return;

    this.taak.taakdata = formState;
    this.updateTaakdataMutation.mutate(this.taak, {
      onSuccess: (task) => {
        this.init(task);
      },
    });
  }

  onConfigurableFormSubmit(formState?: Record<string, string>) {
    if (!formState) return;
    if (!this.taak) return;

    this.taak.taakdata = formState;
    this.completeTaakMutation.mutate(this.taak, {
      onSuccess: (task) => {
        this.init(task);
      },
    });
  }

  onFormioFormSubmit(submission: {
    data: Record<string, string>;
    state: string;
  }) {
    for (const key in submission.data) {
      if (key !== "submit" && key !== "save" && this.taak?.taakdata) {
        this.taak.taakdata[key] = submission.data[key];
      }
    }
    if (!this.taak) return;

    if (submission.state === "submitted") {
      this.completeTaakMutation.mutate(this.taak);
      return;
    }

    this.updateTaakdataMutation.mutate(this.taak);
  }

  onFormioFormChange(event: FormioChangeEvent) {
    this.formioSetupService.setFormioChangeData(event.data);
  }

  onDocumentCreate(event: FormioCustomEvent) {
    this.smartDocumentsTemplateName =
      this.formioSetupService.extractSmartDocumentsTemplateName(event);
    if (!this.smartDocumentsTemplateName) {
      console.debug("No SmartDocuments template name selected!");
      return;
    }

    this.activeSideAction = "actie.document.maken";
    this.smartDocumentsGroupPath =
      this.formioSetupService.getSmartDocumentsGroups(event.component);
    const normalizedTemplateName =
      this.formioSetupService.normalizeSmartDocumentsTemplateName(
        this.smartDocumentsTemplateName,
      );
    this.smartDocumentsInformatieobjecttypeUuid =
      this.formioSetupService.getInformatieobjecttypeUuid(
        event,
        normalizedTemplateName,
      );
    void this.actionsSidenav.open();
  }

  updateTaakdocumenten(
    informatieobject: GeneratedType<"RestEnkelvoudigInformatieobject">,
  ) {
    if (!this.taak) return;

    if (!this.taak.taakdocumenten) {
      this.taak.taakdocumenten = [];
    }

    this.taak.taakdocumenten.push(informatieobject.uuid!);

    if (this.formulier) {
      // Old way of handling new attachments (using the ATOS forms)
      this.formulier.refreshTaakdocumentenEnBijlagen();
    } else {
      // New way of handling new attachments (using Angular forms)
      const control = this.form.get("bijlagen");
      if (!control) return;
      const newAttachments = [...(control.value ?? []), informatieobject];
      this.formFields.forEach((field) => {
        if (field.type === "documents" && field.key === "bijlagen") {
          field.options = newAttachments;
        }
      });
      control.setValue(newAttachments as never);
    }
  }

  documentCreated() {
    void this.actionsSidenav.close();

    if (!this.taak) return;
    const listener = this.websocketService.addListener(
      Opcode.UPDATED,
      ObjectType.ZAAK_INFORMATIEOBJECTEN,
      this.taak.zaakUuid,
      () => {
        this.websocketService.removeListener(listener);
        this.loadHistorie();
      },
    );
  }

  documentMoveToCase(
    $event: Partial<GeneratedType<"RestEnkelvoudigInformatieobject">>,
  ) {
    this.activeSideAction = "actie.document.verplaatsen";
    this.documentToMove = $event;
    void this.actionsSidenav.open();
  }

  updateZaakDocumentList() {
    this.zaakDocumentenComponent.updateDocumentList();
  }

  protected updateZaak() {
    const zaakUuid = this.zaak?.uuid ?? this.taak?.zaakUuid;
    if (!zaakUuid) return;
    this.zakenService.readZaak(zaakUuid).subscribe((zaak) => {
      this.zaak = zaak;
    });
  }

  /**
   *  Zaak is nog niet geladen, beschikbare zaak-data uit de taak vast weergeven totdat de zaak is geladen
   */
  private createZaakFromTaak(taak: GeneratedType<"RestTask">) {
    const zaaktype = {
      omschrijving: taak.zaaktypeOmschrijving,
    } satisfies Partial<
      GeneratedType<"RestZaaktype">
    > as GeneratedType<"RestZaaktype">;

    this.zaak = {
      identificatie: taak.zaakIdentificatie,
      uuid: taak.zaakUuid,
      zaaktype,
    } satisfies Partial<GeneratedType<"RestZaak">> as GeneratedType<"RestZaak">;
  }
}

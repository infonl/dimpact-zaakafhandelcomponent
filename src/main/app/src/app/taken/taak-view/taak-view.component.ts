/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 Dimpact, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
import { CommonModule } from "@angular/common";
import {
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
  computed,
  effect,
  inject,
  signal,
  untracked,
  viewChild,
} from "@angular/core";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatDividerModule } from "@angular/material/divider";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressBarModule } from "@angular/material/progress-bar";
import {
  MatDrawer,
  MatSidenav,
  MatSidenavContainer,
  MatSidenavModule,
} from "@angular/material/sidenav";
import { MatSort, MatSortModule } from "@angular/material/sort";
import { MatTableDataSource, MatTableModule } from "@angular/material/table";
import { MatTabsModule } from "@angular/material/tabs";
import { MatTooltipModule } from "@angular/material/tooltip";
import { ActivatedRoute } from "@angular/router";
import type { FormioForm } from "@formio/angular";
import { TranslatePipe } from "@ngx-translate/core";
import { QueryClient, injectQuery } from "@tanstack/angular-query-experimental";
import { ZaakDocumentenComponent } from "src/app/zaken/zaak-documenten/zaak-documenten.component";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { mapStringToDocumentenStrings } from "../../documenten/document-utils";
import type {
  FormioChangeEvent,
  FormioCustomEvent,
} from "../../formulieren/formio-wrapper/formio-wrapper.component";
import { FormioWrapperComponent } from "../../formulieren/formio-wrapper/formio-wrapper.component";
import { TaakFormulierenService } from "../../formulieren/taken/taak-formulieren.service";
import {
  mapFormGroupToTaskData,
  mapTaskdataToTaskInformation,
} from "../../formulieren/taken/taak.utils";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { IdentityService } from "../../identity/identity.service";
import { InformatieObjectAddComponent } from "../../informatie-objecten/informatie-object-add/informatie-object-add.component";
import { InformatieObjectCreateAttendedComponent } from "../../informatie-objecten/informatie-object-create-attended/informatie-object-create-attended.component";
import { InformatieObjectLinkComponent } from "../../informatie-objecten/informatie-object-link/informatie-object-link.component";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { ActionsViewComponent } from "../../shared/abstract-view/actions-view-component";
import { TextIcon } from "../../shared/edit/text-icon";
import { ZacComposedForm } from "../../shared/form/composed-form/composed-form.component";
import {
  FormField,
  FormConfig as NewFormConfig,
} from "../../shared/form/composed-form/form-field.types";
import { PatchBody, PutBody } from "../../shared/http/http-client";
import { injectMutation } from "../../shared/http/inject-mutation";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { I18nKeyPipe } from "../../shared/pipes/i18n-key.pipe";
import { I18nLabelPipe } from "../../shared/pipes/i18n-label.pipe";
import { MimetypeToExtensionPipe } from "../../shared/pipes/mimetypeToExtension.pipe";
import { ReadMoreComponent } from "../../shared/read-more/read-more.component";
import { ButtonMenuItem } from "../../shared/side-nav/menu-item/button-menu-item";
import { HeaderMenuItem } from "../../shared/side-nav/menu-item/header-menu-item";
import { MenuItem } from "../../shared/side-nav/menu-item/menu-item";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { DateConditionals } from "../../shared/utils/date-conditionals";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakVerkortComponent } from "../../zaken/zaak-verkort/zaak-verkort.component";
import { ZakenService } from "../../zaken/zaken.service";
import { TaakEditComponent } from "../taak-edit/taak-edit.component";
import { TakenService } from "../taken.service";
import { FormioSetupService } from "./formio/formio-setup-service";

@Component({
  templateUrl: "./taak-view.component.html",
  styleUrls: ["./taak-view.component.less"],
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatDividerModule,
    MatIconModule,
    MatProgressBarModule,
    MatSidenavModule,
    MatSortModule,
    MatTableModule,
    MatTabsModule,
    MatTooltipModule,
    TranslatePipe,
    DatumPipe,
    EmptyPipe,
    I18nKeyPipe,
    I18nLabelPipe,
    MimetypeToExtensionPipe,
    FormioWrapperComponent,
    InformatieObjectAddComponent,
    InformatieObjectCreateAttendedComponent,
    InformatieObjectLinkComponent,
    ReadMoreComponent,
    SideNavComponent,
    StaticTextComponent,
    TaakEditComponent,
    ZaakDocumentenComponent,
    ZaakVerkortComponent,
    ZacComposedForm,
  ],
})
export class TaakViewComponent
  extends ActionsViewComponent
  implements OnInit, OnDestroy
{
  private readonly route = inject(ActivatedRoute);
  private readonly takenService = inject(TakenService);
  private readonly zakenService = inject(ZakenService);
  private readonly websocketService = inject(WebsocketService);
  private readonly taakFormulierenService = inject(TaakFormulierenService);
  private readonly identityService = inject(IdentityService);
  private readonly formioSetupService = inject(FormioSetupService);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  private readonly formBuilder = inject(FormBuilder);
  private readonly informatieObjectenService = inject(
    InformatieObjectenService,
  );
  private readonly foutAfhandelingService = inject(FoutAfhandelingService);

  readonly actionsSidenav = viewChild.required<MatDrawer>("actionsSidenav");
  readonly menuSidenav = viewChild.required<MatSidenav>("menuSidenav");
  readonly sideNavContainer =
    viewChild.required<MatSidenavContainer>("sideNavContainer");
  readonly historieSort = viewChild<MatSort>("historieSort");
  readonly zaakDocumentenComponent = viewChild<ZaakDocumentenComponent>(
    "zaakDocumentenComponent",
  );

  protected readonly taak = signal<GeneratedType<"RestTask"> | undefined>(
    undefined,
  );
  protected readonly zaak = signal<GeneratedType<"RestZaak"> | undefined>(
    undefined,
  );
  protected readonly formioFormulier = signal<FormioForm | undefined>(
    undefined,
  );

  protected readonly smartDocumentsGroupId = signal<string | undefined>(
    undefined,
  );
  protected readonly smartDocumentsTemplateId = signal<string | undefined>(
    undefined,
  );

  protected readonly menu = signal<MenuItem[]>([]);
  protected readonly activeSideAction = signal<string | null>(null);
  protected readonly documentToMove = signal<
    Partial<GeneratedType<"RestEnkelvoudigInformatieobject">>
  >({});

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

  protected readonly fataledatumIcon = signal<TextIcon | null>(null);
  protected readonly initialized = signal(false);

  private taakListener?: WebsocketListener;
  readonly TaakStatusAfgerond =
    "AFGEROND" satisfies GeneratedType<"TaakStatus">;

  protected form = this.formBuilder.group({});
  protected readonly formFields = signal<FormField[]>([]);
  protected _formConfig: NewFormConfig = {
    submitLabel: "actie.opslaan.afronden",
    partialSubmitLabel: "actie.opslaan",
    hideCancelButton: true,
  };

  private readonly loggedInUserQuery = injectQuery(() =>
    this.identityService.readLoggedInUser(),
  );

  private readonly updateTaakdataMutation = injectMutation(() =>
    this.takenService.updateTaakdata(),
  );

  private readonly completeTaakMutation = injectMutation(() =>
    this.takenService.complete(),
  );

  protected readonly isPending = computed(
    () =>
      this.updateTaakdataMutation.isPending() ||
      this.completeTaakMutation.isPending(),
  );

  private readonly queryClient = inject(QueryClient);

  private readonly taakIdToReread = signal<string | null>(null);
  private readonly rereadTaakQuery = injectQuery(() => ({
    ...this.takenService.readTaak(this.taakIdToReread()!),
    enabled: !!this.taakIdToReread(),
    structuralSharing: false,
  }));

  private readonly historieTaakId = signal<string | null>(null);
  private readonly historieQuery = injectQuery(() => ({
    ...this.takenService.listHistorieVoorTaak(this.historieTaakId()!),
    enabled: !!this.historieTaakId(),
  }));
  protected readonly historie = computed(() => this.historieQuery.data() ?? []);

  private readonly lastSubmitFailed = signal(false);
  protected readonly hasFailed = this.lastSubmitFailed.asReadonly();

  constructor() {
    super();

    effect(() => {
      const task = this.rereadTaakQuery.data();
      if (!task) return;
      untracked(() => this.init(task, false));
    });

    effect(() => {
      this.historieSrc.data = this.historie();
    });
  }

  ngOnInit() {
    this.route.data.subscribe((data) => {
      this.createZaakFromTaak(data.taak);
      this.init(data.taak);

      this.taakListener = this.websocketService.addListenerWithSnackbar(
        "ANY",
        "TAAK",
        data.taak.id,
        () => this.rereadTaak(data.taak.id),
      );

      this.historieSrc.sortingDataAccessor = (item, property) => {
        switch (property) {
          case "datum":
            return item.datumTijd!;
          default:
            return item[property as keyof typeof item] as string;
        }
      };
      this.historieSrc.sort = this.historieSort() ?? null;

      this.changeDetectorRef.detectChanges();

      if (data.taak.status === "AFGEROND") return;

      this.fataledatumIcon.set(
        new TextIcon(
          DateConditionals.provideFormControlValue(DateConditionals.isExceeded),
          "report_problem",
          "errorTaakVerlopen_icon",
          "msg.datum.overschreden",
          "error",
        ),
      );
    });
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.websocketService.removeListener(this.taakListener);
  }

  private initTaakGegevens(taak: GeneratedType<"RestTask">) {
    this.taak.set(taak);
    this.loadHistorie();
    this.setupMenu();
  }

  private init(taak: GeneratedType<"RestTask">, readZaak = true) {
    this.initialized.set(false);

    this.initTaakGegevens(taak);

    if (!readZaak) {
      this.initialized.set(true);
      return;
    }

    this.zakenService.readZaak(taak.zaakUuid).subscribe((zaak) => {
      this.zaak.set(zaak);
      void this.createTaakForm(taak, zaak);
      this.initialized.set(true);
      this.setupMenu();
    });
  }

  private async createTaakForm(
    taak: GeneratedType<"RestTask">,
    zaak: GeneratedType<"RestZaak">,
  ) {
    this.formioFormulier.set(undefined);

    this.changeDetectorRef.detectChanges();

    if (taak.formulierDefinitieId) {
      void this.createHardCodedTaakForm(taak, zaak);
    } else if (taak.formioFormulier) {
      await this.formioSetupService.createFormioForm(
        taak.formioFormulier,
        taak,
      );
      this.formioFormulier.set(taak.formioFormulier);
    }
  }

  private async createHardCodedTaakForm(
    taak: GeneratedType<"RestTask">,
    zaak: GeneratedType<"RestZaak">,
  ) {
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
      });

      const explanationControl = this.formBuilder.control(taak.toelichting, [
        Validators.maxLength(1000),
      ]);
      this.form.addControl("toelichting", explanationControl);
      this.formFields.update((fields) => [
        ...fields,
        ...formFields,
        { type: "textarea", key: "toelichting" },
      ]);

      const allAttachments = [
        ...(taak.taakdocumenten ?? []),
        ...mapStringToDocumentenStrings(taak.taakdata?.bijlagen),
      ];
      const attachments = await this.queryClient.fetchQuery(
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
      this.formFields.update((fields) => [
        ...fields,
        {
          type: "documents",
          key: "bijlagen",
          readonly: true,
          options: attachments,
        },
      ]);
    } catch (e) {
      console.warn(e);
    }
  }

  protected isReadonly() {
    const taak = this.taak();
    return taak?.status === "AFGEROND" || !taak?.rechten.canWijzigen;
  }

  private setupMenu() {
    const menu: MenuItem[] = [new HeaderMenuItem("taak")];
    const zaak = this.zaak();

    if (this.taak()?.rechten.canToevoegenDocument) {
      menu.push(
        new ButtonMenuItem(
          "actie.document.toevoegen",
          () => this.actionsSidenav().open(),
          "upload_file",
        ),
      );

      if (
        zaak?.zaaktype.zaakafhandelparameters?.smartDocuments
          .isEnabledGlobally &&
        zaak?.zaaktype?.zaakafhandelparameters.smartDocuments
          .isEnabledForZaaktype
      ) {
        menu.push(
          new ButtonMenuItem(
            "actie.document.maken",
            () => {
              this.smartDocumentsGroupId.set(undefined);
              this.smartDocumentsTemplateId.set(undefined);
              this.actionsSidenav().open();
            },
            "note_add",
          ),
        );
      }
    }
    this.menu.set(menu);
  }

  private rereadTaak(taakId: string) {
    if (this.taakIdToReread() === taakId) {
      void this.rereadTaakQuery.refetch();
      return;
    }
    this.taakIdToReread.set(taakId);
  }

  private loadHistorie() {
    const taakId = this.taak()?.id;
    if (!taakId) return;

    if (this.historieTaakId() === taakId) {
      void this.historieQuery.refetch();
      return;
    }
    this.historieTaakId.set(taakId);
  }

  editTaak() {
    this.activeSideAction.set("actie.taak.wijzigen");
    this.actionsSidenav().open();
  }

  onHardCodedFormSubmit(formGroup: FormGroup, partial = false) {
    const taak = this.taak()!;
    const taskBody:
      | PutBody<"/rest/taken/taakdata">
      | PatchBody<"/rest/taken/complete"> = {
      ...taak,
      taakdata: {
        ...taak.taakdata,
        ...mapFormGroupToTaskData(formGroup, {
          ignoreKeys: ["bijlagen"],
        }),
      },
      toelichting: formGroup.get("toelichting")?.value,
      taakinformatie: {
        ...taak.taakinformatie,
        ...mapTaskdataToTaskInformation(
          mapFormGroupToTaskData(formGroup, {
            mapControlOptions: {
              documentKey: "titel",
              documentSeparator: ", ",
            },
          }),
          taak,
        ),
      },
    };

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
        void this.taakFormulierenService
          .getAngularTaskForm(task.formulierDefinitieId)
          .onTaskCompleted(task, this.form, this.formFields());
      },
    });
  }

  onFormioFormSubmit(submission: {
    data: Record<string, string>;
    state: string;
  }) {
    const taak = this.taak();
    for (const key in submission.data) {
      if (key !== "submit" && key !== "save" && taak?.taakdata) {
        taak.taakdata[key] = submission.data[key];
      }
    }
    if (!taak) return;

    // Re-initialise from the response so the view reflects whatever the flow decided, without waiting
    // for the websocket event to arrive.
    const onSuccess = (task: GeneratedType<"RestTask">) =>
      this.init(task, false);
    const onError = () => this.lastSubmitFailed.set(true);

    this.lastSubmitFailed.set(false);

    if (submission.state === "submitted") {
      this.completeTaakMutation.mutate(taak, { onSuccess, onError });
      return;
    }

    this.updateTaakdataMutation.mutate(taak, { onSuccess, onError });
  }

  onFormioFormChange(event: FormioChangeEvent) {
    this.formioSetupService.setFormioChangeData(event.data);
  }

  onDocumentCreate(event: FormioCustomEvent) {
    this.smartDocumentsGroupId.set(
      this.formioSetupService.extractSmartDocumentsGroupId(event),
    );
    this.smartDocumentsTemplateId.set(
      this.formioSetupService.extractSmartDocumentsTemplateId(event),
    );
    if (!this.smartDocumentsGroupId() || !this.smartDocumentsTemplateId()) {
      console.debug("No SmartDocuments template selected!");
      return;
    }

    this.activeSideAction.set("actie.document.maken");
    void this.actionsSidenav().open();
  }

  updateTaakdocumenten(
    informatieobject: GeneratedType<"RestEnkelvoudigInformatieobject">,
  ) {
    const taak = this.taak();
    if (!taak) return;

    if (!taak.taakdocumenten) {
      taak.taakdocumenten = [];
    }

    taak.taakdocumenten.push(informatieobject.uuid!);

    const control = this.form.get("bijlagen");
    if (!control) return;
    const newAttachments = [...(control.value ?? []), informatieobject];
    this.formFields().forEach((field) => {
      if (field.type === "documents" && field.key === "bijlagen") {
        field.options = newAttachments;
      }
    });
    control.setValue(newAttachments as never);
  }

  documentCreated() {
    void this.actionsSidenav().close();

    const taak = this.taak();
    if (!taak) return;
    const listener = this.websocketService.addListener(
      "UPDATED",
      "ZAAK_INFORMATIEOBJECTEN",
      taak.zaakUuid,
      () => {
        this.websocketService.removeListener(listener);
        this.loadHistorie();
      },
    );
  }

  documentMoveToCase(
    $event: Partial<GeneratedType<"RestEnkelvoudigInformatieobject">>,
  ) {
    this.activeSideAction.set("actie.document.verplaatsen");
    this.documentToMove.set($event);
    void this.actionsSidenav().open();
  }

  updateZaakDocumentList() {
    this.zaakDocumentenComponent()?.updateDocumentList();
  }

  protected updateZaak() {
    const zaakUuid = this.zaak()?.uuid ?? this.taak()?.zaakUuid;
    if (!zaakUuid) return;
    this.zakenService.readZaak(zaakUuid).subscribe((zaak) => {
      this.zaak.set(zaak);
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

    this.zaak.set({
      identificatie: taak.zaakIdentificatie,
      uuid: taak.zaakUuid,
      zaaktype,
    } satisfies Partial<
      GeneratedType<"RestZaak">
    > as GeneratedType<"RestZaak">);
  }
}

/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 Dimpact, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
  ViewChild,
} from "@angular/core";
import { FormGroup } from "@angular/forms";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import { MatSort } from "@angular/material/sort";
import { MatTableDataSource } from "@angular/material/table";
import { ActivatedRoute } from "@angular/router";
import { FormioForm } from "@formio/angular";
import { TranslateService } from "@ngx-translate/core";
import { ZaakDocumentenComponent } from "src/app/zaken/zaak-documenten/zaak-documenten.component";
import { UtilService } from "../../core/service/util.service";
import { ObjectType } from "../../core/websocket/model/object-type";
import { Opcode } from "../../core/websocket/model/opcode";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import {
  FormioChangeEvent,
  FormioCustomEvent,
} from "../../formulieren/formio-wrapper/formio-wrapper.component";
import { AbstractTaakFormulier } from "../../formulieren/taken/abstract-taak-formulier";
import { TaakFormulierenService } from "../../formulieren/taken/taak-formulieren.service";
import { IdentityService } from "../../identity/identity.service";
import { ActionsViewComponent } from "../../shared/abstract-view/actions-view-component";
import { TextIcon } from "../../shared/edit/text-icon";
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
  formulierDefinitie?: GeneratedType<"RESTFormulierDefinitie">;
  formioFormulier?: FormioForm;

  smartDocumentsGroupPath: string[] = [];
  smartDocumentsTemplateName?: string;
  smartDocumentsInformatieobjecttypeUuid?: string;

  menu: MenuItem[] = [];
  activeSideAction: string | null = null;
  documentToMove!: Partial<GeneratedType<"RestEnkelvoudigInformatieobject">>;

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

  editFormFields = new Map<string, unknown>();
  fataledatumIcon: TextIcon | null = null;
  protected initialized = false;

  private taakListener?: WebsocketListener;
  private ingelogdeMedewerker?: GeneratedType<"RestLoggedInUser">;
  readonly TaakStatusAfgerond =
    "AFGEROND" satisfies GeneratedType<"TaakStatus">;

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
  ) {
    super();
  }

  ngOnInit() {
    this.getIngelogdeMedewerker();
    this.route.data.subscribe((data) => {
      this.createZaakFromTaak(data.taak);
      this.init(data.taak, true);

      this.taakListener = this.websocketService.addListenerWithSnackbar(
        Opcode.ANY,
        ObjectType.TAAK,
        data.taak.id,
        (event) => {
          this.reloadTaak(event.objectId.resource);
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
    this.createTaakForm(taak, this.zaak!);
  }

  private init(taak: GeneratedType<"RestTask">, initZaak: boolean) {
    this.initTaakGegevens(taak);

    if (!initZaak) return;

    this.zakenService.readZaak(taak.zaakUuid).subscribe((zaak) => {
      this.zaak = zaak;
      this.initTaakGegevens(taak);
      this.initialized = true;
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
      this.createHardCodedTaakForm(taak, zaak);
    } else if (taak.formulierDefinitie) {
      this.createConfigurableTaakForm(taak.formulierDefinitie);
    } else if (!this.formioFormulier) {
      this.formioFormulier = taak.formioFormulier ?? undefined;
      if (!this.formioFormulier) return;
      this.formioSetupService.createFormioForm(this.formioFormulier, taak);
    }
  }

  private createHardCodedTaakForm(
    taak: GeneratedType<"RestTask">,
    zaak: GeneratedType<"RestZaak">,
  ) {
    if (this.taak?.status !== "AFGEROND" && this.taak?.rechten.wijzigen) {
      this.formConfig = new FormConfigBuilder()
        .partialText("actie.opslaan")
        .saveText("actie.opslaan.afronden")
        .build();
    } else {
      this.formConfig = null;
    }

    this.formulier = this.taakFormulierenService
      .getFormulierBuilder(
        this.taak?.formulierDefinitieId as GeneratedType<"FormulierDefinitie">,
      )
      .behandelForm(taak, zaak)
      .build();
    if (this.formulier.disablePartialSave && this.formConfig) {
      this.formConfig.partialButtonText = null;
    }
    this.utilService.setTitle("title.taak", {
      taak: this.formulier.getBehandelTitel(),
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

  isReadonly() {
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

  onHardCodedFormPartial(formGroup: FormGroup) {
    if (!this.formulier) return;

    this.takenService
      .updateTaakdata(this.formulier.getTaak(formGroup))
      .subscribe(() => {
        this.utilService.openSnackbar("msg.taak.opgeslagen");
      });
  }

  onHardCodedFormSubmit(formGroup?: FormGroup) {
    if (!formGroup) return;
    if (!this.formulier) return;

    this.takenService
      .complete(this.formulier.getTaak(formGroup))
      .subscribe(() => {
        this.utilService.openSnackbar("msg.taak.afgerond");
      });
  }

  onConfigurableFormPartial(formState?: Record<string, string>) {
    if (!formState) return;
    if (!this.taak) return;

    this.taak.taakdata = formState;
    this.takenService.updateTaakdata(this.taak).subscribe(() => {
      this.utilService.openSnackbar("msg.taak.opgeslagen");
    });
  }

  onConfigurableFormSubmit(formState?: Record<string, string>) {
    if (!formState) return;
    if (!this.taak) return;

    this.taak.taakdata = formState;
    this.takenService.complete(this.taak).subscribe(() => {
      this.utilService.openSnackbar("msg.taak.afgerond");
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
      this.takenService.complete(this.taak).subscribe(() => {
        this.utilService.openSnackbar("msg.taak.afgerond");
      });
      return;
    }

    this.takenService.updateTaakdata(this.taak).subscribe(() => {
      this.utilService.openSnackbar("msg.taak.opgeslagen");
    });
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

  // TODO add the correct type
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  editToewijzing(event: any) {
    if (
      event["medewerker-groep"].medewerker &&
      event["medewerker-groep"].medewerker.id ===
        this.ingelogdeMedewerker?.id &&
      this.taak?.groep === event["medewerker-groep"].groep
    ) {
      this.assignToMe();
      return;
    }

    if (!this.taak) return;

    this.taak.groep = event["medewerker-groep"].groep;
    this.taak.behandelaar = event["medewerker-groep"].medewerker;

    this.takenService
      .toekennen({
        taakId: this.taak.id!,
        zaakUuid: this.taak.zaakUuid,
        groepId: this.taak.groep!.id!,
        behandelaarId: this.taak.behandelaar?.id,
        reden: event["reden"],
      })
      .subscribe(() => {
        if (this.taak?.behandelaar) {
          this.utilService.openSnackbar("msg.taak.toegekend", {
            behandelaar: this.taak?.behandelaar?.naam,
          });
        } else {
          this.utilService.openSnackbar("msg.vrijgegeven.taak");
        }
      });
  }

  private reloadTaak(taakId: string) {
    this.takenService.readTaak(taakId).subscribe((taak) => {
      this.init(taak, true);
    });
  }

  private getIngelogdeMedewerker() {
    this.identityService.readLoggedInUser().subscribe((ingelogdeMedewerker) => {
      this.ingelogdeMedewerker = ingelogdeMedewerker;
    });
  }

  private assignToMe() {
    if (!this.taak) return;

    this.takenService
      .toekennenAanIngelogdeMedewerker({
        taakId: this.taak.id!,
        zaakUuid: this.taak.zaakUuid,
        groepId: null as unknown as string,
      })
      .subscribe((taak) => {
        this.utilService.openSnackbar("msg.taak.toegekend", {
          behandelaar: taak.behandelaar?.naam,
        });
      });
  }

  updateTaakdocumenten(
    informatieobject: GeneratedType<"RestEnkelvoudigInformatieobject">,
  ) {
    if (!this.taak) return;

    if (!this.taak.taakdocumenten) {
      this.taak.taakdocumenten = [];
    }

    this.taak.taakdocumenten.push(informatieobject.uuid!);
    if (!this.formulier) return;
    this.formulier.refreshTaakdocumentenEnBijlagen();
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

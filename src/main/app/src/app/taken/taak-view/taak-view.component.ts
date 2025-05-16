/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 Dimpact, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
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
import { ExtendedComponentSchema, FormioForm } from "@formio/angular";
import { TranslateService } from "@ngx-translate/core";
import { lastValueFrom } from "rxjs";
import { tap } from "rxjs/operators";
import { ZaakDocumentenComponent } from "src/app/zaken/zaak-documenten/zaak-documenten.component";
import { FormulierDefinitie } from "../../admin/model/formulieren/formulier-definitie";
import { ZaakafhandelParametersService } from "../../admin/zaakafhandel-parameters.service";
import { UtilService } from "../../core/service/util.service";
import { ObjectType } from "../../core/websocket/model/object-type";
import { Opcode } from "../../core/websocket/model/opcode";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { FormioCustomEvent } from "../../formulieren/formio-wrapper/formio-wrapper.component";
import { AbstractTaakFormulier } from "../../formulieren/taken/abstract-taak-formulier";
import { TaakFormulierenService } from "../../formulieren/taken/taak-formulieren.service";
import { IdentityService } from "../../identity/identity.service";
import { ActionsViewComponent } from "../../shared/abstract-view/actions-view-component";
import { TextIcon } from "../../shared/edit/text-icon";
import { TaakHistorieRegel } from "../../shared/historie/model/taak-historie-regel";
import { InputFormFieldBuilder } from "../../shared/material-form-builder/form-components/input/input-form-field-builder";
import { MedewerkerGroepFieldBuilder } from "../../shared/material-form-builder/form-components/medewerker-groep/medewerker-groep-field-builder";
import { TextareaFormFieldBuilder } from "../../shared/material-form-builder/form-components/textarea/textarea-form-field-builder";
import { FormConfig } from "../../shared/material-form-builder/model/form-config";
import { FormConfigBuilder } from "../../shared/material-form-builder/model/form-config-builder";
import { OrderUtil } from "../../shared/order/order-util";
import { ButtonMenuItem } from "../../shared/side-nav/menu-item/button-menu-item";
import { HeaderMenuItem } from "../../shared/side-nav/menu-item/header-menu-item";
import { MenuItem } from "../../shared/side-nav/menu-item/menu-item";
import { DateConditionals } from "../../shared/utils/date-conditionals";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../../zaken/zaken.service";
import { Taak } from "../model/taak";
import { TaakStatus } from "../model/taak-status.enum";
import { TakenService } from "../taken.service";

@Component({
  templateUrl: "./taak-view.component.html",
  styleUrls: ["./taak-view.component.less"],
})
export class TaakViewComponent
  extends ActionsViewComponent
  implements OnInit, AfterViewInit, OnDestroy
{
  @ViewChild("actionsSidenav") actionsSidenav!: MatSidenav;
  @ViewChild("menuSidenav") menuSidenav!: MatSidenav;
  @ViewChild("sideNavContainer") sideNavContainer!: MatSidenavContainer;
  @ViewChild("historieSort") historieSort!: MatSort;
  @ViewChild("zaakDocumentenComponent")
  zaakDocumentenComponent!: ZaakDocumentenComponent;

  taak: Taak;
  zaak: GeneratedType<"RestZaak">;
  formulier: AbstractTaakFormulier;
  formConfig: FormConfig;
  formulierDefinitie: FormulierDefinitie;
  formioFormulier: Record<string, any> = {};
  formioChangeData;

  smartDocumentsGroupPath: string[];
  smartDocumentsTemplateName: string;
  smartDocumentsInformatieObjectTypeUUID: string;

  menu: MenuItem[] = [];
  activeSideAction: string | null = null;
  documentToMove!: Partial<GeneratedType<"RestEnkelvoudigInformatieobject">>;

  historieSrc: MatTableDataSource<TaakHistorieRegel> =
    new MatTableDataSource<TaakHistorieRegel>();
  historieColumns: string[] = [
    "datum",
    "wijziging",
    "oudeWaarde",
    "nieuweWaarde",
    "toelichting",
  ];

  editFormFields: Map<string, any> = new Map<string, any>();
  fataledatumIcon: TextIcon;
  initialized = false;

  posts = 0;
  private taakListener: WebsocketListener;
  private ingelogdeMedewerker: GeneratedType<"RestLoggedInUser">;
  readonly TaakStatusAfgerond = TaakStatus.Afgerond;

  constructor(
    private route: ActivatedRoute,
    private takenService: TakenService,
    private zakenService: ZakenService,
    public utilService: UtilService,
    private websocketService: WebsocketService,
    private taakFormulierenService: TaakFormulierenService,
    private identityService: IdentityService,
    private zaakafhandelParametersService: ZaakafhandelParametersService,
    protected translate: TranslateService,
  ) {
    super();
  }

  ngOnInit(): void {
    this.getIngelogdeMedewerker();
    this.route.data.subscribe((data) => {
      this.createZaakFromTaak(data.taak);
      this.init(data.taak, true);
    });
  }

  ngAfterViewInit(): void {
    super.ngAfterViewInit();

    this.taakListener = this.websocketService.addListenerWithSnackbar(
      Opcode.ANY,
      ObjectType.TAAK,
      this.taak.id,
      () => this.reloadTaak(),
    );

    this.historieSrc.sortingDataAccessor = (item, property) => {
      switch (property) {
        case "datum":
          return item.datumTijd;
        default:
          return item[property];
      }
    };
    this.historieSrc.sort = this.historieSort;
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.websocketService.removeListener(this.taakListener);
  }

  private initTaakGegevens(taak: Taak): void {
    this.taak = taak;
    this.loadHistorie();
    this.setEditableFormFields();
    this.setupMenu();
  }

  private init(taak: Taak, initZaak: boolean): void {
    this.initTaakGegevens(taak);
    if (initZaak) {
      this.zakenService.readZaak(this.taak.zaakUuid).subscribe((zaak) => {
        this.zaak = zaak;
        this.initialized = true;
        this.setupMenu();
        this.createTaakForm(taak, zaak);
      });
    } else {
      this.createTaakForm(taak, this.zaak);
    }
  }

  private createTaakForm(taak: Taak, zaak: GeneratedType<"RestZaak">): void {
    if (taak.formulierDefinitieId) {
      this.createHardCodedTaakForm(taak, zaak);
    } else if (taak.formulierDefinitie) {
      this.createConfigurableTaakForm(taak.formulierDefinitie);
    } else {
      this.createFormioForm(taak.formioFormulier);
    }
  }

  private createHardCodedTaakForm(
    taak: Taak,
    zaak: GeneratedType<"RestZaak">,
  ): void {
    if (
      this.taak.status !== TaakStatus.Afgerond &&
      this.taak.rechten.wijzigen
    ) {
      this.formConfig = new FormConfigBuilder()
        .partialText("actie.opslaan")
        .saveText("actie.opslaan.afronden")
        .build();
    } else {
      this.formConfig = null;
    }

    this.formulier = this.taakFormulierenService
      .getFormulierBuilder(this.taak.formulierDefinitieId)
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
    formulierDefinitie: FormulierDefinitie,
  ): void {
    this.formulierDefinitie = formulierDefinitie;
    this.utilService.setTitle("title.taak", {
      taak: formulierDefinitie.naam,
    });
  }

  private createFormioForm(formioFormulier: FormioForm): void {
    this.formioFormulier = formioFormulier;
    this.initializeSpecializedFormioComponents(formioFormulier.components);
    this.utilService.setTitle("title.taak", {
      taak: formioFormulier.title,
    });
  }

  private initializeSpecializedFormioComponents(
    components: ExtendedComponentSchema[] | undefined,
  ): void {
    components?.forEach((component) => {
      switch (component.type) {
        case "groepMedewerkerFieldset":
          this.initializeFormioGroepMedewerkerFieldsetComponent(component);
          break;
        case "groepSmartDocumentsFieldset":
          this.initializeFormioGroepSmartDocumentsFieldsetComponent(component);
          break;
      }
      if ("components" in component) {
        this.initializeSpecializedFormioComponents(component.components);
      }
    });
  }

  private initializeFormioGroepMedewerkerFieldsetComponent(
    component: ExtendedComponentSchema,
  ): void {
    component.type = "fieldset";
    const groepComponent = component.components[0];
    const medewerkerComponent = component.components[1];
    this.initializeFormioGroepMedewerkerFieldsetGroepComponent(groepComponent);
    this.initializeFormioGroepMedewerkerFieldsetMedewerkerComponent(
      medewerkerComponent,
      groepComponent.key,
    );
  }

  private initializeFormioGroepMedewerkerFieldsetGroepComponent(
    groepComponent: ExtendedComponentSchema,
  ): void {
    groepComponent.valueProperty = "id";
    groepComponent.template = "{{ item.naam }}";
    groepComponent.data = {
      custom: () =>
        lastValueFrom(
          this.identityService
            .listGroups(this.taak.zaaktypeUUID)
            .pipe(tap((value) => value.sort(OrderUtil.orderBy("naam")))),
        ),
    };
  }

  private initializeFormioGroepMedewerkerFieldsetMedewerkerComponent(
    medewerkerComponent: ExtendedComponentSchema,
    groepComponentKey: string,
  ): void {
    medewerkerComponent.valueProperty = "id";
    medewerkerComponent.template = "{{ item.naam }}";
    medewerkerComponent.data = {
      custom: () => {
        if (
          this.formioChangeData &&
          groepComponentKey in this.formioChangeData &&
          this.formioChangeData[groepComponentKey] !== ""
        ) {
          return lastValueFrom(
            this.identityService
              .listUsersInGroup(this.formioChangeData[groepComponentKey])
              .pipe(tap((value) => value.sort(OrderUtil.orderBy("naam")))),
          );
        } else {
          return Promise.resolve([]);
        }
      },
    };
  }

  private initializeFormioGroepSmartDocumentsFieldsetComponent(
    component: ExtendedComponentSchema,
  ): void {
    component.type = "fieldset";
    const smartDocumentsPath: GeneratedType<"RestSmartDocumentsPath"> = {
      path: this.formioGetSmartDocumentsGroups(component),
    };

    const smartDocumentsTemplateComponent = component.components[0];
    smartDocumentsTemplateComponent.valueProperty = "id";
    smartDocumentsTemplateComponent.template = "{{ item.naam }}";
    smartDocumentsTemplateComponent.data = {
      custom: () =>
        lastValueFrom(
          this.zaakafhandelParametersService
            .listSmartDocumentsGroupTemplateNames(smartDocumentsPath)
            .pipe(tap((value) => value.sort())),
        ),
    };
  }

  private formioGetSmartDocumentsGroups(
    component: ExtendedComponentSchema,
  ): string[] {
    return component.properties["SmartDocuments_Group"].split("/");
  }

  isReadonly() {
    return (
      this.taak.status === TaakStatus.Afgerond || !this.taak.rechten.wijzigen
    );
  }

  private setEditableFormFields(): void {
    this.editFormFields.set(
      "medewerker-groep",
      new MedewerkerGroepFieldBuilder(this.taak.groep, this.taak.behandelaar)
        .id("medewerker-groep")
        .groepLabel("groep.-kies-")
        .groepRequired()
        .medewerkerLabel("behandelaar.-kies-")
        .setZaaktypeUuid(this.taak.zaaktypeUUID)
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

    this.fataledatumIcon = new TextIcon(
      DateConditionals.provideFormControlValue(DateConditionals.isExceeded),
      "report_problem",
      "errorTaakVerlopen_icon",
      "msg.datum.overschreden",
      "error",
    );
  }

  private setupMenu(): void {
    this.menu = [];
    this.menu.push(new HeaderMenuItem("taak"));

    if (this.taak.rechten.toevoegenDocument) {
      this.menu.push(
        new ButtonMenuItem(
          "actie.document.toevoegen",
          () => this.actionsSidenav.open(),
          "upload_file",
        ),
      );

      if (
        this.zaak.zaaktype.zaakafhandelparameters?.smartDocuments
          .enabledGlobally &&
        this.zaak.zaaktype?.zaakafhandelparameters.smartDocuments
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

  private loadHistorie(): void {
    this.takenService
      .listHistorieVoorTaak(this.taak.id)
      .subscribe((historie) => {
        this.historieSrc.data = historie;
      });
  }

  onHardCodedFormPartial(formGroup: FormGroup): void {
    this.websocketService.suspendListener(this.taakListener);
    this.takenService
      .updateTaakdata(this.formulier.getTaak(formGroup))
      .subscribe((taak) => {
        this.utilService.openSnackbar("msg.taak.opgeslagen");
        this.init(taak, false);
        this.posts++;
      });
  }

  onHardCodedFormSubmit(formGroup: FormGroup): void {
    if (formGroup) {
      this.websocketService.suspendListener(this.taakListener);
      this.takenService
        .complete(this.formulier.getTaak(formGroup))
        .subscribe((taak) => {
          this.utilService.openSnackbar("msg.taak.afgerond");
          this.init(taak, false);
        });
    }
  }

  onConfigurableFormPartial(formState?: Record<string, string>): void {
    if (formState) {
      this.websocketService.suspendListener(this.taakListener);
      this.taak.taakdata = formState;
      this.takenService.updateTaakdata(this.taak).subscribe((taak) => {
        this.utilService.openSnackbar("msg.taak.opgeslagen");
        this.init(taak, false);
        this.posts++;
      });
    }
  }

  onConfigurableFormSubmit(formState?: Record<string, string>): void {
    if (formState) {
      this.websocketService.suspendListener(this.taakListener);
      this.taak.taakdata = formState;
      this.takenService.complete(this.taak).subscribe((taak) => {
        this.utilService.openSnackbar("msg.taak.afgerond");
        this.init(taak, true);
      });
    }
  }

  onFormioFormSubmit(submission: any) {
    this.websocketService.suspendListener(this.taakListener);
    for (const key in submission.data) {
      if (key !== "submit" && key !== "save") {
        this.taak.taakdata[key] = submission.data[key];
      }
    }
    if (submission.state === "submitted") {
      this.takenService.complete(this.taak).subscribe((taak) => {
        this.utilService.openSnackbar("msg.taak.afgerond");
        this.init(taak, true);
      });
    } else {
      this.takenService.updateTaakdata(this.taak).subscribe((taak) => {
        this.utilService.openSnackbar("msg.taak.opgeslagen");
        this.init(taak, false);
        this.posts++;
      });
    }
  }

  onFormioFormChange(event: any) {
    this.formioChangeData = event.data;
  }

  editToewijzing(event: any) {
    if (
      event["medewerker-groep"].medewerker &&
      event["medewerker-groep"].medewerker.id === this.ingelogdeMedewerker.id &&
      this.taak.groep === event["medewerker-groep"].groep
    ) {
      this.assignToMe();
    } else {
      this.taak.groep = event["medewerker-groep"].groep;
      this.taak.behandelaar = event["medewerker-groep"].medewerker;
      const reden: string = event["reden"];
      this.websocketService.suspendListener(this.taakListener);
      this.takenService.toekennen(this.taak, reden).subscribe(() => {
        if (this.taak.behandelaar) {
          this.utilService.openSnackbar("msg.taak.toegekend", {
            behandelaar: this.taak.behandelaar?.naam,
          });
        } else {
          this.utilService.openSnackbar("msg.vrijgegeven.taak");
        }
        this.init(this.taak, false);
      });
    }
  }

  private reloadTaak() {
    this.takenService.readTaak(this.taak.id).subscribe((taak) => {
      this.init(taak, true);
    });
  }

  private getIngelogdeMedewerker() {
    this.identityService.readLoggedInUser().subscribe((ingelogdeMedewerker) => {
      this.ingelogdeMedewerker = ingelogdeMedewerker;
    });
  }

  private assignToMe(): void {
    this.websocketService.suspendListener(this.taakListener);
    this.takenService
      .toekennenAanIngelogdeMedewerker(this.taak)
      .subscribe((taak) => {
        this.taak.behandelaar = taak.behandelaar;
        this.utilService.openSnackbar("msg.taak.toegekend", {
          behandelaar: taak.behandelaar?.naam,
        });
        this.init(this.taak, false);
      });
  }

  updateTaakdocumenten(
    informatieobject: GeneratedType<"RestEnkelvoudigInformatieobject">,
  ) {
    if (!this.taak.taakdocumenten) {
      this.taak.taakdocumenten = [];
    }

    this.taak.taakdocumenten.push(informatieobject.uuid);
    this.formulier.refreshTaakdocumentenEnBijlagen();
  }

  documentCreated(): void {
    void this.actionsSidenav.close();

    const listener = this.websocketService.addListener(
      Opcode.UPDATED,
      ObjectType.ZAAK_INFORMATIEOBJECTEN,
      this.taak.zaakUuid,
      () => {
        this.websocketService.removeListener(listener);
        this.reloadTaak();
      },
    );
  }

  documentMoveToCase(
    $event: Partial<GeneratedType<"RestEnkelvoudigInformatieobject">>,
  ): void {
    this.activeSideAction = "actie.document.verplaatsen";
    this.documentToMove = $event;
    this.actionsSidenav.open();
  }

  updateZaakDocumentList(): void {
    this.zaakDocumentenComponent.updateDocumentList();
  }

  /**
   *  Zaak is nog niet geladen, beschikbare zaak-data uit de taak vast weergeven totdat de zaak is geladen
   */
  private createZaakFromTaak(taak: Taak): void {
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

  onDocumentCreate(event: FormioCustomEvent) {
    const parent = event.component.parent;
    this.activeSideAction = "actie.document.maken";
    this.smartDocumentsGroupPath = this.formioGetSmartDocumentsGroups(parent);
    this.smartDocumentsInformatieObjectTypeUUID =
      parent.properties["SmartDocuments_InformatieObjectTypeUUID"];
    this.smartDocumentsTemplateName =
      event.data[parent.key + "_Template"].toString();
    this.actionsSidenav.open();
  }
}

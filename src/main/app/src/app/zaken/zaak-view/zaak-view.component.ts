/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024-2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  Component,
  OnDestroy,
  OnInit,
  ViewChild,
} from "@angular/core";
import { FormControl, Validators } from "@angular/forms";
import { MatDialog } from "@angular/material/dialog";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import { MatSort } from "@angular/material/sort";
import { MatTableDataSource } from "@angular/material/table";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import moment from "moment";
import { forkJoin } from "rxjs";
import { map, tap } from "rxjs/operators";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "src/app/shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { DateConditionals } from "src/app/shared/utils/date-conditionals";
import { ReferentieTabelService } from "../../admin/referentie-tabel.service";
import { ZaakafhandelParametersService } from "../../admin/zaakafhandel-parameters.service";
import { BAGService } from "../../bag/bag.service";
import { BAGObject } from "../../bag/model/bagobject";
import { BAGObjectGegevens } from "../../bag/model/bagobject-gegevens";
import { UtilService } from "../../core/service/util.service";
import { ObjectType } from "../../core/websocket/model/object-type";
import { Opcode } from "../../core/websocket/model/opcode";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { KlantenService } from "../../klanten/klanten.service";
import { Klant } from "../../klanten/model/klanten/klant";
import { KlantGegevens } from "../../klanten/model/klanten/klant-gegevens";
import { ViewResourceUtil } from "../../locatie/view-resource.util";
import { NotitieType } from "../../notities/model/notitietype.enum";
import { PlanItem } from "../../plan-items/model/plan-item";
import { UserEventListenerActie } from "../../plan-items/model/user-event-listener-actie-enum";
import { PlanItemsService } from "../../plan-items/plan-items.service";
import { ActionsViewComponent } from "../../shared/abstract-view/actions-view-component";
import { detailExpand } from "../../shared/animations/animations";
import { DialogData } from "../../shared/dialog/dialog-data";
import { DialogComponent } from "../../shared/dialog/dialog.component";
import { ExpandableTableData } from "../../shared/dynamic-table/model/expandable-table-data";
import { TextIcon } from "../../shared/edit/text-icon";
import { HistorieRegel } from "../../shared/historie/model/historie-regel";
import { IndicatiesLayout } from "../../shared/indicaties/indicaties.component";
import { InputFormFieldBuilder } from "../../shared/material-form-builder/form-components/input/input-form-field-builder";
import { ReadonlyFormFieldBuilder } from "../../shared/material-form-builder/form-components/readonly/readonly-form-field-builder";
import { SelectFormFieldBuilder } from "../../shared/material-form-builder/form-components/select/select-form-field-builder";
import { TextareaFormFieldBuilder } from "../../shared/material-form-builder/form-components/textarea/textarea-form-field-builder";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { ButtonMenuItem } from "../../shared/side-nav/menu-item/button-menu-item";
import { HeaderMenuItem } from "../../shared/side-nav/menu-item/header-menu-item";
import { MenuItem } from "../../shared/side-nav/menu-item/menu-item";
import { SessionStorageUtil } from "../../shared/storage/session-storage.util";
import { GeneratedType } from "../../shared/utils/generated-types";
import { Taak } from "../../taken/model/taak";
import { TaakStatus } from "../../taken/model/taak-status.enum";
import { TakenService } from "../../taken/taken.service";
import { IntakeAfrondenDialogComponent } from "../intake-afronden-dialog/intake-afronden-dialog.component";
import { GeometryGegevens } from "../model/geometry-gegevens";
import { Zaak } from "../model/zaak";
import { ZaakBetrokkene } from "../model/zaak-betrokkene";
import { ZaakAfhandelenDialogComponent } from "../zaak-afhandelen-dialog/zaak-afhandelen-dialog.component";
import { ZaakKoppelenService } from "../zaak-koppelen/zaak-koppelen.service";
import { ZaakOntkoppelenDialogComponent } from "../zaak-ontkoppelen/zaak-ontkoppelen-dialog.component";
import { ZaakOpschortenDialogComponent } from "../zaak-opschorten-dialog/zaak-opschorten-dialog.component";
import { ZaakVerlengenDialogComponent } from "../zaak-verlengen-dialog/zaak-verlengen-dialog.component";
import { ZakenService } from "../zaken.service";

@Component({
  templateUrl: "./zaak-view.component.html",
  styleUrls: ["./zaak-view.component.less"],
  animations: [detailExpand],
})
export class ZaakViewComponent
  extends ActionsViewComponent
  implements OnInit, AfterViewInit, OnDestroy
{
  readonly indicatiesLayout = IndicatiesLayout;
  zaak: Zaak;
  zaakOpschorting: GeneratedType<"RESTZaakOpschorting">;
  actiefPlanItem: PlanItem;
  menu: MenuItem[];
  activeSideAction: string | null = null;
  teWijzigenBesluit: GeneratedType<"RestDecision">;

  takenDataSource = new MatTableDataSource<ExpandableTableData<Taak>>();
  allTakenExpanded = false;
  toonAfgerondeTaken = new FormControl(false);
  takenFilter: any = {};
  takenLoading = false;
  takenColumnsToDisplay: string[] = [
    "naam",
    "status",
    "creatiedatumTijd",
    "fataledatum",
    "groep",
    "behandelaar",
    "id",
  ];

  historie = new MatTableDataSource<HistorieRegel>();
  historieColumns: string[] = [
    "datum",
    "gebruiker",
    "wijziging",
    "actie",
    "oudeWaarde",
    "nieuweWaarde",
    "toelichting",
  ];
  betrokkenen = new MatTableDataSource<ZaakBetrokkene>();
  betrokkenenColumns: string[] = [
    "roltype",
    "betrokkenegegevens",
    "betrokkeneidentificatie",
    "roltoelichting",
    "actions",
  ];
  bagObjectenDataSource = new MatTableDataSource<BAGObjectGegevens>();
  gekoppeldeBagObjecten: BAGObject[];
  bagObjectenColumns: string[] = [
    "identificatie",
    "type",
    "omschrijving",
    "actions",
  ];
  gerelateerdeZaakColumns: string[] = [
    "identificatie",
    "zaaktypeOmschrijving",
    "statustypeOmschrijving",
    "startdatum",
    "relatieType",
  ];
  notitieType = NotitieType.ZAAK;
  editFormFields = new Map<string, any>();
  dateFieldIcon = new Map<string, TextIcon>();
  viewInitialized = false;
  toolTipIcon = new TextIcon(
    DateConditionals.provideFormControlValue(DateConditionals.always),
    "info",
    "toolTip_icon",
    "",
    "pointer",
    true,
  );
  loggedInUser: GeneratedType<"RestLoggedInUser">;

  private zaakListener: WebsocketListener;
  private zaakRollenListener: WebsocketListener;
  private zaakBesluitenListener: WebsocketListener;
  private zaakTakenListener: WebsocketListener;
  private datumPipe = new DatumPipe("nl");

  @ViewChild("actionsSidenav") actionsSidenav: MatSidenav;
  @ViewChild("menuSidenav") menuSidenav: MatSidenav;
  @ViewChild("sideNavContainer") sideNavContainer: MatSidenavContainer;

  @ViewChild("historieSort") historieSort: MatSort;
  @ViewChild("takenSort") takenSort: MatSort;

  constructor(
    private takenService: TakenService,
    private zakenService: ZakenService,
    private identityService: IdentityService,
    private planItemsService: PlanItemsService,
    private klantenService: KlantenService,
    private zaakafhandelParametersService: ZaakafhandelParametersService,
    private route: ActivatedRoute,
    private utilService: UtilService,
    private websocketService: WebsocketService,
    private dialog: MatDialog,
    private translate: TranslateService,
    private zaakKoppelenService: ZaakKoppelenService,
    private bagService: BAGService,
    private referentieTabelService: ReferentieTabelService,
    private vertrouwelijkaanduidingToTranslationKeyPipe: VertrouwelijkaanduidingToTranslationKeyPipe,
  ) {
    super();
  }

  ngOnInit(): void {
    this.subscriptions$.push(
      this.route.data.subscribe((data) => {
        this.init(data["zaak"]);

        this.zaakListener = this.websocketService.addListenerWithSnackbar(
          Opcode.ANY,
          ObjectType.ZAAK,
          this.zaak.uuid,
          () => this.updateZaak(),
        );

        this.zaakRollenListener = this.websocketService.addListenerWithSnackbar(
          Opcode.UPDATED,
          ObjectType.ZAAK_ROLLEN,
          this.zaak.uuid,
          () => this.updateZaak(),
        );

        this.zaakBesluitenListener =
          this.websocketService.addListenerWithSnackbar(
            Opcode.UPDATED,
            ObjectType.ZAAK_BESLUITEN,
            this.zaak.uuid,
            () => this.loadBesluiten(),
          );

        this.zaakTakenListener = this.websocketService.addListener(
          Opcode.UPDATED,
          ObjectType.ZAAK_TAKEN,
          this.zaak.uuid,
          () => this.loadTaken(),
        );

        this.utilService.setTitle("title.zaak", {
          zaak: this.zaak.identificatie,
        });

        this.getIngelogdeMedewerker();
        this.loadTaken();
      }),
    );

    this.takenDataSource.filterPredicate = (
      data: ExpandableTableData<Taak>,
      filter: string,
    ): boolean => {
      return !this.toonAfgerondeTaken.value
        ? data.data.status !== filter["status"]
        : true;
    };

    this.toonAfgerondeTaken.setValue(
      Boolean(SessionStorageUtil.getItem("toonAfgerondeTaken")),
    );
  }

  init(zaak: Zaak): void {
    this.zaak = zaak;
    this.utilService.disableActionBar(!zaak.rechten.wijzigen);
    this.loadHistorie();
    this.loadBetrokkenen();
    this.loadBagObjecten();
    this.setupMenu();
    this.loadOpschorting();
    this.setDateFieldIconSet();
    ViewResourceUtil.actieveZaak = zaak;
  }

  private getIngelogdeMedewerker() {
    this.identityService.readLoggedInUser().subscribe((loggedInUser) => {
      this.loggedInUser = loggedInUser;
    });
  }

  ngAfterViewInit() {
    this.viewInitialized = true;
    super.ngAfterViewInit();

    this.takenDataSource.sortingDataAccessor = (item, property) => {
      switch (property) {
        case "groep":
          return item.data.groep.naam;
        case "behandelaar":
          return item.data.behandelaar.naam;
        default:
          return item.data[property];
      }
    };
    this.takenDataSource.sort = this.takenSort;

    this.historie.sortingDataAccessor = (item, property) => {
      switch (property) {
        case "datum":
          return item.datumTijd;
        case "gebruiker":
          return item.door;
        default:
          return item[property];
      }
    };
    this.historie.sort = this.historieSort;
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    ViewResourceUtil.actieveZaak = null;
    this.utilService.disableActionBar(false);
    this.websocketService.removeListener(this.zaakListener);
    this.websocketService.removeListener(this.zaakBesluitenListener);
    this.websocketService.removeListener(this.zaakRollenListener);
    this.websocketService.removeListener(this.zaakTakenListener);
  }

  private setDateFieldIconSet() {
    this.dateFieldIcon.set(
      "einddatumGepland",
      new TextIcon(
        DateConditionals.provideFormControlValue(
          DateConditionals.isExceeded,
          this.zaak.einddatum,
        ),
        "report_problem",
        "warningVerlopen_icon",
        "msg.datum.overschreden",
        "warning",
      ),
    );
    this.dateFieldIcon.set(
      "uiterlijkeEinddatumAfdoening",
      new TextIcon(
        DateConditionals.provideFormControlValue(DateConditionals.isExceeded),
        "report_problem",
        "errorVerlopen_icon",
        "msg.datum.overschreden",
        "error",
      ),
    );
  }

  private createUserEventListenerPlanItemMenuItem(
    userEventListenerPlanItem: PlanItem,
  ): MenuItem {
    return new ButtonMenuItem(
      "planitem." + userEventListenerPlanItem.userEventListenerActie,
      () => this.openPlanItemStartenDialog(userEventListenerPlanItem),
      this.getuserEventListenerPlanItemMenuItemIcon(
        userEventListenerPlanItem.userEventListenerActie,
      ),
    );
  }

  private createPlanItemMenuItem(planItem: PlanItem, icon: string): MenuItem {
    return new ButtonMenuItem(
      planItem.naam,
      () => {
        if (!this.actiefPlanItem || this.actiefPlanItem.id !== planItem.id) {
          this.activeSideAction = null;
          this.planItemsService
            .readHumanTaskPlanItem(planItem.id)
            .subscribe((planItem) => {
              this.actiefPlanItem = planItem;
              this.activeSideAction = planItem.naam;
              this.actionsSidenav.open();
            });
        } else {
          this.activeSideAction = planItem.naam;
          this.actionsSidenav.open();
        }
      },
      icon,
    );
  }

  private getuserEventListenerPlanItemMenuItemIcon(
    userEventListenerActie: UserEventListenerActie,
  ): string {
    switch (userEventListenerActie) {
      case UserEventListenerActie.IntakeAfronden:
        return "thumbs_up_down";
      case UserEventListenerActie.ZaakAfhandelen:
        return "thumb_up_alt";
      default:
        return "fact_check";
    }
  }

  private setupMenu(): void {
    this.menu = [new HeaderMenuItem("zaak")];

    if (this.zaak.rechten.behandelen && !this.zaak.isProcesGestuurd) {
      if (
        !this.zaak.isOntvangstbevestigingVerstuurd &&
        this.zaak.rechten.versturenOntvangstbevestiging
      ) {
        this.menu.push(
          new ButtonMenuItem(
            "actie.ontvangstbevestiging.versturen",
            () => this.actionsSidenav.open(),
            "mark_email_read",
          ),
        );
      }

      if (this.zaak.rechten.versturenEmail) {
        this.menu.push(
          new ButtonMenuItem(
            "actie.mail.versturen",
            () => this.actionsSidenav.open(),
            "mail",
          ),
        );
      }
    }

    if (this.zaak.rechten.creeerenDocument) {
      if (
        this.zaak.zaaktype.zaakafhandelparameters.smartDocuments
          .enabledGlobally &&
        this.zaak.zaaktype.zaakafhandelparameters.smartDocuments
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

      this.menu.push(
        new ButtonMenuItem(
          "actie.document.toevoegen",
          () => this.actionsSidenav.open(),
          "upload_file",
        ),
      );

      this.menu.push(
        new ButtonMenuItem(
          "actie.document.verzenden",
          () => this.actionsSidenav.open(),
          "local_post_office",
        ),
      );
    }

    if (
      this.zaak.isOpen &&
      this.zaak.rechten.behandelen &&
      !this.zaak.isInIntakeFase &&
      this.zaak.isBesluittypeAanwezig &&
      !this.zaak.isProcesGestuurd
    ) {
      this.menu.push(
        new ButtonMenuItem(
          "actie.besluit.vastleggen",
          () => this.actionsSidenav.open(),
          "gavel",
        ),
      );
    }

    forkJoin([
      this.planItemsService.listUserEventListenerPlanItems(this.zaak.uuid),
      this.planItemsService.listHumanTaskPlanItems(this.zaak.uuid),
      this.planItemsService.listProcessTaskPlanItems(this.zaak.uuid),
    ]).subscribe(
      ([
        userEventListenerPlanItems,
        humanTaskPlanItems,
        processTaskPlanItems,
      ]) => {
        const actionMenuItems = this.createActionMenuItems();

        if (this.hasZaakData() && this.zaak.rechten.bekijkenZaakdata) {
          this.menu.push(
            new ButtonMenuItem(
              "actie.zaakdata.bekijken",
              () => this.actionsSidenav.open(),
              "folder_copy",
            ),
          );
        }

        if (
          userEventListenerPlanItems.length > 0 ||
          actionMenuItems.length > 0
        ) {
          this.menu.push(new HeaderMenuItem("actie.zaak.acties"));
          if (this.zaak.rechten.behandelen) {
            this.menu = this.menu.concat(
              userEventListenerPlanItems
                .map((userEventListenerPlanItem) =>
                  this.createUserEventListenerPlanItemMenuItem(
                    userEventListenerPlanItem,
                  ),
                )
                .filter((menuItem) => menuItem != null),
            );
          }
          this.menu = this.menu.concat(actionMenuItems);
        }

        if (this.zaak.rechten.behandelen && humanTaskPlanItems.length > 0) {
          this.menu.push(new HeaderMenuItem("actie.taak.starten"));
          this.menu = this.menu.concat(
            humanTaskPlanItems.map((humanTaskPlanItem) =>
              this.createPlanItemMenuItem(humanTaskPlanItem, "assignment"),
            ),
          );
        }

        if (this.zaak.rechten.behandelen && processTaskPlanItems.length > 0) {
          this.menu.push(new HeaderMenuItem("actie.proces.starten"));
          this.menu = this.menu.concat(
            processTaskPlanItems.map((processTaskPlanItem) =>
              this.createPlanItemMenuItem(processTaskPlanItem, "receipt_long"),
            ),
          );
        }

        this.createKoppelingenMenuItems();
        this.updateMargins();
      },
    );
  }

  private createKoppelingenMenuItems(): void {
    if (this.zaak.rechten.behandelen || this.zaak.rechten.wijzigen) {
      this.menu.push(new HeaderMenuItem("koppelingen"));
      if (
        this.zaak.rechten.toevoegenBetrokkeneBedrijf ||
        this.zaak.rechten.toevoegenBetrokkenePersoon
      ) {
        this.menu.push(
          new ButtonMenuItem(
            "actie.betrokkene.toevoegen",
            () => this.actionsSidenav.open(),
            "group_add",
          ),
        );
        if (this.zaak.rechten.toevoegenBagObject) {
          this.menu.push(
            new ButtonMenuItem(
              "actie.bagObject.toevoegen",
              () => this.actionsSidenav.open(),
              "add_home_work",
            ),
          );
        }
      }
      if (this.zaak.rechten.wijzigen) {
        this.menu.push(
          new ButtonMenuItem(
            "actie.zaak.koppelen",
            () => {
              this.zaakKoppelenService.addTeKoppelenZaak(this.zaak);
            },
            "account_tree",
          ),
        );
      }
    }
  }

  private createActionMenuItems(): MenuItem[] {
    const actionMenuItems: MenuItem[] = [];

    if (!this.zaak.isOpen && this.zaak.rechten.heropenen) {
      actionMenuItems.push(
        new ButtonMenuItem(
          "actie.zaak.heropenen",
          () => this.openZaakHeropenenDialog(),
          "restart_alt",
        ),
      );
    }

    if (
      this.zaak.isOpen &&
      this.zaak.rechten.behandelen &&
      this.zaak.zaaktype.opschortingMogelijk &&
      !this.zaak.isHeropend &&
      !this.zaak.isOpgeschort &&
      !this.zaak.isProcesGestuurd
    ) {
      actionMenuItems.push(
        new ButtonMenuItem(
          "actie.zaak.opschorten",
          () => this.openZaakOpschortenDialog(),
          "pause",
        ),
      );
    }

    if (
      this.zaak.isOpen &&
      this.zaak.rechten.wijzigenDoorlooptijd &&
      this.zaak.zaaktype.verlengingMogelijk &&
      !this.zaak.duurVerlenging &&
      !this.zaak.isHeropend &&
      !this.zaak.isOpgeschort &&
      !this.zaak.isProcesGestuurd
    ) {
      actionMenuItems.push(
        new ButtonMenuItem(
          "actie.zaak.verlengen",
          () => this.openZaakVerlengenDialog(),
          "update",
        ),
      );
    }

    if (
      this.zaak.isOpgeschort &&
      this.zaak.rechten.behandelen &&
      !this.zaak.isProcesGestuurd
    ) {
      actionMenuItems.push(
        new ButtonMenuItem(
          "actie.zaak.hervatten",
          () => this.openZaakHervattenDialog(),
          "play_circle",
        ),
      );
    }

    if (
      this.zaak.isOpen &&
      !this.zaak.isHeropend &&
      this.zaak.rechten.afbreken
    ) {
      actionMenuItems.push(
        new ButtonMenuItem(
          "actie.zaak.afbreken",
          () => this.openZaakAfbrekenDialog(),
          "thumb_down_alt",
        ),
      );
    }

    if (this.zaak.isHeropend && this.zaak.rechten.behandelen) {
      actionMenuItems.push(
        new ButtonMenuItem(
          "actie.zaak.afsluiten",
          () => this.openZaakAfsluitenDialog(),
          "thumb_up_alt",
        ),
      );
    }

    return actionMenuItems;
  }

  openPlanItemStartenDialog(planItem: PlanItem): void {
    this.actionsSidenav.close();
    this.websocketService.doubleSuspendListener(this.zaakListener);
    const userEventListenerDialog =
      this.createUserEventListenerDialog(planItem);
    this.dialog
      .open(userEventListenerDialog.dialogComponent, {
        data: userEventListenerDialog.dialogData,
      })
      .afterClosed()
      .subscribe((result) => {
        this.activeSideAction = null;
        if (result) {
          if (result === "openBesluitVastleggen") {
            this.actionsSidenav.open();
          } else {
            this.utilService.openSnackbar(
              "msg.planitem.uitgevoerd." + planItem.userEventListenerActie,
            );
            this.updateZaak();
          }
        }
      });
  }

  createUserEventListenerDialog(planItem: PlanItem): {
    dialogComponent: any;
    dialogData: any;
  } {
    switch (planItem.userEventListenerActie) {
      case UserEventListenerActie.IntakeAfronden:
        return this.createUserEventListenerIntakeAfrondenDialog(planItem);
      case UserEventListenerActie.ZaakAfhandelen:
        return this.createUserEventListenerZaakAfhandelenDialog(planItem);
      default:
        throw new Error(
          `Niet bestaande UserEventListenerActie: ${planItem.userEventListenerActie}`,
        );
    }
  }

  createUserEventListenerIntakeAfrondenDialog(planItem: PlanItem): {
    dialogComponent: any;
    dialogData: any;
  } {
    return {
      dialogComponent: IntakeAfrondenDialogComponent,
      dialogData: { zaak: this.zaak, planItem: planItem },
    };
  }

  createUserEventListenerZaakAfhandelenDialog(planItem: PlanItem): {
    dialogComponent: any;
    dialogData: any;
  } {
    return {
      dialogComponent: ZaakAfhandelenDialogComponent,
      dialogData: { zaak: this.zaak, planItem: planItem },
    };
  }

  private openZaakAfbrekenDialog(): void {
    this.actionsSidenav.close();
    const dialogData = new DialogData(
      [
        new SelectFormFieldBuilder()
          .id("reden")
          .label("actie.zaak.afbreken.reden")
          .optionLabel("naam")
          .options(
            this.zaakafhandelParametersService.listZaakbeeindigRedenenForZaaktype(
              this.zaak.zaaktype.uuid,
            ),
          )
          .validators(Validators.required)
          .build(),
      ],
      (results: any[]) =>
        this.zakenService
          .afbreken(this.zaak.uuid, results["reden"])
          .pipe(
            tap(() => this.websocketService.suspendListener(this.zaakListener)),
          ),
    );

    dialogData.confirmButtonActionKey = "actie.zaak.afbreken";

    this.dialog
      .open(DialogComponent, { data: dialogData })
      .afterClosed()
      .subscribe((result) => {
        this.activeSideAction = null;
        if (result) {
          this.updateZaak();
          this.loadTaken();
          this.utilService.openSnackbar("msg.zaak.afgebroken");
        }
      });
  }

  private openZaakHeropenenDialog(): void {
    const dialogData = new DialogData(
      [
        new InputFormFieldBuilder()
          .id("reden")
          .label("actie.zaak.heropenen.reden")
          .validators(Validators.required)
          .maxlength(100)
          .build(),
      ],
      (results: any[]) =>
        this.zakenService
          .heropenen(this.zaak.uuid, results["reden"])
          .pipe(
            tap(() => this.websocketService.suspendListener(this.zaakListener)),
          ),
    );

    dialogData.confirmButtonActionKey = "actie.zaak.heropenen";

    this.dialog
      .open(DialogComponent, { data: dialogData })
      .afterClosed()
      .subscribe((result) => {
        this.activeSideAction = null;
        if (result) {
          this.updateZaak();
          this.loadTaken();
          this.utilService.openSnackbar("msg.zaak.heropend");
        }
      });
  }

  private openZaakAfsluitenDialog(): void {
    this.actionsSidenav.close();
    const dialogData = new DialogData(
      [
        new SelectFormFieldBuilder()
          .id("resultaattype")
          .label("resultaat")
          .optionLabel("naam")
          .options(
            this.zakenService.listResultaattypes(this.zaak.zaaktype.uuid),
          )
          .validators(Validators.required)
          .build(),
        new InputFormFieldBuilder()
          .id("toelichting")
          .label("toelichting")
          .maxlength(80)
          .build(),
      ],
      (results: any[]) =>
        this.zakenService
          .afsluiten(
            this.zaak.uuid,
            results["toelichting"],
            results["resultaattype"].id,
          )
          .pipe(
            tap(() => this.websocketService.suspendListener(this.zaakListener)),
          ),
    );

    dialogData.confirmButtonActionKey = "actie.zaak.afsluiten";

    this.dialog
      .open(DialogComponent, { data: dialogData })
      .afterClosed()
      .subscribe((result) => {
        this.activeSideAction = null;
        if (result) {
          this.updateZaak();
          this.loadTaken();
          this.utilService.openSnackbar("msg.zaak.afgesloten");
        }
      });
  }

  private openZaakOpschortenDialog(): void {
    this.actionsSidenav.close();
    this.dialog
      .open(ZaakOpschortenDialogComponent, {
        data: { zaak: this.zaak },
      })
      .afterClosed()
      .subscribe((result) => {
        this.activeSideAction = null;
        if (result) {
          this.init(result);
          this.utilService.openSnackbar("msg.zaak.opgeschort");
        }
      });
  }

  private openZaakVerlengenDialog(): void {
    this.actionsSidenav.close();
    this.dialog
      .open(ZaakVerlengenDialogComponent, {
        data: { zaak: this.zaak },
      })
      .afterClosed()
      .subscribe((result) => {
        this.activeSideAction = null;
        if (result) {
          this.init(result);
          this.utilService.openSnackbar("msg.zaak.verlengd");
        }
      });
  }

  private openZaakHervattenDialog(): void {
    this.actionsSidenav.close();

    const werkelijkeOpschortDuur = moment().diff(
      moment(this.zaakOpschorting?.vanafDatumTijd),
      "days",
    );
    const duurVerkortingOpschorting: number =
      werkelijkeOpschortDuur - this.zaakOpschorting.duurDagen;

    const dialogData = new DialogData(
      [
        new InputFormFieldBuilder()
          .id("redenOpschortingField")
          .label("reden")
          .validators(Validators.required)
          .build(),
      ],
      (results: any[]) => {
        const zaakOpschortGegevens: GeneratedType<"RESTZaakOpschortGegevens"> =
          {
            indicatieOpschorting: false,
            duurDagen: werkelijkeOpschortDuur,
            uiterlijkeEinddatumAfdoening: moment(
              this.zaak.uiterlijkeEinddatumAfdoening,
            )
              .add(duurVerkortingOpschorting, "days")
              .format("YYYY-MM-DD"),
            redenOpschorting: results["redenOpschortingField"],
          };

        if (this.zaak.einddatumGepland) {
          zaakOpschortGegevens.einddatumGepland = moment(
            this.zaak.einddatumGepland,
          )
            .add(duurVerkortingOpschorting, "days")
            .format("YYYY-MM-DD");
        }

        return this.zakenService.opschortenZaak(
          this.zaak.uuid,
          zaakOpschortGegevens,
        );
      },
      this.translate.instant("msg.zaak.hervatten", {
        duur: werkelijkeOpschortDuur,
        verwachteDuur: this.zaakOpschorting.duurDagen,
      }),
    );
    dialogData.confirmButtonActionKey = "actie.zaak.hervatten";

    this.dialog
      .open(DialogComponent, { data: dialogData })
      .afterClosed()
      .subscribe((result) => {
        this.activeSideAction = null;
        if (result) {
          this.utilService.openSnackbar("msg.zaak.hervat");
          this.updateZaak();
        }
      });
  }

  private loadOpschorting(): void {
    if (this.zaak.isOpgeschort) {
      this.zakenService
        .readOpschortingZaak(this.zaak.uuid)
        .subscribe((objectData) => {
          this.zaakOpschorting = objectData;
        });
    }
  }

  public updateZaak(): void {
    this.zakenService.readZaak(this.zaak.uuid).subscribe((zaak) => {
      this.init(zaak);
    });
  }

  private loadHistorie(): void {
    this.zakenService
      .listHistorieVoorZaak(this.zaak.uuid)
      .subscribe((historie) => {
        this.historie.data = historie;
      });
  }

  private loadBetrokkenen(): void {
    this.zakenService
      .listBetrokkenenVoorZaak(this.zaak.uuid)
      .subscribe((betrokkenen) => {
        this.betrokkenen.data = betrokkenen;
      });
  }

  private loadBagObjecten(): void {
    this.bagService.list(this.zaak.uuid).subscribe((bagObjecten) => {
      this.gekoppeldeBagObjecten = bagObjecten.map((bg) => bg.zaakobject);
      this.bagObjectenDataSource.data = bagObjecten;
    });
  }

  showOrEditZaakLocatie(): void {
    if (
      (this.zaak.isOpen && this.zaak.rechten.wijzigen) ||
      this.zaak.zaakgeometrie != null
    ) {
      this.activeSideAction = "actie.locatie.toevoegen";
      this.actionsSidenav.open();
    }
  }

  editCaseDetails(): void {
    if (this.zaak.isOpen && this.zaak.rechten.wijzigen) {
      this.activeSideAction = "actie.zaak.wijzigen";
      this.actionsSidenav.open();
    }
  }

  addOrEditZaakInitiator(): void {
    this.activeSideAction = "actie.initiator.toevoegen";
    this.actionsSidenav.open();
  }

  private loadBesluiten(): void {
    this.zakenService
      .listBesluitenForZaak(this.zaak.uuid)
      .subscribe((besluiten) => (this.zaak.besluiten = besluiten));
  }

  private loadTaken(): void {
    this.takenLoading = true;
    this.takenService
      .listTakenVoorZaak(this.zaak.uuid)
      .pipe(
        map((values) => values.map((value) => new ExpandableTableData(value))),
      )
      .subscribe((taken) => {
        taken = taken.sort(
          (a, b) =>
            a.data.fataledatum?.localeCompare(b.data.fataledatum) ||
            a.data.creatiedatumTijd?.localeCompare(b.data.creatiedatumTijd),
        );
        this.takenDataSource.data = taken;
        this.filterTakenOpStatus();
        this.takenLoading = false;
      });
  }

  expandTaken(expand: boolean): void {
    this.takenDataSource.data.forEach((value) => (value.expanded = expand));
    this.checkAllTakenExpanded();
  }

  expandTaak(taak: ExpandableTableData<Taak>): void {
    taak.expanded = !taak.expanded;
    this.checkAllTakenExpanded();
  }

  checkAllTakenExpanded(): void {
    const filter = this.toonAfgerondeTaken.value
      ? this.takenDataSource.data.filter((value) => !value.expanded)
      : this.takenDataSource.data.filter(
          (value) => value.data.status !== "AFGEROND" && !value.expanded,
        );

    this.allTakenExpanded = filter.length === 0;
  }

  showAssignTaakToMe(taak: Taak): boolean {
    return (
      taak.status !== TaakStatus.Afgerond &&
      taak.rechten.toekennen &&
      this.loggedInUser.id !== taak.behandelaar?.id &&
      this.loggedInUser.groupIds.indexOf(taak.groep.id) >= 0
    );
  }

  locatieGeselecteerd(locatie: GeometryGegevens): void {
    this.actionsSidenav.close();
    this.websocketService.suspendListener(this.zaakListener);
    this.zakenService
      .updateZaakLocatie(this.zaak.uuid, locatie.geometry, locatie.reden)
      .subscribe((updatedZaak) => {
        this.init(updatedZaak);
      });
  }

  initiatorGeselecteerd(initiator: Klant): void {
    this.websocketService.suspendListener(this.zaakRollenListener);
    this.actionsSidenav.close();
    const melding = this.zaak.initiatorIdentificatie
      ? "msg.initiator.gewijzigd"
      : "msg.initiator.toegevoegd";
    this.zakenService
      .updateInitiator(this.zaak, initiator)
      .subscribe((zaak) => {
        this.zaak = zaak;
        this.utilService.openSnackbar(melding, {
          naam: zaak.initiatorIdentificatie,
        });
        this.loadHistorie();
      });
  }

  deleteInitiator(): void {
    this.websocketService.suspendListener(this.zaakRollenListener);
    this.dialog
      .open(DialogComponent, {
        data: new DialogData(
          [
            new TextareaFormFieldBuilder()
              .id("reden")
              .label("reden")
              .validators(Validators.required)
              .build(),
          ],
          (results: any[]) =>
            this.zakenService.deleteInitiator(this.zaak, results["reden"]),
          this.translate.instant("msg.initiator.ontkoppelen.bevestigen"),
        ),
      })
      .afterClosed()
      .subscribe((result) => {
        this.activeSideAction = null;
        if (result) {
          this.utilService.openSnackbar("msg.initiator.ontkoppelen.uitgevoerd");
          this.zakenService.readZaak(this.zaak.uuid).subscribe((zaak) => {
            this.zaak = zaak;
            this.loadHistorie();
          });
        }
      });
  }

  betrokkeneGeselecteerd(betrokkene: KlantGegevens): void {
    this.websocketService.suspendListener(this.zaakRollenListener);
    this.actionsSidenav.close();
    this.zakenService
      .createBetrokkene(
        this.zaak,
        betrokkene.klant,
        betrokkene.betrokkeneRoltype,
        betrokkene.betrokkeneToelichting,
      )
      .subscribe((zaak) => {
        this.zaak = zaak;
        this.utilService.openSnackbar("msg.betrokkene.toegevoegd", {
          roltype: betrokkene.betrokkeneRoltype.naam,
        });
        this.loadHistorie();
        this.loadBetrokkenen();
      });
  }

  deleteBetrokkene(betrokkene: ZaakBetrokkene): void {
    this.websocketService.suspendListener(this.zaakRollenListener);
    const betrokkeneIdentificatie: string =
      betrokkene.roltype + " " + betrokkene.identificatie;
    this.dialog
      .open(DialogComponent, {
        data: new DialogData(
          [
            new TextareaFormFieldBuilder()
              .id("reden")
              .label("reden")
              .validators(Validators.required)
              .build(),
          ],
          (results: any[]) =>
            this.zakenService.deleteBetrokkene(
              betrokkene.rolid,
              results["reden"],
            ),
          this.translate.instant("msg.betrokkene.ontkoppelen.bevestigen", {
            betrokkene: betrokkeneIdentificatie,
          }),
        ),
      })
      .afterClosed()
      .subscribe((result) => {
        this.activeSideAction = null;
        if (result) {
          this.utilService.openSnackbar(
            "msg.betrokkene.ontkoppelen.uitgevoerd",
            { betrokkene: betrokkeneIdentificatie },
          );
          this.zakenService.readZaak(this.zaak.uuid).subscribe((zaak) => {
            this.zaak = zaak;
            this.loadHistorie();
            this.loadBetrokkenen();
          });
        }
      });
  }

  adresGeselecteerd(bagObject: BAGObject): void {
    this.websocketService.suspendListener(this.zaakListener);
    this.bagService
      .create(new BAGObjectGegevens(this.zaak.uuid, bagObject))
      .subscribe(() => {
        this.utilService.openSnackbar("msg.bagObject.toegevoegd");
        this.loadHistorie();
        this.loadBagObjecten();
      });
  }

  assignTaakToMe(taak: Taak, $event) {
    $event.stopPropagation();

    this.websocketService.suspendListener(this.zaakTakenListener);
    this.takenService
      .toekennenAanIngelogdeMedewerker(taak)
      .subscribe((returnTaak) => {
        taak.behandelaar = returnTaak.behandelaar;
        taak.status = returnTaak.status;
        this.utilService.openSnackbar("msg.taak.toegekend", {
          behandelaar: taak.behandelaar.naam,
        });
      });
  }

  filterTakenOpStatus() {
    if (!this.toonAfgerondeTaken.value) {
      this.takenFilter["status"] = "AFGEROND";
    }

    this.takenDataSource.filter = this.takenFilter;
    SessionStorageUtil.setItem(
      "toonAfgerondeTaken",
      this.toonAfgerondeTaken.value,
    );
  }

  sluitSidenav(): void {
    this.activeSideAction = null;
    this.actiefPlanItem = null;
    this.actionsSidenav.close();
  }

  taakGestart(): void {
    this.sluitSidenav();
    this.updateZaak();
  }

  processGestart(): void {
    this.sluitSidenav();
    this.updateZaak();
  }

  mailVerstuurd(mailVerstuurd: boolean): void {
    this.sluitSidenav();
    if (mailVerstuurd) {
      this.updateZaak();
    }
  }

  ontvangstBevestigd(ontvangstBevestigd: boolean): void {
    this.sluitSidenav();
    if (ontvangstBevestigd) {
      this.updateZaak();
    }
  }

  documentToegevoegd(): void {
    this.updateZaak();
  }

  documentCreated(): void {
    this.sluitSidenav();
    this.updateZaak();
  }

  documentSent(): void {
    this.sluitSidenav();
    this.updateZaak();
  }

  startZaakOntkoppelenDialog(
    gerelateerdeZaak: GeneratedType<"RestGerelateerdeZaak">,
  ): void {
    this.dialog
      .open(ZaakOntkoppelenDialogComponent, {
        data: {
          zaakUuid: this.zaak.uuid,
          gekoppeldeZaakIdentificatie: gerelateerdeZaak.identificatie,
          relatieType: gerelateerdeZaak.relatieType,
          reden: "",
        },
      })
      .afterClosed()
      .subscribe((result) => {
        this.activeSideAction = null;
        if (result) {
          this.utilService.openSnackbar("msg.zaak.ontkoppelen.uitgevoerd");
        }
      });
  }

  besluitVastgelegd(): void {
    this.sluitSidenav();
  }

  besluitWijzigen($event): void {
    this.activeSideAction = "actie.besluit.wijzigen";
    this.teWijzigenBesluit = $event;
    this.actionsSidenav.open();
  }

  doIntrekking($event): void {
    const gegevens: GeneratedType<"RestDecisionWithdrawalData"> = {
      besluitUuid: $event.uuid,
      vervaldatum: $event.vervaldatum,
      vervalreden: $event.vervalreden.value,
      reden: $event.toelichting,
    };
    this.zakenService.intrekkenBesluit(gegevens).subscribe(() => {
      this.utilService.openSnackbar("msg.besluit.ingetrokken");
    });
  }

  betrokkeneGegevensOphalen(betrokkene: ZaakBetrokkene): void {
    betrokkene["gegevens"] = "LOADING";
    switch (betrokkene.type) {
      case "NATUURLIJK_PERSOON":
        this.klantenService
          .readPersoon(betrokkene.identificatie)
          .subscribe((persoon) => {
            betrokkene["gegevens"] = persoon.naam;
            if (persoon.geboortedatum) {
              betrokkene["gegevens"] += `, ${this.datumPipe.transform(
                persoon.geboortedatum,
              )}`;
            }
            if (persoon.verblijfplaats) {
              betrokkene["gegevens"] += `,\n${persoon.verblijfplaats}`;
            }
          });
        break;
      case "NIET_NATUURLIJK_PERSOON":
      case "VESTIGING":
        this.klantenService
          .readBedrijf(betrokkene.identificatie)
          .subscribe((bedrijf) => {
            betrokkene["gegevens"] = bedrijf.naam;
            if (bedrijf.adres) {
              betrokkene["gegevens"] += `,\n${bedrijf.adres}`;
            }
          });
        break;
      case "ORGANISATORISCHE_EENHEID":
      case "MEDEWERKER":
        betrokkene["gegevens"] = "-";
        break;
    }
  }

  bagObjectVerwijderen(bagObjectGegevens: BAGObjectGegevens): void {
    const bagObject = bagObjectGegevens.zaakobject;
    const reden = new InputFormFieldBuilder()
      .maxlength(80)
      .id("reden")
      .label("reden")
      .validators(Validators.required)
      .build();
    const dialogData = new DialogData([reden], (results: any[]) =>
      this.bagService
        .delete(
          new BAGObjectGegevens(
            this.zaak.uuid,
            bagObject,
            bagObjectGegevens.uuid,
            results["reden"],
          ),
        )
        .pipe(
          tap(() => this.websocketService.suspendListener(this.zaakListener)),
        ),
    );

    dialogData.uitleg = this.translate.instant(
      "msg.bagObject.verwijderen.bevestigen",
      { omschrijving: bagObject.omschrijving },
    );
    dialogData.confirmButtonActionKey = "actie.bagObject.verwijderen";
    this.dialog
      .open(DialogComponent, { data: dialogData })
      .afterClosed()
      .subscribe((result) => {
        this.activeSideAction = null;
        if (result) {
          this.loadHistorie();
          this.loadBagObjecten();
          this.utilService.openSnackbar(
            "msg.bagObject.verwijderen.uitgevoerd",
            { omschrijving: bagObject.omschrijving },
          );
        }
      });
  }

  showProces() {
    const dialogData = new DialogData([
      new ReadonlyFormFieldBuilder(
        '<img src="/rest/zaken/' +
          this.zaak.uuid +
          '/procesdiagram"/ alt="diagram">',
      )
        .id("diagram")
        .label("proces.toestand")
        .build(),
    ]);
    dialogData.confirmButtonActionKey = "actie.ok";
    dialogData.cancelButtonActionKey = null;
    this.dialog.open(DialogComponent, { data: dialogData });
  }

  hasZaakData() {
    return this.zaak.zaakdata && Object.keys(this.zaak.zaakdata).length > 0;
  }

  protected taskStatusChipColor(status: TaakStatus) {
    switch (status) {
      case TaakStatus.Afgerond:
        return "success";
      case TaakStatus.Toegekend:
        return "primary";
      default:
        return "";
    }
  }

  async menuItemChanged(event: string) {
    if (event === "actie.zaak.koppelen") {
      return;
    }

    setTimeout(() => {
      this.activeSideAction = event;
    }, 100); // Prevent `closedStart` from `#actionsSidenav` to reset the value to `null` on dialog actions
  }
}

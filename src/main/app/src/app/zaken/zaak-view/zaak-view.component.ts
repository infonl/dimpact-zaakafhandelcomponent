/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024-2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentType } from "@angular/cdk/portal";
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
import { ActieOnmogelijkDialogComponent } from "src/app/fout-afhandeling/dialog/actie-onmogelijk-dialog.component";
import { PolicyService } from "src/app/policy/policy.service";
import { DateConditionals } from "src/app/shared/utils/date-conditionals";
import { ZaakafhandelParametersService } from "../../admin/zaakafhandel-parameters.service";
import { BAGService } from "../../bag/bag.service";
import { UtilService } from "../../core/service/util.service";
import { ObjectType } from "../../core/websocket/model/object-type";
import { Opcode } from "../../core/websocket/model/opcode";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { KlantenService } from "../../klanten/klanten.service";
import { KlantGegevens } from "../../klanten/model/klanten/klant-gegevens";
import { ViewResourceUtil } from "../../locatie/view-resource.util";
import { PlanItemsService } from "../../plan-items/plan-items.service";
import { ActionsViewComponent } from "../../shared/abstract-view/actions-view-component";
import { detailExpand } from "../../shared/animations/animations";
import { DialogData } from "../../shared/dialog/dialog-data";
import { DialogComponent } from "../../shared/dialog/dialog.component";
import { ExpandableTableData } from "../../shared/dynamic-table/model/expandable-table-data";
import { TextIcon } from "../../shared/edit/text-icon";
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
import { TakenService } from "../../taken/taken.service";
import { IntakeAfrondenDialogComponent } from "../intake-afronden-dialog/intake-afronden-dialog.component";
import { BetrokkeneIdentificatie } from "../model/betrokkeneIdentificatie";
import { ZaakAfhandelenDialogComponent } from "../zaak-afhandelen-dialog/zaak-afhandelen-dialog.component";
import { ZaakDocumentenComponent } from "../zaak-documenten/zaak-documenten.component";
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
  zaak!: GeneratedType<"RestZaak">;
  zaakOpschorting!: GeneratedType<"RESTZaakOpschorting">;
  menu: MenuItem[] = [];
  actiefPlanItem: GeneratedType<"RESTPlanItem"> | null = null;
  activeSideAction: string | null = null;
  teWijzigenBesluit!: GeneratedType<"RestDecision">;
  documentToMove!: Partial<GeneratedType<"RestEnkelvoudigInformatieobject">>;

  takenDataSource = new MatTableDataSource<
    ExpandableTableData<GeneratedType<"RestTask">>
  >();
  allTakenExpanded = false;
  toonAfgerondeTaken = new FormControl(false);
  takenStatusFilter: GeneratedType<"TaakStatus"> | "" = "";
  takenLoading = false;
  takenColumnsToDisplay = [
    "naam",
    "status",
    "creatiedatumTijd",
    "fataledatum",
    "groep",
    "behandelaar",
    "id",
  ] as const;

  historie = new MatTableDataSource<GeneratedType<"RestTaskHistoryLine">>();
  historieColumns = [
    "datum",
    "gebruiker",
    "wijziging",
    "actie",
    "oudeWaarde",
    "nieuweWaarde",
    "toelichting",
  ] as const;
  betrokkenen = new MatTableDataSource<GeneratedType<"RestZaakBetrokkene">>();
  betrokkenenColumns = [
    "roltype",
    "betrokkenegegevens",
    "betrokkeneidentificatie",
    "roltoelichting",
    "actions",
  ] as const;
  bagObjectenDataSource = new MatTableDataSource<
    GeneratedType<"RESTBAGObjectGegevens">
  >();
  gekoppeldeBagObjecten: GeneratedType<"RESTBAGObject">[] = [];
  bagObjectenColumns = [
    "identificatie",
    "type",
    "omschrijving",
    "actions",
  ] as const;
  gerelateerdeZaakColumns = [
    "identificatie",
    "zaaktypeOmschrijving",
    "statustypeOmschrijving",
    "startdatum",
    "relatieType",
  ] as const;
  gerelateerdeZaakColumnsWithAction = [
    ...this.gerelateerdeZaakColumns,
    "actions",
  ];

  notitieRechten!: GeneratedType<"RestNotitieRechten">;
  dateFieldIconMap = new Map<string, TextIcon>();
  viewInitialized = false;
  loggedInUser!: GeneratedType<"RestLoggedInUser">;

  private zaakListener!: WebsocketListener;
  private zaakRollenListener!: WebsocketListener;
  private zaakBesluitenListener!: WebsocketListener;
  private zaakTakenListener!: WebsocketListener;
  private datumPipe = new DatumPipe("nl");

  @ViewChild("actionsSidenav") actionsSidenav!: MatSidenav;
  @ViewChild("menuSidenav") menuSidenav!: MatSidenav;
  @ViewChild("sideNavContainer") sideNavContainer!: MatSidenavContainer;

  @ViewChild("historieSort") historieSort!: MatSort;
  @ViewChild("takenSort") takenSort!: MatSort;
  @ViewChild("zaakDocumentenComponent")
  zaakDocumentenComponent!: ZaakDocumentenComponent;

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
    private bagService: BAGService,
    private policyService: PolicyService,
  ) {
    super();
  }

  ngOnInit() {
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
        this.loadNotitieRechten();
      }),
    );

    this.takenDataSource.filterPredicate = (data, filter) => {
      if (!filter) return true;

      return !this.toonAfgerondeTaken.value
        ? data.data.status !== filter
        : true;
    };

    this.toonAfgerondeTaken.setValue(
      Boolean(SessionStorageUtil.getItem("toonAfgerondeTaken")),
    );
  }

  init(zaak: GeneratedType<"RestZaak">) {
    this.zaak = zaak;
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
          return item.data.groep?.naam ?? "";
        case "behandelaar":
          return item.data.behandelaar?.naam ?? "";
        default:
          return String(item.data[property as keyof typeof item.data]);
      }
    };
    this.takenDataSource.sort = this.takenSort;

    this.historie.sortingDataAccessor = (item, property) => {
      switch (property) {
        case "datum":
          return String(item.datumTijd);
        case "gebruiker":
          return (item as unknown as { door: string }).door;
        default:
          return String(item[property as keyof typeof item]);
      }
    };

    this.historie.sort = this.historieSort;
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    ViewResourceUtil.actieveZaak = null;
    this.websocketService.removeListener(this.zaakListener);
    this.websocketService.removeListener(this.zaakBesluitenListener);
    this.websocketService.removeListener(this.zaakRollenListener);
    this.websocketService.removeListener(this.zaakTakenListener);
  }

  private setDateFieldIconSet() {
    this.dateFieldIconMap.set(
      "einddatumGepland",
      new TextIcon(
        (control: FormControl) => {
          return DateConditionals.isExceeded(
            control.value,
            this.zaak.einddatum,
          );
        },
        "report_problem",
        "warningVerlopen_icon",
        this.zaak.einddatum
          ? "msg.einddatum.overschreden"
          : "msg.datum.overschreden",
        "warning",
      ),
    );

    this.dateFieldIconMap.set(
      "uiterlijkeEinddatumAfdoening",
      new TextIcon(
        (control: FormControl) => {
          return DateConditionals.isExceeded(
            control.value,
            this.zaak.einddatum,
          );
        },
        "report_problem",
        "errorVerlopen_icon",
        this.zaak.einddatum
          ? "msg.einddatum.overschreden"
          : "msg.datum.overschreden",
        "error",
      ),
    );
  }

  private createUserEventListenerPlanItemMenuItem(
    userEventListenerPlanItem: GeneratedType<"RESTPlanItem">,
  ) {
    return new ButtonMenuItem(
      "planitem." + userEventListenerPlanItem.userEventListenerActie,
      () => this.openPlanItemStartenDialog(userEventListenerPlanItem),
      this.getuserEventListenerPlanItemMenuItemIcon(
        userEventListenerPlanItem.userEventListenerActie,
      ),
    );
  }

  private createPlanItemMenuItem(
    planItem: GeneratedType<"RESTPlanItem">,
    icon: string,
  ) {
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
    userEventListenerActie?: GeneratedType<"UserEventListenerActie"> | null,
  ) {
    switch (userEventListenerActie) {
      case "INTAKE_AFRONDEN":
        return "thumbs_up_down";
      case "ZAAK_AFHANDELEN":
        return "thumb_up_alt";
      default:
        return "fact_check";
    }
  }

  private setupMenu() {
    this.menu = [new HeaderMenuItem("zaak")];

    if (this.zaak.rechten.behandelen && !this.zaak.isProcesGestuurd) {
      if (
        this.zaak.rechten.versturenOntvangstbevestiging &&
        !this.zaak.heeftOntvangstbevestigingVerstuurd
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

    if (this.zaak.rechten.creerenDocument) {
      if (
        this.zaak.zaaktype.zaakafhandelparameters?.smartDocuments
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

  private createKoppelingenMenuItems() {
    if (this.zaak.rechten.behandelen || this.zaak.rechten.wijzigen) {
      this.menu.push(new HeaderMenuItem("koppelingen"));
      if (this.allowedToAddBetrokkene()) {
        this.menu.push(
          new ButtonMenuItem(
            "actie.betrokkene.koppelen",
            () => this.actionsSidenav.open(),
            "group_add",
          ),
        );
      }

      if (this.zaak.rechten.toevoegenBagObject) {
        this.menu.push(
          new ButtonMenuItem(
            "actie.bagObject.koppelen",
            () => this.actionsSidenav.open(),
            "add_home_work",
          ),
        );
      }

      if (this.zaak.rechten.wijzigenLocatie && !this.zaak.zaakgeometrie) {
        this.menu.push(
          new ButtonMenuItem(
            "actie.zaak.locatie.koppelen",
            () => this.actionsSidenav.open(),
            "add_location_alt",
          ),
        );
      }

      if (this.zaak.rechten.wijzigen) {
        this.menu.push(
          new ButtonMenuItem(
            "actie.zaak.koppelen",
            () => this.actionsSidenav.open(),
            "account_tree",
          ),
        );
      }
    }
  }

  private createActionMenuItems() {
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
      !this.zaak.isProcesGestuurd &&
      !this.zaak.isEerderOpgeschort
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

  openPlanItemStartenDialog(planItem: GeneratedType<"RESTPlanItem">) {
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
        if (!result) return;

        if (result === "openBesluitVastleggen") {
          this.activeSideAction = "actie.besluit.vastleggen";
          this.actionsSidenav.open();
          return;
        }

        this.utilService.openSnackbar(
          `msg.planitem.uitgevoerd.${planItem.userEventListenerActie}`,
        );
        this.updateZaak();
      });
  }

  createUserEventListenerDialog(planItem: GeneratedType<"RESTPlanItem">): {
    dialogComponent: ComponentType<unknown>;
    dialogData: {
      zaak: GeneratedType<"RestZaak">;
      planItem: GeneratedType<"RESTPlanItem">;
    };
  } {
    switch (planItem.userEventListenerActie) {
      case "INTAKE_AFRONDEN":
        return this.createUserEventListenerIntakeAfrondenDialog(planItem);
      case "ZAAK_AFHANDELEN":
        return this.createUserEventListenerZaakAfhandelenDialog(planItem);
      default:
        throw new Error(
          `Niet bestaande UserEventListenerActie: ${planItem.userEventListenerActie}`,
        );
    }
  }

  createUserEventListenerIntakeAfrondenDialog(
    planItem: GeneratedType<"RESTPlanItem">,
  ) {
    return {
      dialogComponent: IntakeAfrondenDialogComponent,
      dialogData: { zaak: this.zaak, planItem: planItem },
    };
  }

  createUserEventListenerZaakAfhandelenDialog(
    planItem: GeneratedType<"RESTPlanItem">,
  ) {
    return {
      dialogComponent: this.zaak.isOpgeschort
        ? ActieOnmogelijkDialogComponent
        : ZaakAfhandelenDialogComponent,
      dialogData: { zaak: this.zaak, planItem: planItem },
    };
  }

  private openZaakAfbrekenDialog() {
    void this.actionsSidenav.close();

    if (this.zaak.isOpgeschort) {
      this.dialog.open(ActieOnmogelijkDialogComponent);
      return;
    }

    const dialogData = new DialogData<
      unknown,
      { reden: GeneratedType<"RESTZaakbeeindigReden"> }
    >({
      formFields: [
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
      callback: ({ reden }) =>
        this.zakenService
          .afbreken(this.zaak.uuid, { zaakbeeindigRedenId: reden.id! })
          .pipe(
            tap(() => this.websocketService.suspendListener(this.zaakListener)),
          ),
      confirmButtonActionKey: "actie.zaak.afbreken",
      icon: "thumb_down_alt",
    });

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

  private openZaakHeropenenDialog() {
    const dialogData = new DialogData<unknown, { reden: string }>({
      formFields: [
        new InputFormFieldBuilder()
          .id("reden")
          .label("actie.zaak.heropenen.reden")
          .validators(Validators.required)
          .maxlength(100)
          .build(),
      ],
      callback: ({ reden }) =>
        this.zakenService
          .heropenen(this.zaak.uuid, { reden })
          .pipe(
            tap(() => this.websocketService.suspendListener(this.zaakListener)),
          ),
      confirmButtonActionKey: "actie.zaak.heropenen",
      icon: "restart_alt",
    });

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

  private openZaakAfsluitenDialog() {
    void this.actionsSidenav.close();

    this.dialog
      .open(ZaakAfhandelenDialogComponent, { data: { zaak: this.zaak } })
      .afterClosed()
      .subscribe((result) => {
        this.activeSideAction = null;
        if (!result) return;
        this.updateZaak();
        this.loadTaken();
        this.utilService.openSnackbar("msg.zaak.afgesloten");
      });
  }

  private openZaakOpschortenDialog() {
    void this.actionsSidenav.close();
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

  private openZaakVerlengenDialog() {
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

  private openZaakHervattenDialog() {
    this.actionsSidenav.close();

    const werkelijkeOpschortDuur = moment().diff(
      moment(this.zaakOpschorting?.vanafDatumTijd),
      "days",
    );

    const dialogData = new DialogData<
      unknown,
      { redenOpschortingField?: string }
    >({
      formFields: [
        new InputFormFieldBuilder()
          .id("redenOpschortingField")
          .label("reden")
          .validators(Validators.required)
          .maxlength(200)
          .build(),
      ],
      callback: ({ redenOpschortingField }) => {
        const duurVerkortingOpschorting: number =
          werkelijkeOpschortDuur - (this.zaakOpschorting?.duurDagen ?? 0);

        const zaakOpschortGegevens: GeneratedType<"RESTZaakOpschortGegevens"> =
          {
            indicatieOpschorting: false,
            duurDagen: werkelijkeOpschortDuur,
            uiterlijkeEinddatumAfdoening: moment(
              this.zaak.uiterlijkeEinddatumAfdoening,
            )
              .add(duurVerkortingOpschorting, "days")
              .format("YYYY-MM-DD"),
            redenOpschorting: redenOpschortingField,
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
      melding: this.translate.instant("msg.zaak.hervatten", {
        duur: werkelijkeOpschortDuur,
        verwachteDuur: this.zaakOpschorting.duurDagen,
      }),
      confirmButtonActionKey: "actie.zaak.hervatten",
      icon: "play_circle",
    });

    this.dialog
      .open(DialogComponent, { data: dialogData })
      .afterClosed()
      .subscribe((result) => {
        this.activeSideAction = null;
        if (result) {
          this.utilService.openSnackbar("msg.zaak.hervat");
          this.updateZaak();
          this.loadOpschorting();
        }
      });
  }

  private loadOpschorting() {
    if (this.zaak.isOpgeschort) {
      this.zakenService
        .readOpschortingZaak(this.zaak.uuid)
        .subscribe((objectData) => {
          this.zaakOpschorting = objectData;
        });
    }
  }

  public updateZaak() {
    this.zakenService.readZaak(this.zaak.uuid).subscribe((zaak) => {
      this.init(zaak);
    });
  }

  private loadHistorie() {
    this.zakenService
      .listHistorieVoorZaak(this.zaak.uuid)
      .subscribe((historie) => {
        this.historie.data = historie;
      });
  }

  private loadBetrokkenen() {
    this.zakenService
      .listBetrokkenenVoorZaak(this.zaak.uuid)
      .subscribe((betrokkenen) => {
        this.betrokkenen.data = betrokkenen;
      });
  }

  private loadBagObjecten() {
    this.bagService.list(this.zaak.uuid).subscribe((bagObjecten) => {
      this.gekoppeldeBagObjecten = bagObjecten
        .map((bg) => bg.zaakobject!)
        .filter(Boolean);
      this.bagObjectenDataSource.data = bagObjecten;
    });
  }

  editCaseDetails() {
    if (this.zaak.rechten.wijzigen || this.zaak.rechten.toekennen) {
      this.activeSideAction = "actie.zaak.wijzigen";
      this.actionsSidenav.open();
    }
  }

  editLocationDetails() {
    if (this.zaak.rechten.wijzigen) {
      this.activeSideAction = "actie.zaak.locatie.koppelen";
      this.actionsSidenav.open();
    }
  }

  addOrEditZaakInitiator() {
    this.activeSideAction = "actie.initiator.koppelen";
    this.actionsSidenav.open();
  }

  private loadBesluiten() {
    this.zakenService
      .listBesluitenForZaak(this.zaak.uuid)
      .subscribe((besluiten) => (this.zaak.besluiten = besluiten));
  }

  private loadTaken() {
    this.takenLoading = true;
    this.takenService
      .listTakenVoorZaak(this.zaak.uuid)
      .pipe(
        map((values) => values.map((value) => new ExpandableTableData(value))),
      )
      .subscribe((taken) => {
        taken = taken.sort(
          (a, b) =>
            (a.data.fataledatum?.localeCompare(b.data.fataledatum ?? "") ||
              a.data.creatiedatumTijd?.localeCompare(
                b.data.creatiedatumTijd ?? "",
              )) ??
            0,
        );
        this.takenDataSource.data = taken;
        this.filterTakenOpStatus();
        this.takenLoading = false;
      });
  }

  private loadNotitieRechten() {
    this.policyService
      .readNotitieRechten()
      .subscribe((rechten) => (this.notitieRechten = rechten));
  }

  expandTaken(expand: boolean) {
    this.takenDataSource.data.forEach((value) => (value.expanded = expand));
    this.checkAllTakenExpanded();
  }

  expandTaak(taak: ExpandableTableData<GeneratedType<"RestTask">>) {
    taak.expanded = !taak.expanded;
    this.checkAllTakenExpanded();
  }

  checkAllTakenExpanded() {
    const filter = this.toonAfgerondeTaken.value
      ? this.takenDataSource.data.filter((value) => !value.expanded)
      : this.takenDataSource.data.filter(
          (value) => value.data.status !== "AFGEROND" && !value.expanded,
        );

    this.allTakenExpanded = filter.length === 0;
  }

  showAssignTaakToMe(taak: GeneratedType<"RestTask">) {
    return Boolean(
      taak.status !== "AFGEROND" &&
        taak.rechten.toekennen &&
        this.loggedInUser.id !== taak.behandelaar?.id &&
        (this.loggedInUser.groupIds ?? []).indexOf(taak.groep?.id ?? "") >= 0,
    );
  }

  initiatorGeselecteerd(initiator: GeneratedType<"RestPersoon">) {
    this.websocketService.suspendListener(this.zaakRollenListener);
    this.actionsSidenav.close();

    if (this.zaak.initiatorIdentificatie) {
      // We already have an initiator, we need a reason to change it
      this.dialog
        .open(DialogComponent, {
          data: new DialogData<unknown, { reden: string }>({
            formFields: [
              new TextareaFormFieldBuilder()
                .id("reden")
                .label("reden")
                .validators(Validators.required)
                .build(),
            ],
            callback: ({ reden }) =>
              this.zakenService.updateInitiator({
                zaakUUID: this.zaak.uuid,
                betrokkeneIdentificatie: new BetrokkeneIdentificatie(initiator),
                toelichting: reden,
              }),
            melding: this.translate.instant("msg.initiator.bevestigen", {
              naam: initiator.naam,
            }),
            icon: "link",
            confirmButtonActionKey: "actie.initiator.wijzigen",
          }),
        })
        .afterClosed()
        .subscribe((zaak) =>
          this.handleNewInitiator("msg.initiator.gewijzigd", zaak),
        );
      return;
    }

    this.zakenService
      .updateInitiator({
        zaakUUID: this.zaak.uuid,
        betrokkeneIdentificatie: new BetrokkeneIdentificatie(initiator),
      })
      .subscribe((zaak) =>
        this.handleNewInitiator("msg.initiator.gekoppeld", zaak),
      );
  }

  private handleNewInitiator(
    notification: string,
    zaak?: GeneratedType<"RestZaak">,
  ) {
    if (!zaak) return;

    this.zaak = zaak;
    this.utilService.openSnackbar(notification, {
      naam:
        zaak.initiatorIdentificatie?.bsnNummer ??
        zaak.initiatorIdentificatie?.kvkNummer ??
        zaak.initiatorIdentificatie?.rsinNummer ??
        zaak.initiatorIdentificatie?.vestigingsnummer,
    });
    this.loadHistorie();
  }

  deleteInitiator() {
    this.websocketService.suspendListener(this.zaakRollenListener);
    this.dialog
      .open(DialogComponent, {
        data: new DialogData<unknown, { reden: string }>({
          formFields: [
            new TextareaFormFieldBuilder()
              .id("reden")
              .label("reden")
              .validators(Validators.required)
              .build(),
          ],
          callback: ({ reden }) =>
            this.zakenService.deleteInitiator(this.zaak.uuid, reden),
          melding: this.translate.instant(
            "msg.initiator.ontkoppelen.bevestigen",
          ),
          confirmButtonActionKey: "actie.initiator.ontkoppelen",
          icon: "link_off",
        }),
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

  betrokkeneGeselecteerd(klantgegevens: KlantGegevens) {
    this.websocketService.suspendListener(this.zaakRollenListener);
    void this.actionsSidenav.close();
    this.zakenService
      .createBetrokkene({
        zaakUUID: this.zaak.uuid,
        roltypeUUID: klantgegevens.betrokkeneRoltype.uuid!,
        roltoelichting: klantgegevens.betrokkeneToelichting,
        betrokkeneIdentificatie: new BetrokkeneIdentificatie(
          klantgegevens.klant,
        ),
      })
      .subscribe((zaak) => {
        this.zaak = zaak;
        this.utilService.openSnackbar("msg.betrokkene.gekoppeld", {
          roltype: klantgegevens.betrokkeneRoltype.naam,
        });
        this.loadHistorie();
        this.loadBetrokkenen();
      });
  }

  deleteBetrokkene(betrokkene: GeneratedType<"RestZaakBetrokkene">) {
    this.websocketService.suspendListener(this.zaakRollenListener);
    const betrokkeneIdentificatie: string =
      betrokkene.roltype + " " + betrokkene.identificatie;
    this.dialog
      .open(DialogComponent, {
        data: new DialogData<unknown, { reden: string }>({
          formFields: [
            new TextareaFormFieldBuilder()
              .id("reden")
              .label("reden")
              .validators(Validators.required)
              .build(),
          ],
          callback: ({ reden }) =>
            this.zakenService.deleteBetrokkene(betrokkene.rolid, reden),
          melding: this.translate.instant(
            "msg.betrokkene.ontkoppelen.bevestigen",
            {
              betrokkene: betrokkeneIdentificatie,
            },
          ),
          confirmButtonActionKey: "actie.betrokkene.ontkoppelen",
          icon: "link_off",
        }),
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

  adresGeselecteerd(bagObject: GeneratedType<"RESTBAGObject">) {
    this.websocketService.suspendListener(this.zaakListener);
    this.bagService
      .create({
        zaakUuid: this.zaak.uuid,
        zaakobject: bagObject,
      })
      .subscribe(() => {
        this.utilService.openSnackbar("msg.bagObject.gekoppeld");
        this.loadHistorie();
        this.loadBagObjecten();
      });
  }

  assignTaakToMe(taak: GeneratedType<"RestTask">, $event: MouseEvent) {
    $event.stopPropagation();

    this.websocketService.suspendListener(this.zaakTakenListener);
    this.takenService
      .toekennenAanIngelogdeMedewerker({
        taakId: taak.id!,
        zaakUuid: taak.zaakUuid,
        groepId: null as unknown as string,
      })
      .subscribe((returnTaak) => {
        taak.behandelaar = returnTaak.behandelaar;
        taak.status = returnTaak.status;
        this.utilService.openSnackbar("msg.taak.toegekend", {
          behandelaar: taak.behandelaar?.naam,
        });
      });
  }

  filterTakenOpStatus() {
    if (!this.toonAfgerondeTaken.value) {
      this.takenStatusFilter = "AFGEROND";
    }

    this.takenDataSource.filter = this.takenStatusFilter;
    SessionStorageUtil.setItem(
      "toonAfgerondeTaken",
      this.toonAfgerondeTaken.value,
    );
  }

  sluitSidenav() {
    this.activeSideAction = null;
    this.actiefPlanItem = null;
    void this.actionsSidenav.close();
  }

  taakGestart() {
    this.sluitSidenav();
    this.updateZaak();
  }

  processGestart() {
    this.sluitSidenav();
    this.updateZaak();
  }

  mailVerstuurd(mailVerstuurd: boolean) {
    this.sluitSidenav();
    if (!mailVerstuurd) return;
    this.updateZaak();
  }

  ontvangstBevestigd(ontvangstBevestigd: boolean) {
    this.sluitSidenav();
    if (!ontvangstBevestigd) return;
    this.updateZaak();
  }

  documentToegevoegd() {
    this.updateZaak();
  }

  documentCreated() {
    this.sluitSidenav();
    this.updateZaak();
  }

  documentSent() {
    this.sluitSidenav();
    this.updateZaak();
  }

  zaakLinked() {
    this.sluitSidenav();
    this.updateZaak();
  }

  locationSelected() {
    this.sluitSidenav();
    this.updateZaak();
  }

  startZaakOntkoppelenDialog(
    gerelateerdeZaak: GeneratedType<"RestGerelateerdeZaak">,
  ) {
    this.dialog
      .open(ZaakOntkoppelenDialogComponent, {
        data: {
          zaakUuid: this.zaak.uuid,
          gekoppeldeZaakIdentificatie: gerelateerdeZaak.identificatie,
          relatieType: gerelateerdeZaak.relatieType,
        },
      })
      .afterClosed()
      .subscribe((result) => {
        this.activeSideAction = null;
        if (!result) return;
        this.utilService.openSnackbar("msg.zaak.ontkoppelen.uitgevoerd");
        this.updateZaak();
      });
  }

  besluitVastgelegd() {
    this.sluitSidenav();
  }

  besluitWijzigen($event: GeneratedType<"RestDecision">) {
    this.activeSideAction = "actie.besluit.wijzigen";
    this.teWijzigenBesluit = $event;
    this.actionsSidenav.open();
  }

  documentMoveToCase(
    $event: Partial<GeneratedType<"RestEnkelvoudigInformatieobject">>,
  ) {
    this.activeSideAction = "actie.document.verplaatsen";
    this.documentToMove = $event;
    this.actionsSidenav.open();
  }

  updateDocumentList() {
    this.zaakDocumentenComponent.updateDocumentList();
    this.loadHistorie();
  }

  doIntrekking($event: {
    uuid: string;
    vervaldatum: string;
    vervalreden: FormControl<string>;
    toelichting: string;
  }) {
    this.zakenService
      .intrekkenBesluit({
        besluitUuid: $event.uuid,
        vervaldatum: $event.vervaldatum,
        vervalreden: $event.vervalreden.value,
        reden: $event.toelichting,
      })
      .subscribe(() => {
        this.utilService.openSnackbar("msg.besluit.ingetrokken");
      });
  }

  betrokkeneGegevensOphalen(
    betrokkene: GeneratedType<"RestZaakBetrokkene"> & {
      gegevens?: string | null;
    },
  ) {
    betrokkene["gegevens"] = "LOADING";
    switch (betrokkene.type) {
      case "NATUURLIJK_PERSOON":
        this.klantenService
          .readPersoon(betrokkene.identificatie, {
            context: this.zaak.uuid,
            action: "list betrokkene",
          })
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
          .readBedrijf(betrokkene.identificatie, betrokkene.kvkNummer ?? null)
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

  bagObjectVerwijderen(
    bagObjectGegevens: GeneratedType<"RESTBAGObjectGegevens">,
  ) {
    const bagObject = bagObjectGegevens.zaakobject;
    const reden = new InputFormFieldBuilder()
      .maxlength(80)
      .id("reden")
      .label("reden")
      .validators(Validators.required)
      .build();
    const dialogData = new DialogData<unknown, { reden: string }>({
      formFields: [reden],
      callback: ({ reden }) =>
        this.bagService
          .delete({
            redenWijzigen: reden,
            bagObject,
            uuid: bagObjectGegevens.uuid,
            zaakUuid: this.zaak.uuid,
          })
          .pipe(
            tap(() => this.websocketService.suspendListener(this.zaakListener)),
          ),
      uitleg: this.translate.instant("msg.bagObject.ontkoppelen.bevestigen", {
        omschrijving: bagObject?.omschrijving,
      }),
      confirmButtonActionKey: "actie.bagObject.ontkoppelen",
      icon: "link_off",
    });

    this.dialog
      .open(DialogComponent, { data: dialogData })
      .afterClosed()
      .subscribe((result) => {
        this.activeSideAction = null;
        if (result) {
          this.loadHistorie();
          this.loadBagObjecten();
          this.utilService.openSnackbar(
            "msg.bagObject.ontkoppelen.uitgevoerd",
            { omschrijving: bagObject?.omschrijving },
          );
        }
      });
  }

  showProces() {
    const dialogData = new DialogData({
      formFields: [
        new ReadonlyFormFieldBuilder(
          '<img src="/rest/zaken/' +
            this.zaak.uuid +
            '/procesdiagram"/ alt="diagram">',
        )
          .id("diagram")
          .label("proces.toestand")
          .build(),
      ],
      confirmButtonActionKey: "actie.ok",
      cancelButtonActionKey: null,
      icon: "play_shapes",
    });
    dialogData.confirmButtonActionKey = "actie.ok";
    dialogData.cancelButtonActionKey = null;
    this.dialog.open(DialogComponent, { data: dialogData });
  }

  hasZaakData() {
    return this.zaak.zaakdata && Object.keys(this.zaak.zaakdata).length > 0;
  }

  protected taskStatusChipColor(status: GeneratedType<"TaakStatus">) {
    switch (status) {
      case "AFGEROND":
        return "success";
      case "TOEGEKEND":
        return "primary";
      default:
        return "";
    }
  }

  async menuItemChanged(event: string | null) {
    this.activeSideAction = event;
  }

  protected showInitiator() {
    if (!this.zaak.zaaktype.zaakafhandelparameters?.betrokkeneKoppelingen)
      return false;

    const { brpKoppelen, kvkKoppelen } =
      this.zaak.zaaktype.zaakafhandelparameters.betrokkeneKoppelingen;

    return Boolean(brpKoppelen || kvkKoppelen);
  }

  protected showPersoonsgegevens() {
    if (!this.zaak.zaaktype.zaakafhandelparameters?.betrokkeneKoppelingen)
      return false;

    const { brpKoppelen } =
      this.zaak.zaaktype.zaakafhandelparameters.betrokkeneKoppelingen;

    return Boolean(
      brpKoppelen &&
        ["BSN"].includes(this.zaak.initiatorIdentificatie?.type ?? ""),
    );
  }

  protected showBedrijfsgegevens() {
    if (!this.zaak.zaaktype.zaakafhandelparameters?.betrokkeneKoppelingen)
      return false;

    const { kvkKoppelen } =
      this.zaak.zaaktype.zaakafhandelparameters.betrokkeneKoppelingen;

    return Boolean(
      kvkKoppelen &&
        ["VN", "RSIN"].includes(this.zaak.initiatorIdentificatie?.type ?? ""),
    );
  }

  protected allowedToAddBetrokkene() {
    const brpAllowed =
      !!this.zaak.zaaktype.zaakafhandelparameters?.betrokkeneKoppelingen
        ?.brpKoppelen && this.zaak.rechten.toevoegenInitiatorPersoon;
    const kvkAllowed =
      !!this.zaak.zaaktype.zaakafhandelparameters?.betrokkeneKoppelingen
        ?.kvkKoppelen && this.zaak.rechten.toevoegenInitiatorBedrijf;

    return Boolean(brpAllowed || kvkAllowed);
  }

  protected allowBedrijf() {
    return Boolean(
      this.zaak.rechten.toevoegenInitiatorBedrijf &&
        this.zaak.zaaktype.zaakafhandelparameters?.betrokkeneKoppelingen
          ?.kvkKoppelen,
    );
  }

  protected allowPersoon() {
    return Boolean(
      this.zaak.rechten.toevoegenInitiatorPersoon &&
        this.zaak.zaaktype.zaakafhandelparameters?.betrokkeneKoppelingen
          ?.brpKoppelen,
    );
  }

  protected showBetrokkeneKoppelingen() {
    const brpAllowed =
      !!this.zaak.zaaktype.zaakafhandelparameters?.betrokkeneKoppelingen
        ?.brpKoppelen;
    const kvkAllowed =
      !!this.zaak.zaaktype.zaakafhandelparameters?.betrokkeneKoppelingen
        ?.kvkKoppelen;

    return Boolean(brpAllowed || kvkAllowed) && !!this.betrokkenen.data.length;
  }

  protected isAfterDate(datum: Date | moment.Moment | string) {
    return DateConditionals.isExceeded(datum);
  }
}

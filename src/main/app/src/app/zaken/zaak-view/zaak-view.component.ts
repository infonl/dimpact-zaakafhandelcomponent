/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024-2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentType } from "@angular/cdk/portal";
import {
  AfterViewInit,
  Component,
  inject,
  OnDestroy,
  TemplateRef,
  viewChild,
  ViewChild,
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormControl, FormGroup, Validators } from "@angular/forms";
import { MatDialog } from "@angular/material/dialog";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import { MatSort } from "@angular/material/sort";
import { MatTableDataSource } from "@angular/material/table";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { injectQuery, QueryClient } from "@tanstack/angular-query-experimental";
import moment from "moment";
import { defer, forkJoin } from "rxjs";
import { tap } from "rxjs/operators";
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
import { openGenericDialog } from "../../shared/dialog/generic-dialog/generic-dialog.component";
import { TextIcon } from "../../shared/edit/text-icon";
import { IndicatiesLayout } from "../../shared/indicaties/indicaties.component";
import { InputFormFieldBuilder } from "../../shared/material-form-builder/form-components/input/input-form-field-builder";
import { TextareaFormFieldBuilder } from "../../shared/material-form-builder/form-components/textarea/textarea-form-field-builder";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { ButtonMenuItem } from "../../shared/side-nav/menu-item/button-menu-item";
import { HeaderMenuItem } from "../../shared/side-nav/menu-item/header-menu-item";
import { MenuItem } from "../../shared/side-nav/menu-item/menu-item";
import { GeneratedType } from "../../shared/utils/generated-types";
import { IntakeAfrondenDialogComponent } from "../intake-afronden-dialog/intake-afronden-dialog.component";
import { BetrokkeneIdentificatie } from "../model/betrokkeneIdentificatie";
import { ZaakAfhandelenDialogComponent } from "../zaak-afhandelen-dialog/zaak-afhandelen-dialog.component";
import { ZaakDocumentenComponent } from "../zaak-documenten/zaak-documenten.component";
import { ZaakOntkoppelenDialogComponent } from "../zaak-ontkoppelen/zaak-ontkoppelen-dialog.component";
import { ZaakOpschortenDialogComponent } from "../zaak-opschorten-dialog/zaak-opschorten-dialog.component";
import { ZaakTakenComponent } from "../zaak-taken/zaak-taken.component";
import { ZaakVerlengenDialogComponent } from "../zaak-verlengen-dialog/zaak-verlengen-dialog.component";
import { ZakenService } from "../zaken.service";

type InitiatorViewType = "PERSON" | "COMPANY" | "CONTACT_DETAILS" | "ADD";

type RedenForm = FormGroup<{ reden: FormControl<string | null> }>;
type AfbrekenForm = FormGroup<{
  reden: FormControl<GeneratedType<"RestZaakbeeindigReden"> | null>;
}>;

@Component({
  templateUrl: "./zaak-view.component.html",
  styleUrls: ["./zaak-view.component.less"],
  animations: [detailExpand],
  standalone: false,
})
export class ZaakViewComponent
  extends ActionsViewComponent
  implements AfterViewInit, OnDestroy
{
  private readonly queryClient = inject(QueryClient);

  readonly indicatiesLayout = IndicatiesLayout;
  zaak!: GeneratedType<"RestZaak">;
  zaakOpschorting!: GeneratedType<"RESTZaakOpschorting">;
  menu: MenuItem[] = [];
  actiefPlanItem: GeneratedType<"RESTPlanItem"> | null = null;
  activeSideAction: string | null = null;
  teWijzigenBesluit!: GeneratedType<"RestBesluit">;
  documentToMove!: Partial<GeneratedType<"RestEnkelvoudigInformatieobject">>;

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

  private zaakListener!: WebsocketListener;
  private zaakRollenListener!: WebsocketListener;
  private zaakBesluitenListener!: WebsocketListener;
  private datumPipe = new DatumPipe("nl");

  @ViewChild("actionsSidenav") actionsSidenav!: MatSidenav;
  @ViewChild("menuSidenav") menuSidenav!: MatSidenav;
  @ViewChild("sideNavContainer") sideNavContainer!: MatSidenavContainer;

  @ViewChild("historieSort") historieSort!: MatSort;

  private readonly heropenenDialogTemplate = viewChild.required<
    TemplateRef<{ $implicit: RedenForm }>
  >("heropenenDialogTemplate");
  private readonly afbrekenDialogTemplate = viewChild.required<
    TemplateRef<{ $implicit: AfbrekenForm }>
  >("afbrekenDialogTemplate");
  private readonly hervattenDialogTemplate = viewChild.required<
    TemplateRef<{ $implicit: RedenForm }>
  >("hervattenDialogTemplate");
  private readonly initiatorWijzigenDialogTemplate = viewChild.required<
    TemplateRef<{ $implicit: RedenForm }>
  >("initiatorWijzigenDialogTemplate");

  // Loaded lazily on subscribe so the current zaaktype is used each time the afbreken dialog opens.
  protected readonly afbrekenRedenen = defer(() =>
    this.zaakafhandelParametersService.listZaakbeeindigRedenenForZaaktype(
      this.zaak.zaaktype.uuid,
    ),
  );
  @ViewChild("zaakDocumentenComponent")
  zaakDocumentenComponent!: ZaakDocumentenComponent;
  @ViewChild("zaakTakenComponent")
  private zaakTakenComponent!: ZaakTakenComponent;

  protected readonly loggedInUser = injectQuery(() =>
    this.identityService.readLoggedInUser(),
  );

  protected readonly brpRechtenQuery = injectQuery(() =>
    this.policyService.readBrpRechten(),
  );
  constructor(
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
    this.route.data.pipe(takeUntilDestroyed()).subscribe((data) => {
      const zaak = data["zaak"] as GeneratedType<"RestZaak">;
      this.init(zaak);

      this.zaakListener = this.websocketService.addListenerWithSnackbar(
        Opcode.ANY,
        ObjectType.ZAAK,
        zaak.uuid,
        () => this.updateZaak(),
      );

      this.zaakRollenListener = this.websocketService.addListenerWithSnackbar(
        Opcode.UPDATED,
        ObjectType.ZAAK_ROLLEN,
        zaak.uuid,
        () => this.updateZaak(),
      );

      this.zaakBesluitenListener =
        this.websocketService.addListenerWithSnackbar(
          Opcode.UPDATED,
          ObjectType.ZAAK_BESLUITEN,
          zaak.uuid,
          () => this.loadBesluiten(),
        );

      this.utilService.setTitle("title.zaak", {
        zaak: zaak.identificatie,
      });

      this.loadNotitieRechten();
    });
  }

  private init(zaak: GeneratedType<"RestZaak">) {
    this.zaak = zaak;
    this.loadHistorie();
    this.loadBetrokkenen();
    this.loadBagObjecten();
    this.setupMenu();
    this.loadOpschorting();
    this.setDateFieldIconSet();
    ViewResourceUtil.actieveZaak = zaak;
  }

  ngAfterViewInit() {
    this.viewInitialized = true;
    super.ngAfterViewInit();

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
          .enabledForZaaktype &&
        this.zaak.zaaktype.zaakafhandelparameters?.smartDocuments
          .enabledGlobally
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

    if (this.hasZaakData() && this.zaak.rechten.bekijkenZaakdata) {
      this.menu.push(
        new ButtonMenuItem(
          "actie.zaakdata.bekijken",
          () => this.actionsSidenav.open(),
          "folder_copy",
        ),
      );
    }

    if (this.zaak.bpmnProcessDefinition) {
      this.menu.push(
        new ButtonMenuItem(
          "actie.procesverloop.bekijken",
          () => this.actionsSidenav.open(),
          "play_shapes",
        ),
      );
    }

    const menuSubscription = forkJoin([
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

        if (this.zaak.rechten.behandelen) {
          if (userEventListenerPlanItems.length || actionMenuItems.length) {
            this.menu.push(new HeaderMenuItem("actie.zaak.acties"));
          }
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

        if (this.zaak.rechten.behandelen) {
          if (humanTaskPlanItems.length) {
            this.menu.push(new HeaderMenuItem("actie.taak.starten"));
          }
          this.menu = this.menu.concat(
            humanTaskPlanItems.map((humanTaskPlanItem) =>
              this.createPlanItemMenuItem(humanTaskPlanItem, "assignment"),
            ),
          );

          if (processTaskPlanItems.length) {
            this.menu.push(new HeaderMenuItem("actie.proces.starten"));
          }
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

    this.subscriptions$.push(menuSubscription);
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
      !this.zaak.eerdereOpschorting
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

  private openPlanItemStartenDialog(planItem: GeneratedType<"RESTPlanItem">) {
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

  private createUserEventListenerDialog(
    planItem: GeneratedType<"RESTPlanItem">,
  ): {
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

  private createUserEventListenerIntakeAfrondenDialog(
    planItem: GeneratedType<"RESTPlanItem">,
  ) {
    return {
      dialogComponent: IntakeAfrondenDialogComponent,
      dialogData: { zaak: this.zaak, planItem: planItem },
    };
  }

  private createUserEventListenerZaakAfhandelenDialog(
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

    const form: AfbrekenForm = new FormGroup({
      reden: new FormControl<GeneratedType<"RestZaakbeeindigReden"> | null>(
        null,
        Validators.required,
      ),
    });

    openGenericDialog(this.dialog, {
      form,
      contentTemplate: this.afbrekenDialogTemplate(),
      callback: () =>
        this.zakenService
          .afbreken(this.zaak.uuid, {
            zaakbeeindigRedenId: form.getRawValue().reden?.id ?? "",
          })
          .pipe(
            tap(() => this.websocketService.suspendListener(this.zaakListener)),
          ),
      confirmButtonActionKey: "actie.zaak.afbreken",
      icon: "thumb_down_alt",
    })
      .afterClosed()
      .subscribe((result) => {
        this.activeSideAction = null;
        if (result) {
          this.updateZaak();
          this.zaakTakenComponent.reload();
          this.utilService.openSnackbar("msg.zaak.afgebroken");
        }
      });
  }

  private openZaakHeropenenDialog() {
    const form: RedenForm = new FormGroup({
      reden: new FormControl<string | null>(null, [
        Validators.required,
        Validators.maxLength(100),
      ]),
    });

    openGenericDialog(this.dialog, {
      form,
      contentTemplate: this.heropenenDialogTemplate(),
      callback: () =>
        this.zakenService
          .heropenen(this.zaak.uuid, {
            reden: form.getRawValue().reden ?? "",
          })
          .pipe(
            tap(() => this.websocketService.suspendListener(this.zaakListener)),
          ),
      confirmButtonActionKey: "actie.zaak.heropenen",
      icon: "restart_alt",
    })
      .afterClosed()
      .subscribe((result) => {
        this.activeSideAction = null;
        if (result) {
          this.updateZaak();
          this.zaakTakenComponent.reload();
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
        this.zaakTakenComponent.reload();
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

    const form: RedenForm = new FormGroup({
      reden: new FormControl<string | null>(null, [
        Validators.required,
        Validators.maxLength(200),
      ]),
    });

    openGenericDialog(this.dialog, {
      form,
      contentTemplate: this.hervattenDialogTemplate(),
      callback: () =>
        this.zakenService.resumeZaak(this.zaak.uuid, {
          reason: form.getRawValue().reden ?? "",
        }),
      melding: this.translate.instant("msg.zaak.hervatten", {
        duur: werkelijkeOpschortDuur,
        verwachteDuur: this.zaakOpschorting.duurDagen,
      }),
      confirmButtonActionKey: "actie.zaak.hervatten",
      icon: "play_circle",
    })
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

  protected editCaseDetails() {
    if (this.zaak.rechten.wijzigen || this.zaak.rechten.toekennen) {
      this.activeSideAction = "actie.zaak.wijzigen";
      this.actionsSidenav.open();
    }
  }

  protected editLocationDetails() {
    if (this.zaak.rechten.wijzigen) {
      this.activeSideAction = "actie.zaak.locatie.koppelen";
      this.actionsSidenav.open();
    }
  }

  protected addOrEditZaakInitiator() {
    this.activeSideAction = "actie.initiator.koppelen";
    this.actionsSidenav.open();
  }

  private loadBesluiten() {
    this.zakenService
      .listBesluitenForZaak(this.zaak.uuid)
      .subscribe((besluiten) => (this.zaak.besluiten = besluiten));
  }

  private loadNotitieRechten() {
    this.policyService
      .readNotitieRechten()
      .subscribe((rechten) => (this.notitieRechten = rechten));
  }

  protected initiatorGeselecteerd(initiator: GeneratedType<"RestPersoon">) {
    this.websocketService.suspendListener(this.zaakRollenListener);
    this.actionsSidenav.close();

    if (this.zaak.initiatorIdentificatie) {
      // We already have an initiator, we need a reason to change it
      const form: RedenForm = new FormGroup({
        reden: new FormControl<string | null>(null, Validators.required),
      });

      openGenericDialog(this.dialog, {
        form,
        contentTemplate: this.initiatorWijzigenDialogTemplate(),
        callback: () =>
          this.zakenService.updateInitiator({
            zaakUUID: this.zaak.uuid,
            betrokkeneIdentificatie: new BetrokkeneIdentificatie(initiator),
            toelichting: form.getRawValue().reden ?? "",
          }),
        melding: this.translate.instant("msg.initiator.bevestigen", {
          naam: initiator.naam,
        }),
        icon: "link",
        confirmButtonActionKey: "actie.initiator.wijzigen",
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
    const naam = [
      zaak.initiatorIdentificatie?.kvkNummer,
      zaak.initiatorIdentificatie?.vestigingsnummer,
    ].filter(Boolean);
    this.utilService.openSnackbar(notification, {
      naam: naam.join(" - "),
    });
    this.loadHistorie();
  }

  protected deleteInitiator() {
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

  protected betrokkeneGeselecteerd(klantgegevens: KlantGegevens) {
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

  protected deleteBetrokkene(betrokkene: GeneratedType<"RestZaakBetrokkene">) {
    this.websocketService.suspendListener(this.zaakRollenListener);
    const betrokkeneIdentificatie: string =
      betrokkene.roltype +
      " " +
      (betrokkene.vestigingsnummer ??
        betrokkene.kvkNummer ??
        betrokkene.bsn ??
        betrokkene.naam);
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

  protected adresGeselecteerd(bagObject: GeneratedType<"RESTBAGObject">) {
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

  private sluitSidenav() {
    this.activeSideAction = null;
    this.actiefPlanItem = null;
    void this.actionsSidenav.close();
  }

  protected taakGestart() {
    this.sluitSidenav();
    this.updateZaak();
  }

  protected processGestart() {
    this.sluitSidenav();
    this.updateZaak();
  }

  protected mailVerstuurd(mailVerstuurd: boolean) {
    this.sluitSidenav();
    if (!mailVerstuurd) return;
    this.updateZaak();
  }

  protected ontvangstBevestigd(ontvangstBevestigd: boolean) {
    this.sluitSidenav();
    if (!ontvangstBevestigd) return;
    this.updateZaak();
  }

  protected documentToegevoegd() {
    this.updateZaak();
  }

  protected documentCreated() {
    this.sluitSidenav();
    this.updateZaak();
  }

  protected documentSent() {
    this.sluitSidenav();
    this.updateZaak();
  }

  protected zaakLinked() {
    this.sluitSidenav();
    this.updateZaak();
  }

  protected locationSelected() {
    this.sluitSidenav();
    this.updateZaak();
  }

  protected startZaakOntkoppelenDialog(
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

  protected besluitVastgelegd() {
    this.sluitSidenav();
  }

  protected besluitWijzigen($event: GeneratedType<"RestBesluit">) {
    this.activeSideAction = "actie.besluit.wijzigen";
    this.teWijzigenBesluit = $event;
    this.actionsSidenav.open();
  }

  protected documentMoveToCase(
    $event: Partial<GeneratedType<"RestEnkelvoudigInformatieobject">>,
  ) {
    this.activeSideAction = "actie.document.verplaatsen";
    this.documentToMove = $event;
    this.actionsSidenav.open();
  }

  protected updateDocumentList() {
    this.zaakDocumentenComponent.updateDocumentList();
    this.loadHistorie();
  }

  protected async betrokkeneGegevensOphalen(
    betrokkene: GeneratedType<"RestZaakBetrokkene"> & {
      gegevens?: string | null;
    },
  ) {
    betrokkene["gegevens"] = "LOADING";
    switch (betrokkene.type) {
      case "NATUURLIJK_PERSOON": {
        const persoon = await this.queryClient.ensureQueryData(
          this.klantenService.readPersoon(
            betrokkene.temporaryPersonId!,
            this.zaak.zaaktype.uuid,
          ),
        );
        betrokkene["gegevens"] = persoon.naam;
        if (persoon.geboortedatum) {
          betrokkene["gegevens"] += `, ${this.datumPipe.transform(
            persoon.geboortedatum,
          )}`;
        }
        if (persoon.verblijfplaats)
          betrokkene["gegevens"] += `,\n${persoon.verblijfplaats}`;
        break;
      }
      case "NIET_NATUURLIJK_PERSOON":
      case "VESTIGING": {
        const betrokkeneIdentificatie = new BetrokkeneIdentificatie(betrokkene);

        const bedrijf = await this.queryClient.ensureQueryData(
          this.klantenService.readBedrijf(betrokkeneIdentificatie),
        );

        if (!bedrijf) return;

        betrokkene["gegevens"] = bedrijf.naam;
        if (bedrijf.adres?.volledigAdres)
          betrokkene["gegevens"] += `,\n${bedrijf.adres.volledigAdres}`;
        break;
      }
      case "ORGANISATORISCHE_EENHEID":
      case "MEDEWERKER": {
        betrokkene["gegevens"] = "-";
        break;
      }
    }
  }

  protected bagObjectVerwijderen(
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

  private hasZaakData() {
    return this.zaak.zaakdata && Object.keys(this.zaak.zaakdata).length > 0;
  }

  protected async menuItemChanged(event: string | null) {
    this.activeSideAction = event;
  }

  protected showInitiator() {
    if (this.hasZaakSpecificContactDetails()) return true;

    if (!this.zaak.zaaktype.zaakafhandelparameters?.betrokkeneKoppelingen)
      return false;

    const { brpKoppelen, kvkKoppelen } =
      this.zaak.zaaktype.zaakafhandelparameters.betrokkeneKoppelingen;

    return Boolean(brpKoppelen || kvkKoppelen);
  }

  protected initiatorViewType(): InitiatorViewType {
    const koppelingen =
      this.zaak.zaaktype.zaakafhandelparameters?.betrokkeneKoppelingen;

    if (koppelingen) {
      const type = this.zaak.initiatorIdentificatie?.type ?? "";
      if (koppelingen.brpKoppelen && ["BSN"].includes(type)) return "PERSON";
      if (koppelingen.kvkKoppelen && ["VN", "RSIN"].includes(type))
        return "COMPANY";
    }

    if (this.hasZaakSpecificContactDetails()) return "CONTACT_DETAILS";

    return "ADD";
  }

  private hasZaakSpecificContactDetails(): boolean {
    const { zaakSpecificContactDetails } = this.zaak;
    return !!(
      zaakSpecificContactDetails?.telephoneNumber ||
      zaakSpecificContactDetails?.emailAddress
    );
  }

  protected allowedToAddBetrokkene() {
    const brpAllowed =
      !!this.zaak.zaaktype.zaakafhandelparameters?.betrokkeneKoppelingen
        ?.brpKoppelen && this.zaak.rechten.toevoegenInitiatorPersoon;
    const kvkAllowed =
      !!this.zaak.zaaktype.zaakafhandelparameters?.betrokkeneKoppelingen
        ?.kvkKoppelen && this.zaak.rechten.toevoegenInitiatorBedrijf;

    return Boolean(
      (brpAllowed && this.brpRechtenQuery.data()?.zoeken) || kvkAllowed,
    );
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
          ?.brpKoppelen &&
        this.brpRechtenQuery.data()?.zoeken,
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

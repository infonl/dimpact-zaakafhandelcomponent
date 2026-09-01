/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024-2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  Component,
  computed,
  effect,
  inject,
  OnDestroy,
  signal,
  untracked,
  ViewChild,
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { MatDialog } from "@angular/material/dialog";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { injectQuery, QueryClient } from "@tanstack/angular-query-experimental";
import { forkJoin } from "rxjs";
import { PolicyService } from "src/app/policy/policy.service";
import { ZaakafhandelParametersService } from "../../admin/zaakafhandel-parameters.service";
import { BAGService } from "../../bag/bag.service";
import { UtilService } from "../../core/service/util.service";
import { isCausedByCurrentUser } from "../../core/websocket/is-caused-by-current-user";
import { ObjectType } from "../../core/websocket/model/object-type";
import { Opcode } from "../../core/websocket/model/opcode";
import { ScreenEvent } from "../../core/websocket/model/screen-event";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { KlantGegevens } from "../../klanten/model/klanten/klant-gegevens";
import { ViewResourceUtil } from "../../locatie/view-resource.util";
import { PlanItemsService } from "../../plan-items/plan-items.service";
import { ActionsViewComponent } from "../../shared/abstract-view/actions-view-component";
import { detailExpand } from "../../shared/animations/animations";
import { runMutation } from "../../shared/http/run-mutation";
import { MenuItem } from "../../shared/side-nav/menu-item/menu-item";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TakenService } from "../../taken/taken.service";
import { BetrokkeneIdentificatie } from "../model/betrokkeneIdentificatie";
import { ZaakDialogService } from "../zaak-dialog.service";
import { ZaakDocumentenComponent } from "../zaak-documenten/zaak-documenten.component";
import { ZakenService } from "../zaken.service";
import { ZaakActionDialogsService } from "./services/zaak-action-dialogs.service";
import { ZaakSideActionService } from "./services/zaak-side-action.service";
import {
  buildZaakMenu,
  ZaakMenuHandlers,
} from "./utils/zaak-view-menu.builder";
import {
  allowBedrijf,
  allowedToAddBetrokkene,
  allowPersoon,
  initiatorViewType,
  showBetrokkeneKoppelingen,
  showInitiator,
} from "./utils/zaak-view.predicates";

@Component({
  templateUrl: "./zaak-view.component.html",
  animations: [detailExpand],
  standalone: false,
  providers: [ZaakSideActionService, ZaakActionDialogsService],
})
export class ZaakViewComponent
  extends ActionsViewComponent
  implements AfterViewInit, OnDestroy
{
  private readonly queryClient = inject(QueryClient);

  private readonly zaakUuid = signal<string | undefined>(undefined);

  private readonly zaakQuery = injectQuery(() => {
    const uuid = this.zaakUuid();
    return {
      ...this.zakenService.readZaakQuery(uuid ?? ""),
      enabled: Boolean(uuid),
    };
  });

  get zaak(): GeneratedType<"RestZaak"> {
    return this.zaakQuery.data()!;
  }

  // Narrowed off `zaakQuery.data()` so this only changes value (and re-runs
  // dependent effects) when `isOpgeschort` itself changes, not on every
  // unrelated zaak content write.
  private readonly isOpgeschort = computed(
    () => this.zaakQuery.data()?.isOpgeschort,
  );

  menu: MenuItem[] = [];

  bagObjecten: GeneratedType<"RESTBAGObjectGegevens">[] = [];
  gekoppeldeBagObjecten: GeneratedType<"RESTBAGObject">[] = [];
  teWijzigenBesluit!: GeneratedType<"RestBesluit">;
  documentToMove!: Partial<GeneratedType<"RestEnkelvoudigInformatieobject">>;

  notitieRechten!: GeneratedType<"RestNotitieRechten">;
  viewInitialized = false;

  private zaakListener!: WebsocketListener;
  private zaakRollenListener!: WebsocketListener;
  private zaakBesluitenListener!: WebsocketListener;
  private zaakTakenListener!: WebsocketListener;

  @ViewChild("actionsSidenav") actionsSidenav!: MatSidenav;
  @ViewChild("menuSidenav") menuSidenav!: MatSidenav;
  @ViewChild("sideNavContainer") sideNavContainer!: MatSidenavContainer;

  @ViewChild("zaakDocumentenComponent")
  zaakDocumentenComponent!: ZaakDocumentenComponent;

  protected readonly loggedInUser = injectQuery(() =>
    this.identityService.readLoggedInUser(),
  );

  protected readonly brpRechtenQuery = injectQuery(() =>
    this.policyService.readBrpRechten(),
  );

  protected readonly betrokkenenQuery = injectQuery(() =>
    this.zakenService.listBetrokkenenVoorZaakQuery(this.zaak.uuid),
  );

  constructor(
    private zakenService: ZakenService,
    private identityService: IdentityService,
    private planItemsService: PlanItemsService,
    private zaakafhandelParametersService: ZaakafhandelParametersService,
    private route: ActivatedRoute,
    private utilService: UtilService,
    private websocketService: WebsocketService,
    private dialog: MatDialog,
    private translate: TranslateService,
    private bagService: BAGService,
    private policyService: PolicyService,
    private zaakDialogService: ZaakDialogService,
    private takenService: TakenService,
    protected sideActions: ZaakSideActionService,
    protected dialogs: ZaakActionDialogsService,
  ) {
    super();
    this.route.data.pipe(takeUntilDestroyed()).subscribe((data) => {
      const zaak = data["zaak"] as GeneratedType<"RestZaak">;
      this.zakenService.cacheZaak(zaak);
      this.zaakUuid.set(zaak.uuid);

      this.zaakListener = this.websocketService.addListener(
        Opcode.ANY,
        ObjectType.ZAAK,
        zaak.uuid,
        (event) => {
          void this.onZaakChanged(event).catch((error: unknown) =>
            console.error("Websocket zaak listener error: ", error),
          );
        },
      );

      this.zaakRollenListener = this.websocketService.addListenerWithSnackbar(
        Opcode.UPDATED,
        ObjectType.ZAAK_ROLLEN,
        zaak.uuid,
        () => {
          this.invalidateBetrokkenen();
          this.invalidateZaakHistorie();
          this.updateZaak();
        },
      );

      this.zaakBesluitenListener =
        this.websocketService.addListenerWithSnackbar(
          Opcode.UPDATED,
          ObjectType.ZAAK_BESLUITEN,
          zaak.uuid,
          () => this.loadBesluiten(),
        );

      this.zaakTakenListener = this.websocketService.addListener(
        Opcode.UPDATED,
        ObjectType.ZAAK_TAKEN,
        zaak.uuid,
        () => this.setupMenu(),
      );

      this.utilService.setTitle("title.zaak", {
        zaak: zaak.identificatie,
      });

      this.loadNotitieRechten();
    });

    effect(() => {
      const uuid = this.zaakUuid();
      if (!uuid) return;
      // loadBagObjecten reads the whole zaak getter internally (for .uuid) —
      // untracked so that doesn't also make this effect re-run on unrelated
      // content changes.
      untracked(() => this.loadBagObjecten());
    });

    effect(() => {
      const isOpgeschort = this.isOpgeschort();
      if (isOpgeschort === undefined) return;
      untracked(() => this.dialogs.loadOpschorting(this.zaak));
    });

    effect(() => {
      const zaak = this.zaakQuery.data();
      if (!zaak) return;
      this.invalidateZaakHistorie();
      this.setupMenu();
      ViewResourceUtil.actieveZaak = zaak;
    });
  }

  ngAfterViewInit() {
    this.sideActions.register(this.actionsSidenav);
    this.viewInitialized = true;
    super.ngAfterViewInit();
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    ViewResourceUtil.actieveZaak = null;
    this.websocketService.removeListener(this.zaakListener);
    this.websocketService.removeListener(this.zaakBesluitenListener);
    this.websocketService.removeListener(this.zaakRollenListener);
    this.websocketService.removeListener(this.zaakTakenListener);
  }

  private setupMenu() {
    this.menu = buildZaakMenu(this.zaak, null, this.menuHandlers, false);

    const menuSubscription = forkJoin([
      this.planItemsService.listUserEventListenerPlanItems(this.zaak.uuid),
      this.planItemsService.listHumanTaskPlanItems(this.zaak.uuid),
    ]).subscribe(([userEventListenerPlanItems, humanTaskPlanItems]) => {
      this.menu = buildZaakMenu(
        this.zaak,
        {
          userEventListener: userEventListenerPlanItems,
          humanTask: humanTaskPlanItems,
        },
        this.menuHandlers,
        this.hasBrpSearchRight(),
      );
      this.updateMargins();
    });

    this.subscriptions$.push(menuSubscription);
  }

  private readonly menuHandlers: ZaakMenuHandlers = {
    openSideAction: () => this.sideActions.open(),
    startHumanTask: (planItem) => this.startHumanTaskPlanItem(planItem),
    startUserEventListener: (planItem) =>
      this.dialogs.openPlanItemStarten(this.zaak, planItem),
    heropenen: () => this.dialogs.openHeropenen(this.zaak),
    opschorten: () => this.dialogs.openOpschorten(this.zaak),
    verlengen: () => this.dialogs.openVerlengen(this.zaak),
    hervatten: () => this.dialogs.openHervatten(this.zaak),
    afbreken: () => this.dialogs.openAfbreken(this.zaak),
    afsluiten: () => this.dialogs.openAfsluiten(this.zaak),
    brondatumZetten: () => this.dialogs.openBrondatumZetten(this.zaak),
  };

  private startHumanTaskPlanItem(planItem: GeneratedType<"RESTPlanItem">) {
    const actiefPlanItem = this.sideActions.actiefPlanItem();
    if (!actiefPlanItem || actiefPlanItem.id !== planItem.id) {
      this.sideActions.clear();
      this.planItemsService
        .readHumanTaskPlanItem(planItem.id)
        .subscribe((planItem) => {
          this.sideActions.actiefPlanItem.set(planItem);
          this.sideActions.open(planItem.naam);
        });
      return;
    }
    this.sideActions.open(planItem.naam);
  }

  public updateZaak() {
    this.dialogs.refreshZaak(this.zaak);
  }

  /**
   * Refetches the zaak and only stays quiet when the refetch succeeded and
   * its content is unchanged from what was already cached. TanStack's
   * structural sharing keeps the same object reference when a refetch
   * returns a deep-equal payload, so a reference comparison is enough to
   * tell a genuine change made by someone else from the echo of our own
   * save (which already updated the cache from the save's own response). A
   * failed refetch is never treated as an echo, so it still announces the
   * change rather than silently doing nothing.
   */
  private async onZaakChanged(event: ScreenEvent) {
    const queryKey = this.zakenService.readZaakQuery(this.zaak.uuid).queryKey;
    const zaakBeforeRefetch = this.queryClient.getQueryData(queryKey);

    await this.queryClient.refetchQueries({ queryKey });

    // Not part of RestZaak, so the echo check below says nothing about these.
    this.loadBagObjecten();
    this.dialogs.loadOpschorting(this.zaak);
    this.invalidateZaakHistorie();

    const refetchSucceeded =
      this.queryClient.getQueryState(queryKey)?.status === "success";
    const zaakAfterRefetch = this.queryClient.getQueryData(queryKey);
    if (refetchSucceeded && zaakAfterRefetch === zaakBeforeRefetch) return;
    if (isCausedByCurrentUser(event, this.loggedInUser.data()?.id)) return;

    forkJoin({
      msgPart1: this.translate.get(
        "msg.gewijzigd.objecttype." + event.objectType,
      ),
      msgPart2: this.translate.get(
        event.objectType.indexOf("_") < 0
          ? "msg.gewijzigd.2"
          : "msg.gewijzigd.2.details",
      ),
      msgPart3: this.translate.get("msg.gewijzigd.operatie." + event.opcode),
      msgPart4: this.translate.get("msg.gewijzigd.4"),
    }).subscribe((result) => {
      this.utilService.openSnackbar(
        result.msgPart1 + result.msgPart2 + result.msgPart3 + result.msgPart4,
      );
    });
  }

  private invalidateZaakTaken() {
    this.queryClient.invalidateQueries({
      queryKey: this.takenService.listTakenVoorZaakQuery(this.zaak.uuid)
        .queryKey,
    });
  }

  private invalidateZaakHistorie() {
    this.queryClient.invalidateQueries({
      queryKey: this.zakenService.listHistorieVoorZaakQuery(this.zaak.uuid)
        .queryKey,
    });
  }

  protected editCaseDetails() {
    if (this.zaak.rechten.wijzigen || this.zaak.rechten.toekennen) {
      this.sideActions.open("actie.zaak.wijzigen");
    }
  }

  protected editLocationDetails() {
    if (this.zaak.rechten.wijzigen) {
      this.sideActions.open("actie.zaak.locatie.koppelen");
    }
  }

  protected addOrEditZaakInitiator() {
    this.sideActions.open("actie.initiator.koppelen");
  }

  private loadBesluiten() {
    this.zakenService
      .listBesluitenForZaak(this.zaak.uuid)
      .subscribe((besluiten) =>
        this.zakenService.cacheZaak({ ...this.zaak, besluiten }),
      );
  }

  private loadNotitieRechten() {
    this.policyService
      .readNotitieRechten()
      .subscribe((rechten) => (this.notitieRechten = rechten));
  }

  protected initiatorGeselecteerd(initiator: GeneratedType<"RestPersoon">) {
    this.sideActions.close();

    if (this.zaak.initiatorIdentificatie) {
      // We already have an initiator, we need a reason to change it
      this.zaakDialogService
        .openWijzigInitiator(initiator.naam, (reden) =>
          this.zakenService.updateInitiator({
            zaakUUID: this.zaak.uuid,
            betrokkeneIdentificatie: new BetrokkeneIdentificatie(initiator),
            toelichting: reden,
          }),
        )
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

    this.zakenService.cacheZaak(zaak);
    const naam = [
      zaak.initiatorIdentificatie?.kvkNummer,
      zaak.initiatorIdentificatie?.vestigingsnummer,
    ].filter(Boolean);
    this.utilService.openSnackbar(notification, {
      naam: naam.join(" - "),
    });
    this.invalidateZaakHistorie();
  }

  protected deleteInitiator() {
    this.zaakDialogService
      .openOntkoppelInitiator((reden) =>
        runMutation(this.queryClient, this.zakenService.deleteInitiator(), {
          zaakUuid: this.zaak.uuid,
          reden,
        }),
      )
      .afterClosed()
      .subscribe((result) => {
        this.sideActions.clear();
        if (result) {
          this.utilService.openSnackbar("msg.initiator.ontkoppelen.uitgevoerd");
          this.zakenService.readZaak(this.zaak.uuid).subscribe((zaak) => {
            this.zakenService.cacheZaak(zaak);
            this.invalidateZaakHistorie();
          });
        }
      });
  }

  protected betrokkeneGeselecteerd(klantgegevens: KlantGegevens) {
    this.sideActions.close();
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
        this.zakenService.cacheZaak(zaak);
        this.utilService.openSnackbar("msg.betrokkene.gekoppeld", {
          roltype: klantgegevens.betrokkeneRoltype.naam,
        });
        this.invalidateZaakHistorie();
        this.invalidateBetrokkenen();
      });
  }

  private invalidateBetrokkenen() {
    this.queryClient.invalidateQueries({
      queryKey: this.zakenService.listBetrokkenenVoorZaakQuery(this.zaak.uuid)
        .queryKey,
    });
  }

  private loadBagObjecten() {
    this.bagService.list(this.zaak.uuid).subscribe((bagObjecten) => {
      this.gekoppeldeBagObjecten = bagObjecten
        .map(({ zaakobject }) => zaakobject!)
        .filter(Boolean);
      this.bagObjecten = bagObjecten;
    });
  }

  protected adresGeselecteerd(bagObject: GeneratedType<"RESTBAGObject">) {
    this.bagService
      .create({ zaakUuid: this.zaak.uuid, zaakobject: bagObject })
      .subscribe(() => {
        this.utilService.openSnackbar("msg.bagObject.gekoppeld");
        this.invalidateZaakHistorie();
        this.loadBagObjecten();
      });
  }

  protected taakGestart() {
    this.sideActions.reset();
    this.updateZaak();
  }

  protected mailVerstuurd(mailVerstuurd: boolean) {
    this.sideActions.reset();
    if (!mailVerstuurd) return;
    this.updateZaak();
  }

  protected ontvangstBevestigd(ontvangstBevestigd: boolean) {
    this.sideActions.reset();
    if (!ontvangstBevestigd) return;
    this.updateZaak();
  }

  protected documentToegevoegd() {
    this.updateZaak();
  }

  protected documentCreated() {
    this.sideActions.reset();
    this.updateZaak();
  }

  protected documentSent() {
    this.sideActions.reset();
    this.updateZaak();
  }

  protected zaakLinked() {
    this.sideActions.reset();
    this.updateZaak();
  }

  protected locationSelected() {
    this.sideActions.reset();
    this.updateZaak();
  }

  protected startZaakOntkoppelenDialog(
    gerelateerdeZaak: GeneratedType<"RestGerelateerdeZaak">,
  ) {
    this.dialogs.openZaakOntkoppelen(this.zaak, gerelateerdeZaak);
  }

  protected besluitVastgelegd() {
    this.sideActions.reset();
  }

  protected besluitWijzigen($event: GeneratedType<"RestBesluit">) {
    this.teWijzigenBesluit = $event;
    this.sideActions.open("actie.besluit.wijzigen");
  }

  protected documentMoveToCase(
    $event: Partial<GeneratedType<"RestEnkelvoudigInformatieobject">>,
  ) {
    this.documentToMove = $event;
    this.sideActions.open("actie.document.verplaatsen");
  }

  protected updateDocumentList() {
    this.zaakDocumentenComponent.updateDocumentList();
    this.invalidateZaakHistorie();
  }

  protected bagObjectVerwijderen(
    bagObjectGegevens: GeneratedType<"RESTBAGObjectGegevens">,
  ) {
    const bagObject = bagObjectGegevens.zaakobject;
    this.zaakDialogService
      .openVerwijderBagObject(bagObject?.omschrijving, (reden) =>
        runMutation(this.queryClient, this.bagService.delete(), {
          redenWijzigen: reden,
          bagObject,
          uuid: bagObjectGegevens.uuid,
          zaakUuid: this.zaak.uuid,
        }),
      )
      .afterClosed()
      .subscribe((result) => {
        this.sideActions.clear();
        if (!result) return;

        this.invalidateZaakHistorie();
        this.loadBagObjecten();
        this.utilService.openSnackbar("msg.bagObject.ontkoppelen.uitgevoerd", {
          omschrijving: bagObject?.omschrijving,
        });
      });
  }

  protected async menuItemChanged(event: string | null) {
    this.sideActions.activeAction.set(event);
  }

  protected showInitiator() {
    return showInitiator(this.zaak);
  }

  protected initiatorViewType() {
    return initiatorViewType(this.zaak);
  }

  protected allowedToAddBetrokkene() {
    return allowedToAddBetrokkene(this.zaak, this.hasBrpSearchRight());
  }

  protected allowBedrijf() {
    return allowBedrijf(this.zaak);
  }

  protected allowPersoon() {
    return allowPersoon(this.zaak, this.hasBrpSearchRight());
  }

  protected showBetrokkeneKoppelingen() {
    return showBetrokkeneKoppelingen(
      this.zaak,
      this.betrokkenenQuery.data()?.length ?? 0,
    );
  }

  private hasBrpSearchRight() {
    return Boolean(this.brpRechtenQuery.data()?.zoeken);
  }
}

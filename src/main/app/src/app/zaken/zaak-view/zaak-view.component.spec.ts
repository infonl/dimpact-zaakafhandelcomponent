/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { LOCALE_ID } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import {
  provideQueryClient,
  queryOptions,
} from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { within } from "@testing-library/angular";
import { EMPTY, of, ReplaySubject } from "rxjs";
import { UtilService } from "src/app/core/service/util.service";
import { StaticTextComponent } from "src/app/shared/static-text/static-text.component";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { ZaakafhandelParametersService } from "../../admin/zaakafhandel-parameters.service";
import { BAGService } from "../../bag/bag.service";
import { ObjectType } from "../../core/websocket/model/object-type";
import { Opcode } from "../../core/websocket/model/opcode";
import { ScreenEvent } from "../../core/websocket/model/screen-event";
import { ScreenEventId } from "../../core/websocket/model/screen-event-id";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { BedrijfsgegevensComponent } from "../../klanten/bedrijfsgegevens/bedrijfsgegevens.component";
import { ContactgegevensComponent } from "../../klanten/contactgegevens/contactgegevens.component";
import { KlantenService } from "../../klanten/klanten.service";
import { PersoonsgegevensComponent } from "../../klanten/persoonsgegevens/persoonsgegevens.component";
import { NotitiesComponent } from "../../notities/notities.component";
import { PlanItemsService } from "../../plan-items/plan-items.service";
import { PolicyService } from "../../policy/policy.service";
import { ZaakIndicatiesComponent } from "../../shared/indicaties/zaak-indicaties/zaak-indicaties.component";
import { MaterialModule } from "../../shared/material/material.module";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { PipesModule } from "../../shared/pipes/pipes.module";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "../../shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TakenService } from "../../taken/taken.service";
import { ZaakBetrokkeneListComponent } from "../zaak-betrokkenen-list/zaak-betrokkene-list.component";
import { ZaakDocumentenComponent } from "../zaak-documenten/zaak-documenten.component";
import { ZaakInitiatorToevoegenComponent } from "../zaak-initiator-toevoegen/zaak-initiator-toevoegen.component";
import { ZaakProcessFlowComponent } from "../zaak-process-flow/zaak-process-flow.component";
import { ZakenService } from "../zaken.service";
import { ZaakDetailsCardComponent } from "./zaak-details-card/zaak-details-card.component";
import { ZaakViewComponent } from "./zaak-view.component";

const planItemsQuery = (planItems: GeneratedType<"RESTPlanItem">[]) =>
  queryOptions({
    queryKey: ["fakePlanItems", planItems],
    queryFn: () => planItems,
    initialData: planItems,
  }) as ReturnType<PlanItemsService["listHumanTaskPlanItemsQuery"]>;

describe(ZaakViewComponent.name, () => {
  let fixture: ComponentFixture<ZaakViewComponent>;

  const screen = () => within(fixture.nativeElement as HTMLElement);

  let utilService: UtilService;
  let zakenService: ZakenService;
  let bagService: BAGService;
  let planItemsService: PlanItemsService;
  let dialogRef: MatDialogRef<unknown>;
  let takenService: TakenService;
  let websocketService: WebsocketService;
  let zaakafhandelParametersService: ZaakafhandelParametersService;

  const mockActivatedRoute = {
    data: new ReplaySubject<{ zaak: GeneratedType<"RestZaak"> }>(1),
  };

  beforeEach(() => {
    notifyManager.setScheduler((fn) => fn());
  });

  afterEach(() => {
    notifyManager.setScheduler(queueMicrotask);
  });

  const zaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "1234",
    zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
      omschrijving: "mock description",
    }),
    indicaties: [],
    rechten: {
      behandelen: true,
    },
    groep: {},
    vertrouwelijkheidaanduiding: "OPENBAAR",
    gerelateerdeZaken: [],
    initiatorIdentificatie: fromPartial<
      GeneratedType<"BetrokkeneIdentificatie">
    >({
      type: "BSN",
    }),
  });

  beforeEach(async () => {
    dialogRef = fromPartial<MatDialogRef<unknown>>({
      afterClosed: jest.fn().mockReturnValue(of(undefined)),
    });

    const dialogMock = {
      open: jest.fn().mockReturnValue(dialogRef),
    };

    await TestBed.configureTestingModule({
      declarations: [ZaakViewComponent],
      imports: [
        ZaakDocumentenComponent,
        ZaakBetrokkeneListComponent,
        ZaakDetailsCardComponent,
        ZaakInitiatorToevoegenComponent,
        BedrijfsgegevensComponent,
        ContactgegevensComponent,
        PersoonsgegevensComponent,
        NotitiesComponent,
        ZaakIndicatiesComponent,
        SideNavComponent,
        StaticTextComponent,
        ZaakProcessFlowComponent,
        TranslateModule.forRoot(),
        PipesModule,
        MaterialModule,
        VertrouwelijkaanduidingToTranslationKeyPipe,
        NoopAnimationsModule,
        EmptyPipe,
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        PlanItemsService,
        {
          provide: ActivatedRoute,
          useValue: mockActivatedRoute,
        },
        {
          provide: MatDialog,
          useValue: dialogMock,
        },
        VertrouwelijkaanduidingToTranslationKeyPipe,
        // matches the locale the app provides, so dates format as they do in production
        { provide: LOCALE_ID, useValue: "nl-NL" },
      ],
    }).compileComponents();

    utilService = TestBed.inject(UtilService);
    jest.spyOn(utilService, "setTitle").mockImplementation();

    zakenService = TestBed.inject(ZakenService);
    jest
      .spyOn(zakenService, "readOpschortingZaak")
      .mockReturnValue(
        of(fromPartial<GeneratedType<"RESTZaakOpschorting">>({})),
      );

    bagService = TestBed.inject(BAGService);
    jest.spyOn(bagService, "list").mockReturnValue(of([]));

    planItemsService = TestBed.inject(PlanItemsService);
    jest
      .spyOn(planItemsService, "listUserEventListenerPlanItemsQuery")
      .mockReturnValue(
        planItemsQuery([
          fromPartial<GeneratedType<"RESTPlanItem">>({
            userEventListenerActie: "INTAKE_AFRONDEN",
          }),
        ]),
      );
    jest
      .spyOn(planItemsService, "listHumanTaskPlanItemsQuery")
      .mockReturnValue(planItemsQuery([]));

    takenService = TestBed.inject(TakenService);
    jest.spyOn(takenService, "listTakenVoorZaak").mockReturnValue(of([]));

    jest.spyOn(KlantenService.prototype, "readPersoon").mockReturnValue(
      queryOptions({
        queryKey: ["fakePersoon"],
        queryFn: async () =>
          fromPartial<GeneratedType<"RestPersoon">>({
            naam: "fakePersoonNaam",
            indicaties: [],
          }),
      }) as ReturnType<KlantenService["readPersoon"]>,
    );
    jest.spyOn(KlantenService.prototype, "readBedrijf").mockReturnValue(
      queryOptions({
        queryKey: ["fakeBedrijf"],
        queryFn: async () =>
          fromPartial<GeneratedType<"RestBedrijf">>({
            naam: "fakeBedrijfNaam",
            identificatieType: "VN",
            vestigingsnummer: "fakeVestigingsnummer",
            kvkNummer: "fakeKvkNummer",
          }),
      }) as ReturnType<KlantenService["readBedrijf"]>,
    );

    TestBed.inject(KlantenService);

    websocketService = TestBed.inject(WebsocketService);
    jest
      .spyOn(websocketService, "addListener")
      .mockReturnValue(fromPartial<WebsocketListener>({}));
    jest.spyOn(websocketService, "doubleSuspendListener").mockImplementation();
    jest.spyOn(websocketService, "removeListener").mockImplementation();
    jest.spyOn(websocketService, "suspendListener").mockImplementation();

    zaakafhandelParametersService = TestBed.inject(
      ZaakafhandelParametersService,
    );
    jest
      .spyOn(
        zaakafhandelParametersService,
        "listZaakbeeindigRedenenForZaaktype",
      )
      .mockReturnValue(of([]));

    TestBed.inject(PolicyService);
    TestBed.inject(MatDialog);

    fixture = TestBed.createComponent(ZaakViewComponent);

    fixture.componentInstance.actionsSidenav = fromPartial<MatSidenav>({
      close: jest.fn(),
      open: jest.fn(),
    });
    fixture.componentInstance.sideNavContainer =
      fromPartial<MatSidenavContainer>({
        hasBackdrop: false,
        updateContentMargins: jest.fn(),
      });
  });

  describe("zaak historie invalidation", () => {
    it("invalidates the historie query when the zaak is (re)initialised", () => {
      const invalidateSpy = jest.spyOn(testQueryClient, "invalidateQueries");

      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();

      expect(invalidateSpy).toHaveBeenCalledWith(
        {
          queryKey: zakenService.listHistorieVoorZaakQuery(zaak.uuid).queryKey,
        },
        { cancelRefetch: false },
      );
    });

    it("invalidates the historie query again on a content-only change, without a route re-navigation", () => {
      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();

      const invalidateSpy = jest.spyOn(testQueryClient, "invalidateQueries");

      zakenService.cacheZaak({ ...zaak, isOpgeschort: true });
      fixture.detectChanges();

      expect(invalidateSpy).toHaveBeenCalledWith(
        {
          queryKey: zakenService.listHistorieVoorZaakQuery(zaak.uuid).queryKey,
        },
        { cancelRefetch: false },
      );
    });
  });

  describe("content margins", () => {
    it("updates them once the view is initialised, also when the menu is already final on the first effect pass", async () => {
      jest
        .spyOn(TestBed.inject(PolicyService), "readBrpRechten")
        .mockReturnValue(
          queryOptions({
            queryKey: ["fakeBrpRechten"],
            queryFn: () =>
              fromPartial<GeneratedType<"RestBrpRechten">>({ zoeken: true }),
            initialData: fromPartial<GeneratedType<"RestBrpRechten">>({
              zoeken: true,
            }),
          }) as ReturnType<PolicyService["readBrpRechten"]>,
        );
      const updateMargins = jest.spyOn(
        fixture.componentInstance as unknown as { updateMargins: () => void },
        "updateMargins",
      );

      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();
      fixture.detectChanges();

      expect(updateMargins).toHaveBeenCalled();
    });
  });

  describe("subscriptions$", () => {
    let subscriptionsPushSpy: jest.SpyInstance;

    beforeEach(() => {
      subscriptionsPushSpy = jest.spyOn(
        fixture.componentInstance["subscriptions$"],
        "push",
      );
      mockActivatedRoute.data.next({ zaak });

      fixture.detectChanges();
    });

    it("builds the menu without registering a subscription to unsubscribe later", () => {
      expect(subscriptionsPushSpy).not.toHaveBeenCalled();
    });
  });

  describe("notities", () => {
    let policyService: PolicyService;

    beforeEach(() => {
      policyService = TestBed.inject(PolicyService);
    });

    it("should render <zac-notities> when notitieRechten.lezen is true", () => {
      jest
        .spyOn(policyService, "readNotitieRechten")
        .mockReturnValue(of({ lezen: true, wijzigen: false }));
      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();

      expect(
        screen().getByRole("button", { name: "Notities" }),
      ).toBeInTheDocument();
    });

    it("should render <zac-notities> when notitieRechten.wijzigen is true", () => {
      jest
        .spyOn(policyService, "readNotitieRechten")
        .mockReturnValue(of({ lezen: false, wijzigen: true }));
      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();

      expect(
        screen().getByRole("button", { name: "Notities" }),
      ).toBeInTheDocument();
    });

    it("should not render <zac-notities> when both notitieRechten.lezen and wijzigen are false", () => {
      jest
        .spyOn(policyService, "readNotitieRechten")
        .mockReturnValue(of({ lezen: false, wijzigen: false }));
      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();

      expect(screen().queryByRole("button", { name: "Notities" })).toBeNull();
    });

    it("should not render <zac-notities> when notitieRechten is absent", () => {
      jest.spyOn(policyService, "readNotitieRechten").mockReturnValue(EMPTY);
      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();

      expect(screen().queryByRole("button", { name: "Notities" })).toBeNull();
    });
  });

  describe("zaak from cache", () => {
    beforeEach(() => {
      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();
    });

    it("renders the zaak the route resolved", () => {
      expect(fixture.componentInstance.zaak.uuid).toBe("1234");
    });

    it("re-renders from a cache write without a route emission", () => {
      zakenService.cacheZaak({
        ...zaak,
        omschrijving: "fakeUpdatedOmschrijving",
      });
      fixture.detectChanges();

      expect(fixture.componentInstance.zaak.omschrijving).toBe(
        "fakeUpdatedOmschrijving",
      );
    });
  });

  describe("side effects on zaak changes", () => {
    const opschortbareZaak = {
      ...zaak,
      isOpen: true,
      rechten: {
        ...zaak.rechten,
        behandelen: true,
      },
      zaaktype: {
        ...zaak.zaaktype,
        opschortingMogelijk: true,
      },
      isHeropend: false,
      isOpgeschort: false,
      eerdereOpschorting: false,
      isProcesGestuurd: false,
    } satisfies GeneratedType<"RestZaak">;

    beforeEach(() => {
      mockActivatedRoute.data.next({ zaak: opschortbareZaak });
      fixture.detectChanges();
      jest.mocked(bagService.list).mockClear();
    });

    it("does not reload BAG objects when only zaak content changes", () => {
      zakenService.cacheZaak({
        ...opschortbareZaak,
        omschrijving: "fakeUpdatedOmschrijving",
      });
      fixture.detectChanges();

      expect(bagService.list).not.toHaveBeenCalled();
    });

    it("rebuilds the action menu when rechten change", () => {
      const menuTitlesBeforeWrite = fixture.componentInstance["menu"]().map(
        (item) => item.title,
      );
      expect(menuTitlesBeforeWrite).toContain("actie.zaak.opschorten");

      zakenService.cacheZaak({
        ...opschortbareZaak,
        rechten: { ...opschortbareZaak.rechten, behandelen: false },
      });
      fixture.detectChanges();

      const menuTitlesAfterWrite = fixture.componentInstance["menu"]().map(
        (item) => item.title,
      );
      expect(menuTitlesAfterWrite).not.toContain("actie.zaak.opschorten");
    });

    it("does not reload the opschorting when only zaak content changes", () => {
      mockActivatedRoute.data.next({
        zaak: { ...opschortbareZaak, isOpgeschort: true },
      });
      fixture.detectChanges();
      jest.mocked(zakenService.readOpschortingZaak).mockClear();

      zakenService.cacheZaak({
        ...opschortbareZaak,
        isOpgeschort: true,
        omschrijving: "fakeUpdatedOmschrijving",
      });
      fixture.detectChanges();

      expect(zakenService.readOpschortingZaak).not.toHaveBeenCalled();
    });

    it("reloads the opschorting when navigating from one opgeschorte zaak to another", () => {
      mockActivatedRoute.data.next({
        zaak: { ...opschortbareZaak, isOpgeschort: true },
      });
      fixture.detectChanges();
      jest.mocked(zakenService.readOpschortingZaak).mockClear();

      mockActivatedRoute.data.next({
        zaak: {
          ...opschortbareZaak,
          uuid: "fakeOtherZaakUuid",
          isOpgeschort: true,
        },
      });
      fixture.detectChanges();

      expect(zakenService.readOpschortingZaak).toHaveBeenCalledWith(
        "fakeOtherZaakUuid",
      );
    });
  });

  describe("websocket echo suppression", () => {
    let httpTestingController: HttpTestingController;
    let zaakChangedCallback: (event: ScreenEvent) => Promise<void>;

    const zaakChangedEvent = new ScreenEvent(
      Opcode.UPDATED,
      ObjectType.ZAAK,
      new ScreenEventId(zaak.uuid),
    );

    const flushRefetch = async (body: GeneratedType<"RestZaak">) => {
      await new Promise(requestAnimationFrame);
      httpTestingController
        .expectOne((request) => request.url.endsWith("/rest/zaken/zaak/1234"))
        .flush(body);
      await new Promise(requestAnimationFrame);
    };

    // A 4xx status is used (rather than 5xx) because ZacQueryClient.GET retries
    // 5xx/network failures up to DEFAULT_RETRY_COUNT times, which would make this
    // helper race the retry backoff instead of resolving after a single failure.
    const flushRefetchError = async () => {
      await new Promise(requestAnimationFrame);
      httpTestingController
        .expectOne((request) => request.url.endsWith("/rest/zaken/zaak/1234"))
        .flush("fakeClientError", {
          status: 400,
          statusText: "Bad Request",
        });
      await new Promise(requestAnimationFrame);
    };

    beforeEach(() => {
      httpTestingController = TestBed.inject(HttpTestingController);
      jest.spyOn(utilService, "openSnackbar");
      jest
        .spyOn(websocketService, "addListener")
        .mockImplementation((_opcode, objectType, _objectId, callback) => {
          if (objectType === ObjectType.ZAAK) {
            zaakChangedCallback = callback as unknown as (
              event: ScreenEvent,
            ) => Promise<void>;
          }
          return fromPartial<WebsocketListener>({});
        });
      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();
      // Mounting the component fires several unrelated queries (identity,
      // policy, betrokkenen, ...). Drain them so this describe's afterEach
      // only has to account for the zaak refetch it triggers itself.
      httpTestingController.match(() => true);
    });

    afterEach(() => {
      httpTestingController.verify();
    });

    it("stays quiet when the refetch returns identical data", async () => {
      const pending = zaakChangedCallback(zaakChangedEvent);
      await flushRefetch({ ...zaak });
      await pending;

      expect(utilService.openSnackbar).not.toHaveBeenCalled();
    });

    it("notifies when the refetch returns different data", async () => {
      const pending = zaakChangedCallback(zaakChangedEvent);
      await flushRefetch({ ...zaak, omschrijving: "changedByOtherUser" });
      await pending;

      expect(utilService.openSnackbar).toHaveBeenCalled();
    });

    it("renders the changed zaak in the details card", async () => {
      const pending = zaakChangedCallback(zaakChangedEvent);
      await flushRefetch({
        ...zaak,
        status: fromPartial<GeneratedType<"RestZaakStatus">>({
          naam: "fakeChangedStatusNaam",
        }),
      });
      await pending;
      fixture.detectChanges();

      expect(screen().getByText("fakeChangedStatusNaam")).toBeInTheDocument();
    });

    it("reloads the bag objecten, which are fetched separately from the zaak", async () => {
      jest.mocked(bagService.list).mockClear();

      const pending = zaakChangedCallback(zaakChangedEvent);
      await flushRefetch({ ...zaak, omschrijving: "changedByOtherUser" });
      await pending;

      expect(bagService.list).toHaveBeenCalledWith(zaak.uuid);
    });

    it("reloads the bag objecten of an unchanged zaak, since they are not part of it", async () => {
      jest.mocked(bagService.list).mockClear();

      const pending = zaakChangedCallback(zaakChangedEvent);
      await flushRefetch({ ...zaak });
      await pending;

      expect(bagService.list).toHaveBeenCalledWith(zaak.uuid);
    });

    it("reloads the opschorting of a zaak that was already opgeschort before the change", async () => {
      mockActivatedRoute.data.next({ zaak: { ...zaak, isOpgeschort: true } });
      fixture.detectChanges();
      httpTestingController.match(() => true);
      jest.mocked(zakenService.readOpschortingZaak).mockClear();

      const pending = zaakChangedCallback(zaakChangedEvent);
      await flushRefetch({
        ...zaak,
        isOpgeschort: true,
        omschrijving: "changedByOtherUser",
      });
      await pending;

      expect(zakenService.readOpschortingZaak).toHaveBeenCalledWith(zaak.uuid);
    });

    it("notifies when the refetch fails, rather than silently treating it as an echo", async () => {
      const pending = zaakChangedCallback(zaakChangedEvent);
      await flushRefetchError();
      await pending;

      expect(utilService.openSnackbar).toHaveBeenCalled();
    });
  });

  describe("zaak rollen websocket listener", () => {
    let zaakRollenCallback: (event: ScreenEvent) => void;

    const rollenEvent = (opcode: Opcode) =>
      new ScreenEvent(
        opcode,
        ObjectType.ZAAK_ROLLEN,
        new ScreenEventId(zaak.uuid),
      );

    beforeEach(() => {
      jest.spyOn(utilService, "openSnackbar");
      jest
        .spyOn(websocketService, "addListenerWithSnackbar")
        .mockImplementation((_opcode, objectType, _objectId, callback) => {
          if (objectType === ObjectType.ZAAK_ROLLEN) {
            zaakRollenCallback = callback as (event: ScreenEvent) => void;
          }
          return fromPartial<WebsocketListener>({});
        });

      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();
    });

    it("invalidates the betrokkenen query when a betrokkene changes elsewhere", () => {
      const invalidateSpy = jest.spyOn(testQueryClient, "invalidateQueries");

      zaakRollenCallback(rollenEvent(Opcode.UPDATED));

      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: zakenService.listBetrokkenenVoorZaakQuery(zaak.uuid).queryKey,
      });
    });

    it("refetches the zaak, which carries the groep, behandelaar and rechten", () => {
      const invalidateSpy = jest.spyOn(testQueryClient, "invalidateQueries");

      zaakRollenCallback(rollenEvent(Opcode.UPDATED));

      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: zakenService.readZaakQuery(zaak.uuid).queryKey,
      });
    });

    it("invalidates the historie query, which lists the rol changes", () => {
      const invalidateSpy = jest.spyOn(testQueryClient, "invalidateQueries");

      zaakRollenCallback(rollenEvent(Opcode.UPDATED));

      expect(invalidateSpy).toHaveBeenCalledWith(
        {
          queryKey: zakenService.listHistorieVoorZaakQuery(zaak.uuid).queryKey,
        },
        { cancelRefetch: false },
      );
    });
  });

  describe("zaak besluiten websocket listener", () => {
    let zaakBesluitenCallback: (event: ScreenEvent) => void;

    beforeEach(() => {
      // The documenten list subscribes to the same object type through the
      // plain `addListener`, so only the snackbar variant is the zaak view's.
      jest
        .spyOn(websocketService, "addListenerWithSnackbar")
        .mockImplementation((_opcode, objectType, _objectId, callback) => {
          if (objectType === ObjectType.ZAAK_BESLUITEN) {
            zaakBesluitenCallback = callback as (event: ScreenEvent) => void;
          }
          return fromPartial<WebsocketListener>({});
        });

      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();
    });

    it("puts the reloaded besluiten on the cached zaak", () => {
      const besluit = fromPartial<GeneratedType<"RestBesluit">>({
        toelichting: "fakeBesluitToelichting",
      });
      jest
        .spyOn(zakenService, "listBesluitenForZaak")
        .mockReturnValue(of([besluit]));

      zaakBesluitenCallback(
        new ScreenEvent(
          Opcode.UPDATED,
          ObjectType.ZAAK_BESLUITEN,
          new ScreenEventId(zaak.uuid),
        ),
      );

      expect(
        testQueryClient.getQueryData(
          zakenService.readZaakQuery(zaak.uuid).queryKey,
        ),
      ).toEqual(expect.objectContaining({ besluiten: [besluit] }));
    });
  });

  describe("zaak taken websocket listener", () => {
    let zaakTakenCallback: (event: ScreenEvent) => void;

    beforeEach(() => {
      jest
        .spyOn(websocketService, "addListener")
        .mockImplementation((_opcode, objectType, _objectId, callback) => {
          if (objectType === ObjectType.ZAAK_TAKEN) {
            zaakTakenCallback = callback as (event: ScreenEvent) => void;
          }
          return fromPartial<WebsocketListener>({});
        });

      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();
    });

    it("refreshes the plan items the action menu is built from, which do not travel with the zaak", () => {
      const invalidateQueries = jest.spyOn(
        testQueryClient,
        "invalidateQueries",
      );

      zaakTakenCallback(
        new ScreenEvent(
          Opcode.UPDATED,
          ObjectType.ZAAK_TAKEN,
          new ScreenEventId(zaak.uuid),
        ),
      );

      expect(invalidateQueries).toHaveBeenCalledWith(
        {
          queryKey: planItemsService.listHumanTaskPlanItemsQuery(zaak.uuid)
            .queryKey,
        },
        { cancelRefetch: false },
      );
      expect(invalidateQueries).toHaveBeenCalledWith(
        {
          queryKey: planItemsService.listUserEventListenerPlanItemsQuery(
            zaak.uuid,
          ).queryKey,
        },
        { cancelRefetch: false },
      );
    });
  });
});

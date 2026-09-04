/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
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
import { Observable, of, ReplaySubject } from "rxjs";
import { UtilService } from "src/app/core/service/util.service";
import { StaticTextComponent } from "src/app/shared/static-text/static-text.component";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { ZaakafhandelParametersService } from "../../admin/zaakafhandel-parameters.service";
import { BAGService } from "../../bag/bag.service";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { BedrijfsgegevensComponent } from "../../klanten/bedrijfsgegevens/bedrijfsgegevens.component";
import { ContactgegevensComponent } from "../../klanten/contactgegevens/contactgegevens.component";
import { KlantenService } from "../../klanten/klanten.service";
import { KlantGegevens } from "../../klanten/model/klanten/klant-gegevens";
import { PersoonsgegevensComponent } from "../../klanten/persoonsgegevens/persoonsgegevens.component";
import { NotitiesComponent } from "../../notities/notities.component";
import { PlanItemsService } from "../../plan-items/plan-items.service";
import { PolicyService } from "../../policy/policy.service";
import { RedenDialogFormComponent } from "../../shared/dialog/reden-dialog-form/reden-dialog-form.component";
import { ZaakIndicatiesComponent } from "../../shared/indicaties/zaak-indicaties/zaak-indicaties.component";
import { MaterialModule } from "../../shared/material/material.module";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { PipesModule } from "../../shared/pipes/pipes.module";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "../../shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TakenService } from "../../taken/taken.service";
import { ZaakBetrokkeneListComponent } from "../zaak-betrokkenen-list/zaak-betrokkene-list.component";
import { ZaakDialogService } from "../zaak-dialog.service";
import { ZaakDocumentenComponent } from "../zaak-documenten/zaak-documenten.component";
import { ZaakInitiatorToevoegenComponent } from "../zaak-initiator-toevoegen/zaak-initiator-toevoegen.component";
import { ZaakProcessFlowComponent } from "../zaak-process-flow/zaak-process-flow.component";
import { ZakenService } from "../zaken.service";
import { ZaakSideActionService } from "./services/zaak-side-action.service";
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
  let sideActions: ZaakSideActionService;

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
    sideActions = fixture.debugElement.injector.get(ZaakSideActionService);

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

  describe("initiator view", () => {
    const koppelingen = fromPartial<GeneratedType<"RestBetrokkeneKoppelingen">>(
      {
        brpKoppelen: true,
        kvkKoppelen: true,
      },
    );

    it("should show zac-zaak-initiator-toevoegen when no type matches and no contact details", () => {
      mockActivatedRoute.data.next({
        zaak: {
          ...zaak,
          initiatorIdentificatie: null,
          zaakSpecificContactDetails: null,
          zaaktype: {
            ...zaak.zaaktype,
            zaakafhandelparameters: fromPartial<
              GeneratedType<"RestZaaktypeConfiguration">
            >({
              betrokkeneKoppelingen: koppelingen,
            }),
          },
        },
      });
      fixture.detectChanges();

      expect(
        screen().getByRole("button", { name: /msg.zaak.geen.initiator/ }),
      ).toBeInTheDocument();
    });

    it("should show zac-persoongegevens when initiator type is BSN", async () => {
      mockActivatedRoute.data.next({
        zaak: {
          ...zaak,
          initiatorIdentificatie: fromPartial({
            type: "BSN",
            temporaryPersonId: "test-id",
          }),
          zaakSpecificContactDetails: null,
          zaaktype: {
            ...zaak.zaaktype,
            zaakafhandelparameters: fromPartial<
              GeneratedType<"RestZaaktypeConfiguration">
            >({
              betrokkeneKoppelingen: koppelingen,
            }),
          },
        },
      });
      fixture.detectChanges();

      await fixture.whenStable();
      fixture.detectChanges();

      expect(
        screen().getByRole("button", { name: /fakePersoonNaam/ }),
      ).toBeInTheDocument();
    });

    it("should show zac-bedrijfsgegevens when initiator type is VN", async () => {
      mockActivatedRoute.data.next({
        zaak: {
          ...zaak,
          initiatorIdentificatie: fromPartial({
            type: "VN",
            vestigingsnummer: "12345678",
            kvkNummer: "87654321",
          }),
          zaakSpecificContactDetails: null,
          zaaktype: {
            ...zaak.zaaktype,
            zaakafhandelparameters: fromPartial<
              GeneratedType<"RestZaaktypeConfiguration">
            >({
              betrokkeneKoppelingen: koppelingen,
            }),
          },
        },
      });
      fixture.detectChanges();

      await fixture.whenStable();
      fixture.detectChanges();

      expect(
        screen().getByRole("button", { name: /fakeBedrijfNaam/ }),
      ).toBeInTheDocument();
    });

    it("should show zac-contactgegevens when zaakSpecificContactDetails is present", () => {
      mockActivatedRoute.data.next({
        zaak: {
          ...zaak,
          initiatorIdentificatie: null,
          zaakSpecificContactDetails: fromPartial<
            GeneratedType<"ContactDetails">
          >({
            telephoneNumber: "0612345678",
            emailAddress: "test@example.com",
          }),
        },
      });
      fixture.detectChanges();

      expect(
        screen().getByRole("button", {
          name: /initiator.aanvraagspecifieke-contactgegevens/,
        }),
      ).toBeInTheDocument();
    });

    it("should not show zac-contactgegevens when zaakSpecificContactDetails has only empty fields", () => {
      mockActivatedRoute.data.next({
        zaak: {
          ...zaak,
          initiatorIdentificatie: null,
          zaakSpecificContactDetails: fromPartial<
            GeneratedType<"ContactDetails">
          >({
            telephoneNumber: null,
            emailAddress: null,
          }),
          zaaktype: {
            ...zaak.zaaktype,
            zaakafhandelparameters: fromPartial<
              GeneratedType<"RestZaaktypeConfiguration">
            >({
              betrokkeneKoppelingen: koppelingen,
            }),
          },
        },
      });
      fixture.detectChanges();

      expect(
        screen().queryByRole("button", {
          name: /initiator.aanvraagspecifieke-contactgegevens/,
        }),
      ).toBeNull();
      expect(
        screen().getByRole("button", { name: /msg.zaak.geen.initiator/ }),
      ).toBeInTheDocument();
    });

    it("should hide the initiator section when no koppelingen are configured and zaakSpecificContactDetails has only empty fields", () => {
      mockActivatedRoute.data.next({
        zaak: {
          ...zaak,
          initiatorIdentificatie: null,
          zaakSpecificContactDetails: fromPartial<
            GeneratedType<"ContactDetails">
          >({
            telephoneNumber: null,
            emailAddress: null,
          }),
          zaaktype: {
            ...zaak.zaaktype,
            zaakafhandelparameters: fromPartial<
              GeneratedType<"RestZaaktypeConfiguration">
            >({
              betrokkeneKoppelingen: fromPartial<
                GeneratedType<"RestBetrokkeneKoppelingen">
              >({ brpKoppelen: false, kvkKoppelen: false }),
            }),
          },
        },
      });
      fixture.detectChanges();

      expect(
        screen().queryByRole("button", {
          name: /initiator.aanvraagspecifieke-contactgegevens/,
        }),
      ).toBeNull();
      expect(
        screen().queryByRole("button", { name: /msg.zaak.geen.initiator/ }),
      ).toBeNull();
    });
  });

  describe("allowPersoon", () => {
    let policyService: PolicyService;

    const zaakWithPersoonRechten = {
      ...zaak,
      rechten: {
        ...zaak.rechten,
        toevoegenInitiatorPersoon: true,
      },
      zaaktype: {
        ...zaak.zaaktype,
        zaakafhandelparameters: fromPartial<
          GeneratedType<"RestZaaktypeConfiguration">
        >({
          betrokkeneKoppelingen: fromPartial<
            GeneratedType<"RestBetrokkeneKoppelingen">
          >({ brpKoppelen: true }),
        }),
      },
    } satisfies GeneratedType<"RestZaak">;

    beforeEach(() => {
      policyService = TestBed.inject(PolicyService);

      testQueryClient.setQueryData(
        policyService.readBrpRechten().queryKey,
        fromPartial<GeneratedType<"RestBrpRechten">>({
          zoeken: true,
        }),
      );

      mockActivatedRoute.data.next({ zaak: zaakWithPersoonRechten });
      fixture.detectChanges();
    });

    it("should return true when toevoegenInitiatorPersoon, brpKoppelen and brpZoeken are all true", () => {
      expect(fixture.componentInstance["allowPersoon"]()).toBe(true);
    });

    it("should return false when toevoegenInitiatorPersoon is false", () => {
      mockActivatedRoute.data.next({
        zaak: {
          ...zaakWithPersoonRechten,
          rechten: {
            ...zaakWithPersoonRechten.rechten,
            toevoegenInitiatorPersoon: false,
          },
        },
      });
      fixture.detectChanges();

      expect(fixture.componentInstance["allowPersoon"]()).toBe(false);
    });

    it("should return false when brpKoppelen is false", () => {
      mockActivatedRoute.data.next({
        zaak: {
          ...zaakWithPersoonRechten,
          zaaktype: {
            ...zaakWithPersoonRechten.zaaktype,
            zaakafhandelparameters: fromPartial<
              GeneratedType<"RestZaaktypeConfiguration">
            >({
              betrokkeneKoppelingen: fromPartial<
                GeneratedType<"RestBetrokkeneKoppelingen">
              >({ brpKoppelen: false }),
            }),
          },
        },
      });
      fixture.detectChanges();

      expect(fixture.componentInstance["allowPersoon"]()).toBe(false);
    });

    it("should return false when brpZoeken is false", () => {
      testQueryClient.setQueryData(
        policyService.readBrpRechten().queryKey,
        fromPartial<GeneratedType<"RestBrpRechten">>({
          zoeken: false,
        }),
      );
      fixture.detectChanges();

      expect(fixture.componentInstance["allowPersoon"]()).toBe(false);
    });
  });

  describe("allowedToAddBetrokkene", () => {
    let policyService: PolicyService;

    const zaakWithBetrokkeneRechten = {
      ...zaak,
      rechten: {
        ...zaak.rechten,
        toevoegenInitiatorPersoon: true,
        toevoegenInitiatorBedrijf: true,
      },
      zaaktype: {
        ...zaak.zaaktype,
        zaakafhandelparameters: fromPartial<
          GeneratedType<"RestZaaktypeConfiguration">
        >({
          betrokkeneKoppelingen: fromPartial<
            GeneratedType<"RestBetrokkeneKoppelingen">
          >({ brpKoppelen: true, kvkKoppelen: false }),
        }),
      },
    } satisfies GeneratedType<"RestZaak">;

    beforeEach(() => {
      policyService = TestBed.inject(PolicyService);
      mockActivatedRoute.data.next({ zaak: zaakWithBetrokkeneRechten });
      fixture.detectChanges();
      testQueryClient.setQueryData(
        policyService.readBrpRechten().queryKey,
        fromPartial<GeneratedType<"RestBrpRechten">>({
          zoeken: true,
        }),
      );
      fixture.detectChanges();
    });

    it("should return true when brpKoppelen, toevoegenInitiatorPersoon and brpZoeken are true", () => {
      expect(fixture.componentInstance["allowedToAddBetrokkene"]()).toBe(true);
    });

    it("should return true when kvkKoppelen and toevoegenInitiatorBedrijf are true regardless of brpZoeken", () => {
      testQueryClient.setQueryData(
        policyService.readBrpRechten().queryKey,
        fromPartial<GeneratedType<"RestBrpRechten">>({
          zoeken: false,
        }),
      );
      mockActivatedRoute.data.next({
        zaak: {
          ...zaakWithBetrokkeneRechten,
          zaaktype: {
            ...zaakWithBetrokkeneRechten.zaaktype,
            zaakafhandelparameters: fromPartial<
              GeneratedType<"RestZaaktypeConfiguration">
            >({
              betrokkeneKoppelingen: fromPartial<
                GeneratedType<"RestBetrokkeneKoppelingen">
              >({ brpKoppelen: false, kvkKoppelen: true }),
            }),
          },
        },
      });
      fixture.detectChanges();

      expect(fixture.componentInstance["allowedToAddBetrokkene"]()).toBe(true);
    });

    it("should return false when brpZoeken is false and kvkKoppelen is false", () => {
      testQueryClient.setQueryData(
        policyService.readBrpRechten().queryKey,
        fromPartial<GeneratedType<"RestBrpRechten">>({
          zoeken: false,
        }),
      );
      fixture.detectChanges();

      expect(fixture.componentInstance["allowedToAddBetrokkene"]()).toBe(false);
    });

    it("should return false when toevoegenInitiatorPersoon is false and kvkAllowed is false", () => {
      mockActivatedRoute.data.next({
        zaak: {
          ...zaakWithBetrokkeneRechten,
          rechten: {
            ...zaakWithBetrokkeneRechten.rechten,
            toevoegenInitiatorPersoon: false,
          },
        },
      });
      fixture.detectChanges();

      expect(fixture.componentInstance["allowedToAddBetrokkene"]()).toBe(false);
    });
  });

  describe("ontkoppelen met een reden", () => {
    let httpTestingController: HttpTestingController;
    let foutAfhandelen: jest.SpyInstance;

    const confirmWithReden = () => {
      const dialog = TestBed.inject(MatDialog);
      const { data } = jest.mocked(dialog.open).mock.calls.at(-1)![1]!;
      (data as { callback: (reden: string) => Observable<unknown> })
        .callback("fakeReden")
        .subscribe({ error: () => undefined });
    };

    const flushServerError = async (url: string) => {
      await new Promise(requestAnimationFrame);
      httpTestingController
        .expectOne((request) => request.url.endsWith(url))
        .flush(null, { status: 500, statusText: "Server Error" });
      await new Promise(requestAnimationFrame);
    };

    beforeEach(() => {
      httpTestingController = TestBed.inject(HttpTestingController);
      foutAfhandelen = jest
        .spyOn(TestBed.inject(FoutAfhandelingService), "foutAfhandelen")
        .mockReturnValue(of());
      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();
      httpTestingController.match(() => true);
    });

    it("reports a failing initiator ontkoppelen through the error handler", async () => {
      fixture.componentInstance["deleteInitiator"]();
      confirmWithReden();

      await flushServerError(`/rest/zaken/${zaak.uuid}/initiator`);

      expect(foutAfhandelen).toHaveBeenCalled();
    });

    it("reports a failing BAG-object ontkoppelen through the error handler", async () => {
      fixture.componentInstance["bagObjectVerwijderen"](
        fromPartial<GeneratedType<"RESTBAGObjectGegevens">>({
          uuid: "fake-bag-object-uuid",
          zaakobject: { omschrijving: "fake bag object" },
        }),
      );
      confirmWithReden();

      await flushServerError("/rest/bag");

      expect(foutAfhandelen).toHaveBeenCalled();
    });
  });

  describe("koppelen van een initiator", () => {
    const initiator = fromPartial<GeneratedType<"RestPersoon">>({
      naam: "fakeInitiatorNaam",
      identificatieType: "BSN",
      bsn: "fakeBsn",
      temporaryPersonId: "fakeTemporaryPersonId",
    });

    const zaakZonderInitiator = fromPartial<GeneratedType<"RestZaak">>({
      ...zaak,
      initiatorIdentificatie: undefined,
    });

    it("asks for a reason before replacing an initiator the zaak already has", () => {
      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();
      const zaakDialogService = TestBed.inject(ZaakDialogService);
      const openWijzigInitiator = jest
        .spyOn(zaakDialogService, "openWijzigInitiator")
        .mockReturnValue(
          fromPartial<MatDialogRef<RedenDialogFormComponent>>({
            afterClosed: () => of(undefined),
          }),
        );
      const updateInitiator = jest.spyOn(zakenService, "updateInitiator");

      fixture.componentInstance["initiatorGeselecteerd"](initiator);

      expect(openWijzigInitiator).toHaveBeenCalledWith(
        "fakeInitiatorNaam",
        expect.any(Function),
      );
      expect(updateInitiator).not.toHaveBeenCalled();
    });

    it("couples the initiator straight away when the zaak has none yet", () => {
      mockActivatedRoute.data.next({ zaak: zaakZonderInitiator });
      fixture.detectChanges();
      const updatedZaak = fromPartial<GeneratedType<"RestZaak">>({
        ...zaakZonderInitiator,
        initiatorIdentificatie: fromPartial<
          GeneratedType<"BetrokkeneIdentificatie">
        >({ kvkNummer: "fakeKvkNummer" }),
      });
      const updateInitiator = jest
        .spyOn(zakenService, "updateInitiator")
        .mockReturnValue(of(updatedZaak));
      const openSnackbar = jest.spyOn(utilService, "openSnackbar");

      fixture.componentInstance["initiatorGeselecteerd"](initiator);

      expect(updateInitiator).toHaveBeenCalledWith(
        expect.objectContaining({ zaakUUID: zaakZonderInitiator.uuid }),
      );
      expect(openSnackbar).toHaveBeenCalledWith("msg.initiator.gekoppeld", {
        naam: "fakeKvkNummer",
      });
    });

    it("reports the ontkoppelen and refetches the zaak when the dialog confirms", () => {
      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();
      const zaakDialogService = TestBed.inject(ZaakDialogService);
      jest.spyOn(zaakDialogService, "openOntkoppelInitiator").mockReturnValue(
        fromPartial<MatDialogRef<RedenDialogFormComponent>>({
          afterClosed: () => of(true),
        }),
      );
      const readZaak = jest
        .spyOn(zakenService, "readZaak")
        .mockReturnValue(of(zaak));
      const openSnackbar = jest.spyOn(utilService, "openSnackbar");

      fixture.componentInstance["deleteInitiator"]();

      expect(openSnackbar).toHaveBeenCalledWith(
        "msg.initiator.ontkoppelen.uitgevoerd",
      );
      expect(readZaak).toHaveBeenCalledWith(zaak.uuid);
      expect(sideActions.activeAction()).toBeNull();
    });
  });

  describe("koppelen van een betrokkene en een BAG-object", () => {
    beforeEach(() => {
      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();
    });

    it("closes the sidenav and reports the roltype when a betrokkene is coupled", () => {
      const createBetrokkene = jest
        .spyOn(zakenService, "createBetrokkene")
        .mockReturnValue(of(zaak));
      const openSnackbar = jest.spyOn(utilService, "openSnackbar");
      const closeSideNav = jest
        .spyOn(fixture.componentInstance.actionsSidenav, "close")
        .mockResolvedValue("close");

      const klantGegevens = new KlantGegevens(
        fromPartial<GeneratedType<"RestPersoon">>({
          naam: "fakeKlantNaam",
          identificatieType: "BSN",
          bsn: "fakeKlantBsn",
          temporaryPersonId: "fakeKlantTemporaryPersonId",
        }),
      );
      klantGegevens.betrokkeneRoltype = fromPartial<
        GeneratedType<"RestRoltype">
      >({ uuid: "fakeRoltypeUuid", naam: "fakeRoltypeNaam" });
      klantGegevens.betrokkeneToelichting = "fakeToelichting";

      fixture.componentInstance["betrokkeneGeselecteerd"](klantGegevens);

      expect(closeSideNav).toHaveBeenCalled();
      expect(createBetrokkene).toHaveBeenCalledWith(
        expect.objectContaining({
          zaakUUID: zaak.uuid,
          roltypeUUID: "fakeRoltypeUuid",
          roltoelichting: "fakeToelichting",
        }),
      );
      expect(openSnackbar).toHaveBeenCalledWith("msg.betrokkene.gekoppeld", {
        roltype: "fakeRoltypeNaam",
      });
    });

    it("reloads the BAG-objecten after coupling an adres", () => {
      const bagObject = fromPartial<GeneratedType<"RESTBAGObject">>({
        omschrijving: "fakeBagObjectOmschrijving",
      });
      const create = jest
        .spyOn(bagService, "create")
        .mockReturnValue(of(undefined) as never);
      const openSnackbar = jest.spyOn(utilService, "openSnackbar");
      // the view already listed them while initialising, so ignore that call
      const list = jest
        .spyOn(bagService, "list")
        .mockClear()
        .mockReturnValue(of([]));

      fixture.componentInstance["adresGeselecteerd"](bagObject);

      expect(create).toHaveBeenCalledWith({
        zaakUuid: zaak.uuid,
        zaakobject: bagObject,
      });
      expect(list).toHaveBeenCalledWith(zaak.uuid);
      expect(openSnackbar).toHaveBeenCalledWith("msg.bagObject.gekoppeld");
    });
  });
});

/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { MatIconHarness } from "@angular/material/icon/testing";
import {
  MatNavListItemHarness,
  MatSubheaderHarness,
} from "@angular/material/list/testing";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import moment from "moment";
import { EMPTY, of, ReplaySubject } from "rxjs";
import { UtilService } from "src/app/core/service/util.service";
import { StaticTextComponent } from "src/app/shared/static-text/static-text.component";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { ZaakafhandelParametersService } from "../../admin/zaakafhandel-parameters.service";
import { BAGService } from "../../bag/bag.service";
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
import { MenuItemType } from "../../shared/side-nav/menu-item/menu-item";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TakenService } from "../../taken/taken.service";
import { ZaakBetrokkeneListComponent } from "../zaak-betrokkenen-list/zaak-betrokkene-list.component";
import { ZaakDocumentenComponent } from "../zaak-documenten/zaak-documenten.component";
import { ZaakInitiatorToevoegenComponent } from "../zaak-initiator-toevoegen/zaak-initiator-toevoegen.component";
import { ZaakProcessFlowComponent } from "../zaak-process-flow/zaak-process-flow.component";
import { ZakenService } from "../zaken.service";
import { ZaakViewComponent } from "./zaak-view.component";

describe(ZaakViewComponent.name, () => {
  let fixture: ComponentFixture<ZaakViewComponent>;
  let loader: HarnessLoader;

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
    dialogRef = {
      afterClosed: jest.fn().mockReturnValue(of(undefined)),
    } as unknown as MatDialogRef<unknown>;

    const dialogMock = {
      open: jest.fn().mockReturnValue(dialogRef),
    };

    await TestBed.configureTestingModule({
      declarations: [ZaakViewComponent],
      imports: [
        ZaakDocumentenComponent,
        ZaakBetrokkeneListComponent,
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
      .spyOn(planItemsService, "listUserEventListenerPlanItems")
      .mockReturnValue(
        of([
          fromPartial<GeneratedType<"RESTPlanItem">>({
            userEventListenerActie: "INTAKE_AFRONDEN",
          }),
        ]),
      );
    jest
      .spyOn(planItemsService, "listHumanTaskPlanItems")
      .mockReturnValue(of([]));
    jest
      .spyOn(planItemsService, "listProcessTaskPlanItems")
      .mockReturnValue(of([]));

    takenService = TestBed.inject(TakenService);
    jest.spyOn(takenService, "listTakenVoorZaak").mockReturnValue(of([]));

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
    loader = TestbedHarnessEnvironment.loader(fixture);

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

  describe("actie.zaak.opschorten", () => {
    const opschortenZaak = {
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
      mockActivatedRoute.data.next({ zaak: opschortenZaak });
    });

    it("should show the button", async () => {
      const button = await loader.getHarness(
        MatNavListItemHarness.with({ title: "actie.zaak.opschorten" }),
      );
      expect(button).toBeTruthy();
    });

    describe("eerdereOpschorting", () => {
      beforeEach(() => {
        mockActivatedRoute.data.next({
          zaak: {
            ...opschortenZaak,
            eerdereOpschorting: true,
          },
        });
      });

      it("should not show the button", async () => {
        const button = await loader.getHarnessOrNull(
          MatNavListItemHarness.with({ title: "actie.zaak.opschorten" }),
        );
        expect(button).toBeNull();
      });
    });
  });

  describe("zaak historie invalidation", () => {
    it("invalidates the historie query when the zaak is (re)initialised", () => {
      const invalidateSpy = jest.spyOn(testQueryClient, "invalidateQueries");

      mockActivatedRoute.data.next({ zaak });

      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: zakenService.listHistorieVoorZaakQuery(zaak.uuid).queryKey,
      });
    });
  });

  describe("actie.zaak.hervatten", () => {
    const hervattenZaak = {
      ...zaak,
      isOpgeschort: true,
      rechten: {
        ...zaak.rechten,
        behandelen: true,
      },
      isProcesGestuurd: false,
    } satisfies GeneratedType<"RestZaak">;

    beforeEach(() => {
      mockActivatedRoute.data.next({ zaak: hervattenZaak });
    });

    it("should show the button", async () => {
      const button = await loader.getHarness(
        MatNavListItemHarness.with({ title: "actie.zaak.hervatten" }),
      );
      expect(button).toBeTruthy();
    });

    describe("when behandelen right is false", () => {
      beforeEach(() => {
        mockActivatedRoute.data.next({
          zaak: {
            ...hervattenZaak,
            rechten: {
              ...hervattenZaak.rechten,
              behandelen: false,
            },
          },
        });
      });

      it("should not show the button", async () => {
        const button = await loader.getHarnessOrNull(
          MatNavListItemHarness.with({ title: "actie.zaak.hervatten" }),
        );
        expect(button).toBeNull();
      });
    });

    describe("when isOpgeschort is false", () => {
      beforeEach(() => {
        mockActivatedRoute.data.next({
          zaak: {
            ...hervattenZaak,
            isOpgeschort: false,
          },
        });
      });

      it("should not show the button", async () => {
        const button = await loader.getHarnessOrNull(
          MatNavListItemHarness.with({ title: "actie.zaak.hervatten" }),
        );
        expect(button).toBeNull();
      });
    });

    describe("when isProcesGestuurd is true", () => {
      beforeEach(() => {
        mockActivatedRoute.data.next({
          zaak: {
            ...hervattenZaak,
            isProcesGestuurd: true,
          },
        });
      });

      it("should not show the button", async () => {
        const button = await loader.getHarnessOrNull(
          MatNavListItemHarness.with({ title: "actie.zaak.hervatten" }),
        );
        expect(button).toBeNull();
      });
    });
  });

  describe("dateFieldIconMap icon logic", () => {
    let component: ZaakViewComponent;
    const yesterdayDate = moment().subtract(1, "days").format("YYYY-MM-DD");
    const today = moment().format("YYYY-MM-DD");
    const tomorrowDate = moment().add(1, "days").format("YYYY-MM-DD");

    beforeEach(async () => {
      fixture = TestBed.createComponent(ZaakViewComponent);
      component = fixture.componentInstance;
      component.zaak = { ...zaak } as GeneratedType<"RestZaak">;

      loader = TestbedHarnessEnvironment.loader(fixture);

      fixture.detectChanges();
      await fixture.whenStable();
    });

    it.each([
      [
        {
          einddatum: null,
          einddatumGepland: null,
          uiterlijkeEinddatumAfdoening: yesterdayDate,
        },
        1,
      ],
      [
        {
          einddatum: null,
          einddatumGepland: yesterdayDate,
          uiterlijkeEinddatumAfdoening: yesterdayDate,
        },
        2,
      ],
      [
        {
          einddatum: null,
          einddatumGepland: null,
          uiterlijkeEinddatumAfdoening: yesterdayDate,
        },
        1,
      ],
      [
        {
          einddatum: null,
          einddatumGepland: null,
          uiterlijkeEinddatumAfdoening: null,
        },
        0,
      ],
      [
        {
          einddatum: null,
          einddatumGepland: tomorrowDate,
          uiterlijkeEinddatumAfdoening: tomorrowDate,
        },
        0,
      ],
      [
        {
          einddatum: today,
          einddatumGepland: tomorrowDate,
          uiterlijkeEinddatumAfdoening: tomorrowDate,
        },
        0,
      ],
    ])(
      "shows the correct warning icons for overdue data",
      async (zaakData, expectedIcons) => {
        mockActivatedRoute.data.next({ zaak: { ...zaak, ...zaakData } });

        const icons = await loader.getAllHarnesses(
          MatIconHarness.with({ name: "report_problem" }),
        );

        expect(icons.length).toBe(expectedIcons);
      },
    );
  });

  describe("actie.ontvangstbevestiging.versturen", () => {
    const baseZaak = {
      ...zaak,
      heeftOntvangstbevestigingVerstuurd: false,
      rechten: {
        ...zaak.rechten,
        behandelen: true,
        versturenOntvangstbevestiging: true,
      },
      isProcesGestuurd: false,
      indicaties: ["ONTVANGSTBEVESTIGING_NIET_VERSTUURD"],
    } satisfies GeneratedType<"RestZaak">;

    beforeEach(() => {
      mockActivatedRoute.data.next({ zaak: baseZaak });
      fixture.detectChanges();
    });

    it("should show the button when all conditions are met", async () => {
      const button = await loader.getHarness(
        MatNavListItemHarness.with({
          title: "actie.ontvangstbevestiging.versturen",
        }),
      );
      expect(button).toBeTruthy();
    });

    describe("when behandelen right is false", () => {
      beforeEach(() => {
        mockActivatedRoute.data.next({
          zaak: {
            ...baseZaak,
            heeftOntvangstbevestigingVerstuurd: false,
            rechten: {
              ...baseZaak.rechten,
              behandelen: false,
            },
          },
        });
        fixture.detectChanges();
      });

      it("should not show the button", async () => {
        const button = await loader.getHarnessOrNull(
          MatNavListItemHarness.with({
            title: "actie.ontvangstbevestiging.versturen",
          }),
        );
        expect(button).toBeNull();
      });
    });

    describe("when isProcesGestuurd is true", () => {
      beforeEach(() => {
        mockActivatedRoute.data.next({
          zaak: {
            ...baseZaak,
            isProcesGestuurd: true,
          },
        });
        fixture.detectChanges();
      });

      it("should not show the button", async () => {
        const button = await loader.getHarnessOrNull(
          MatNavListItemHarness.with({
            title: "actie.ontvangstbevestiging.versturen",
          }),
        );
        expect(button).toBeNull();
      });
    });

    describe("when versturenOntvangstbevestiging right is false", () => {
      beforeEach(() => {
        mockActivatedRoute.data.next({
          zaak: {
            ...baseZaak,
            rechten: {
              ...baseZaak.rechten,
              versturenOntvangstbevestiging: false,
            },
          },
        });
        fixture.detectChanges();
      });

      it("should not show the button", async () => {
        const button = await loader.getHarnessOrNull(
          MatNavListItemHarness.with({
            title: "actie.ontvangstbevestiging.versturen",
          }),
        );
        expect(button).toBeNull();
      });
    });

    describe("when heeftOntvangstbevestigingVerstuurd is set", () => {
      beforeEach(() => {
        mockActivatedRoute.data.next({
          zaak: {
            ...baseZaak,
            heeftOntvangstbevestigingVerstuurd: true,
          },
        });
        fixture.detectChanges();
      });

      it("should not show the button", async () => {
        const button = await loader.getHarnessOrNull(
          MatNavListItemHarness.with({
            title: "actie.ontvangstbevestiging.versturen",
          }),
        );
        expect(button).toBeNull();
      });
    });
  });

  describe("openPlanItemStartenDialog", () => {
    beforeEach(() => {
      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();
    });

    it("should open side menu and set action when dialog returns 'openBesluitVastleggen'", async () => {
      const openSpy = jest.spyOn(
        fixture.componentInstance.actionsSidenav,
        "open",
      );
      jest
        .spyOn(dialogRef, "afterClosed")
        .mockReturnValue(of("openBesluitVastleggen"));

      const listItem = await loader.getHarnessOrNull(
        MatNavListItemHarness.with({ text: /planitem.INTAKE_AFRONDEN/ }),
      );

      await listItem?.click();

      expect(openSpy).toHaveBeenCalled();
      expect(fixture.componentInstance.activeSideAction).toBe(
        "actie.besluit.vastleggen",
      );
    });

    it("should show snackbar when dialog returns other value", async () => {
      const spy = jest.spyOn(utilService, "openSnackbar");
      jest.spyOn(dialogRef, "afterClosed").mockReturnValue(of("otherValue"));

      const listItem = await loader.getHarnessOrNull(
        MatNavListItemHarness.with({ text: /planitem.INTAKE_AFRONDEN/ }),
      );

      await listItem?.click();

      expect(spy).toHaveBeenCalledWith(
        "msg.planitem.uitgevoerd.INTAKE_AFRONDEN",
      );
      expect(fixture.componentInstance.activeSideAction).toBe(null);
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

    it("should add menu subscription to subscriptions$ array when setupMenu is called", () => {
      expect(subscriptionsPushSpy).toHaveBeenCalledTimes(1);
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

      expect(fixture.nativeElement.querySelector("zac-notities")).toBeTruthy();
    });

    it("should render <zac-notities> when notitieRechten.wijzigen is true", () => {
      jest
        .spyOn(policyService, "readNotitieRechten")
        .mockReturnValue(of({ lezen: false, wijzigen: true }));
      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector("zac-notities")).toBeTruthy();
    });

    it("should not render <zac-notities> when both notitieRechten.lezen and wijzigen are false", () => {
      jest
        .spyOn(policyService, "readNotitieRechten")
        .mockReturnValue(of({ lezen: false, wijzigen: false }));
      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector("zac-notities")).toBeNull();
    });

    it("should not render <zac-notities> when notitieRechten is absent", () => {
      jest.spyOn(policyService, "readNotitieRechten").mockReturnValue(EMPTY);
      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector("zac-notities")).toBeNull();
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
              GeneratedType<"RestZaakafhandelParameters">
            >({
              betrokkeneKoppelingen: koppelingen,
            }),
          },
        },
      });
      fixture.detectChanges();

      expect(
        fixture.nativeElement.querySelector("zac-zaak-initiator-toevoegen"),
      ).toBeTruthy();
    });

    it("should show zac-persoongegevens when initiator type is BSN", () => {
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
              GeneratedType<"RestZaakafhandelParameters">
            >({
              betrokkeneKoppelingen: koppelingen,
            }),
          },
        },
      });
      fixture.detectChanges();

      expect(
        fixture.nativeElement.querySelector("zac-persoongegevens"),
      ).toBeTruthy();
    });

    it("should show zac-bedrijfsgegevens when initiator type is VN", () => {
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
              GeneratedType<"RestZaakafhandelParameters">
            >({
              betrokkeneKoppelingen: koppelingen,
            }),
          },
        },
      });
      fixture.detectChanges();

      expect(
        fixture.nativeElement.querySelector("zac-bedrijfsgegevens"),
      ).toBeTruthy();
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
        fixture.nativeElement.querySelector("zac-contactgegevens"),
      ).toBeTruthy();
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
              GeneratedType<"RestZaakafhandelParameters">
            >({
              betrokkeneKoppelingen: koppelingen,
            }),
          },
        },
      });
      fixture.detectChanges();

      expect(
        fixture.nativeElement.querySelector("zac-contactgegevens"),
      ).toBeNull();
      expect(
        fixture.nativeElement.querySelector("zac-zaak-initiator-toevoegen"),
      ).toBeTruthy();
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
              GeneratedType<"RestZaakafhandelParameters">
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
        fixture.nativeElement.querySelector("zac-contactgegevens"),
      ).toBeNull();
      expect(
        fixture.nativeElement.querySelector("zac-zaak-initiator-toevoegen"),
      ).toBeNull();
    });
  });

  describe("actie.zaak.acties header", () => {
    const baseZaak = {
      ...zaak,
      isOpen: true,
      rechten: {
        ...zaak.rechten,
        behandelen: true,
      },
      isProcesGestuurd: false,
      isHeropend: false,
      isOpgeschort: false,
      eerdereOpschorting: false,
      zaaktype: {
        ...zaak.zaaktype,
        opschortingMogelijk: false,
        verlengingMogelijk: false,
      },
    } satisfies GeneratedType<"RestZaak">;

    beforeEach(() => {
      jest
        .spyOn(planItemsService, "listHumanTaskPlanItems")
        .mockReturnValue(of([]));
      jest
        .spyOn(planItemsService, "listProcessTaskPlanItems")
        .mockReturnValue(of([]));
    });

    it("should add header when userEventListenerPlanItems.length > 0 and actionMenuItems.length === 0", async () => {
      jest
        .spyOn(planItemsService, "listUserEventListenerPlanItems")
        .mockReturnValue(
          of([
            fromPartial<GeneratedType<"RESTPlanItem">>({
              userEventListenerActie: "INTAKE_AFRONDEN",
            }),
          ]),
        );

      mockActivatedRoute.data.next({ zaak: baseZaak });

      const subheader = await loader.getHarness(
        MatSubheaderHarness.with({ text: "actie.zaak.acties" }),
      );
      expect(subheader).toBeTruthy();
    });

    it("should add header when userEventListenerPlanItems.length === 0 and actionMenuItems.length > 0", async () => {
      jest
        .spyOn(planItemsService, "listUserEventListenerPlanItems")
        .mockReturnValue(of([]));

      mockActivatedRoute.data.next({
        zaak: {
          ...baseZaak,
          isOpen: false,
          rechten: {
            ...baseZaak.rechten,
            heropenen: true,
          },
        },
      });

      const subheader = await loader.getHarness(
        MatSubheaderHarness.with({ text: "actie.zaak.acties" }),
      );
      expect(subheader).toBeTruthy();
    });

    it("should not add header when both userEventListenerPlanItems.length === 0 and actionMenuItems.length === 0", async () => {
      jest
        .spyOn(planItemsService, "listUserEventListenerPlanItems")
        .mockReturnValue(of([]));

      mockActivatedRoute.data.next({ zaak: baseZaak });

      const subheader = await loader.getHarnessOrNull(
        MatSubheaderHarness.with({ text: "actie.zaak.acties" }),
      );
      expect(subheader).toBeNull();
    });
  });

  describe("Process Definition Flow tests", () => {
    const bpmnProcessDefinition = fromPartial<
      GeneratedType<"RestZaakBpmnProcessDefinition">
    >({
      processDefinitionKey: "test-key",
      processDefinitionName: "Test Process",
      processDefinitionVersion: 3,
    });

    const zaakWithBpmn = {
      ...zaak,
      bpmnProcessDefinition,
    } satisfies GeneratedType<"RestZaak">;

    describe("when bpmnProcessDefinition is set", () => {
      beforeEach(() => {
        mockActivatedRoute.data.next({ zaak: zaakWithBpmn });
        fixture.detectChanges();
      });

      it("should show the button", async () => {
        const button = await loader.getHarness(
          MatNavListItemHarness.with({ title: "actie.procesverloop.bekijken" }),
        );
        expect(button).toBeTruthy();
      });

      it("should open the sidenav and set the active action when clicked", async () => {
        const openSpy = jest.spyOn(
          fixture.componentInstance.actionsSidenav,
          "open",
        );

        const button = await loader.getHarness(
          MatNavListItemHarness.with({ title: "actie.procesverloop.bekijken" }),
        );
        await button.click();

        expect(openSpy).toHaveBeenCalled();
        expect(fixture.componentInstance.activeSideAction).toBe(
          "actie.procesverloop.bekijken",
        );
      });

      it("should render the process flow sidenav when clicked", async () => {
        const button = await loader.getHarness(
          MatNavListItemHarness.with({ title: "actie.procesverloop.bekijken" }),
        );
        await button.click();
        fixture.detectChanges();

        const processFlowLoader = await loader.getChildLoader(
          "zac-zaak-process-flow",
        );
        expect(processFlowLoader).toBeTruthy();
      });
    });

    describe("when bpmnProcessDefinition is not set", () => {
      beforeEach(() => {
        mockActivatedRoute.data.next({ zaak });
        fixture.detectChanges();
      });

      it("should not show the button", async () => {
        const button = await loader.getHarnessOrNull(
          MatNavListItemHarness.with({ title: "actie.procesverloop.bekijken" }),
        );
        expect(button).toBeNull();
      });
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
          GeneratedType<"RestZaakafhandelParameters">
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
              GeneratedType<"RestZaakafhandelParameters">
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
          GeneratedType<"RestZaakafhandelParameters">
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
              GeneratedType<"RestZaakafhandelParameters">
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

  describe("inactive group indicator", () => {
    it("should show 'inactief' label when groep is inactive", () => {
      mockActivatedRoute.data.next({
        zaak: { ...zaak, groep: { id: "g1", naam: "Groep A", active: false } },
      });
      fixture.detectChanges();

      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector("em")?.textContent?.trim()).toBe("(inactief)");
    });

    it("should not show 'inactief' label when groep is active", () => {
      mockActivatedRoute.data.next({
        zaak: { ...zaak, groep: { id: "g1", naam: "Groep A", active: true } },
      });
      fixture.detectChanges();

      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector("em")).toBeNull();
    });

    it("should not crash when groep is null", () => {
      mockActivatedRoute.data.next({ zaak: { ...zaak, groep: null } });
      fixture.detectChanges();

      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector("em")).toBeNull();
    });
  });

  describe("Menu item ordering", () => {
    it("should sort human task plan items alphabetically by their name", () => {
      jest
        .spyOn(planItemsService, "listHumanTaskPlanItems")
        .mockReturnValue(
          of(
            ["Goedkeuren", "Advies extern", "Document verzenden", "Advies intern"].map(
              (naam) => fromPartial<GeneratedType<"RESTPlanItem">>({ naam }),
            ),
          ),
        );

      mockActivatedRoute.data.next({
        zaak: {
          ...zaak,
          rechten: {
            ...zaak.rechten,
            behandelen: true,
          },
        },
      });

      const menu = fixture.componentInstance.menu;
      const startHeaderIndex = menu.findIndex(
        (menuItem) => menuItem.title === "actie.taak.starten",
      );
      const itemsAfterStartHeader = menu.slice(startHeaderIndex + 1);
      const nextHeaderOffset = itemsAfterStartHeader.findIndex(
        (menuItem) => menuItem.type === MenuItemType.HEADER,
      );
      const humanTaskTitles = itemsAfterStartHeader
        .slice(0, nextHeaderOffset === -1 ? undefined : nextHeaderOffset)
        .map((menuItem) => menuItem.title);

      expect(humanTaskTitles).toEqual([
        "Advies extern",
        "Advies intern",
        "Document verzenden",
        "Goedkeuren",
      ]);
    });
  });
});

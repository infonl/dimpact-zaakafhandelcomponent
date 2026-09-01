/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { LOCALE_ID } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import {
  MatNavListItemHarness,
  MatSubheaderHarness,
} from "@angular/material/list/testing";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import {
  provideQueryClient,
  queryOptions,
} from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { of, ReplaySubject } from "rxjs";
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
import { ZaakBrondatumZettenDialogComponent } from "../zaak-brondatum-zetten-dialog/zaak-brondatum-zetten-dialog.component";
import { ZaakDocumentenComponent } from "../zaak-documenten/zaak-documenten.component";
import { ZaakInitiatorToevoegenComponent } from "../zaak-initiator-toevoegen/zaak-initiator-toevoegen.component";
import { ZaakProcessFlowComponent } from "../zaak-process-flow/zaak-process-flow.component";
import { ZakenService } from "../zaken.service";
import { ZaakActionDialogsService } from "./services/zaak-action-dialogs.service";
import { ZaakDetailsCardComponent } from "./zaak-details-card/zaak-details-card.component";
import { ZaakSideActionService } from "./services/zaak-side-action.service";
import { ZaakViewComponent } from "./zaak-view.component";

describe(ZaakViewComponent.name, () => {
  let fixture: ComponentFixture<ZaakViewComponent>;
  let sideActions: ZaakSideActionService;
  let dialogs: ZaakActionDialogsService;
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
    loader = TestbedHarnessEnvironment.loader(fixture);
    sideActions = fixture.debugElement.injector.get(ZaakSideActionService);
    dialogs = fixture.debugElement.injector.get(ZaakActionDialogsService);

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
      expect(sideActions.activeAction()).toBe("actie.besluit.vastleggen");
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
      expect(sideActions.activeAction()).toBe(null);
    });
  });

  describe("actie.zaak.brondatumZetten", () => {
    const brondatumZettenZaak = {
      ...zaak,
      rechten: {
        ...zaak.rechten,
        brondatumZetten: true,
      },
      resultaat: fromPartial<GeneratedType<"RestZaakResultaat">>({
        resultaattype: fromPartial<GeneratedType<"RestResultaattype">>({
          bronArchiefprocedure: fromPartial<
            GeneratedType<"BrondatumArchiefprocedure">
          >({
            // the backend returns this value in lowercase, unlike the
            // uppercase generated typescript enum type
            afleidingswijze:
              "eigenschap" as GeneratedType<"AfleidingswijzeEnum">,
          }),
        }),
      }),
    } satisfies GeneratedType<"RestZaak">;

    it("should show the button when the brondatumZetten right is true and the afleidingswijze is EIGENSCHAP", async () => {
      mockActivatedRoute.data.next({ zaak: brondatumZettenZaak });

      const button = await loader.getHarness(
        MatNavListItemHarness.with({
          title: "actie.zaak.brondatumZetten",
        }),
      );
      expect(button).toBeTruthy();
    });

    it("should not show the button when the brondatumZetten right is false", async () => {
      mockActivatedRoute.data.next({
        zaak: {
          ...brondatumZettenZaak,
          rechten: { ...brondatumZettenZaak.rechten, brondatumZetten: false },
        },
      });

      const button = await loader.getHarnessOrNull(
        MatNavListItemHarness.with({
          title: "actie.zaak.brondatumZetten",
        }),
      );
      expect(button).toBeNull();
    });

    it("should not show the button when the afleidingswijze is not EIGENSCHAP", async () => {
      mockActivatedRoute.data.next({
        zaak: {
          ...brondatumZettenZaak,
          resultaat: fromPartial<GeneratedType<"RestZaakResultaat">>({
            resultaattype: fromPartial<GeneratedType<"RestResultaattype">>({
              bronArchiefprocedure: fromPartial<
                GeneratedType<"BrondatumArchiefprocedure">
              >({
                afleidingswijze: "TERMIJN",
              }),
            }),
          }),
        },
      });

      const button = await loader.getHarnessOrNull(
        MatNavListItemHarness.with({
          title: "actie.zaak.brondatumZetten",
        }),
      );
      expect(button).toBeNull();
    });

    it("should not show the button when the resultaat is absent", async () => {
      mockActivatedRoute.data.next({
        zaak: { ...brondatumZettenZaak, resultaat: null },
      });

      const button = await loader.getHarnessOrNull(
        MatNavListItemHarness.with({
          title: "actie.zaak.brondatumZetten",
        }),
      );
      expect(button).toBeNull();
    });

    it("should open the dialog with the zaak data when clicked", async () => {
      mockActivatedRoute.data.next({ zaak: brondatumZettenZaak });
      const dialog = TestBed.inject(MatDialog);

      const button = await loader.getHarness(
        MatNavListItemHarness.with({
          title: "actie.zaak.brondatumZetten",
        }),
      );
      await button.click();

      expect(dialog.open).toHaveBeenCalledWith(
        ZaakBrondatumZettenDialogComponent,
        { data: { zaak: brondatumZettenZaak } },
      );
    });

    it("should update the zaak, reload the taken and show a snackbar when the dialog closes with a result", async () => {
      mockActivatedRoute.data.next({ zaak: brondatumZettenZaak });
      const readZaakSpy = jest.spyOn(zakenService, "readZaak");
      const snackbarSpy = jest.spyOn(utilService, "openSnackbar");
      jest.spyOn(dialogRef, "afterClosed").mockReturnValue(of(true));

      const button = await loader.getHarness(
        MatNavListItemHarness.with({
          title: "actie.zaak.brondatumZetten",
        }),
      );
      const invalidateSpy = jest.spyOn(testQueryClient, "invalidateQueries");
      await button.click();

      expect(readZaakSpy).toHaveBeenCalledWith(brondatumZettenZaak.uuid);
      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: takenService.listTakenVoorZaakQuery(brondatumZettenZaak.uuid)
          .queryKey,
      });
      expect(snackbarSpy).toHaveBeenCalledWith("msg.zaak.brondatum.gezet");
    });

    it("should not update the zaak when the dialog closes without a result", async () => {
      mockActivatedRoute.data.next({ zaak: brondatumZettenZaak });
      const updateZaakSpy = jest.spyOn(fixture.componentInstance, "updateZaak");
      jest.spyOn(dialogRef, "afterClosed").mockReturnValue(of(undefined));

      const button = await loader.getHarness(
        MatNavListItemHarness.with({
          title: "actie.zaak.brondatumZetten",
        }),
      );
      await button.click();

      expect(updateZaakSpy).not.toHaveBeenCalled();
    });
  });

  describe("openZaakAfbrekenDialog", () => {
    beforeEach(() => {
      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();
    });

    it("writes the returned zaak into the cache when the dialog closes with one", () => {
      const cacheZaakSpy = jest
        .spyOn(zakenService, "cacheZaak")
        .mockImplementation();
      const fakeReturnedZaak = fromPartial<GeneratedType<"RestZaak">>({
        uuid: zaak.uuid,
      });
      jest
        .spyOn(dialogRef, "afterClosed")
        .mockReturnValue(of(fakeReturnedZaak));

      dialogs.openAfbreken(zaak);

      expect(cacheZaakSpy).toHaveBeenCalledWith(fakeReturnedZaak);
    });

    it("falls back to a refetch when the dialog closes with a confirmation-only result", () => {
      jest.spyOn(zakenService, "cacheZaak").mockImplementation();
      const readZaakSpy = jest
        .spyOn(zakenService, "readZaak")
        .mockReturnValue(of(zaak));
      const invalidateSpy = jest.spyOn(testQueryClient, "invalidateQueries");
      jest.spyOn(dialogRef, "afterClosed").mockReturnValue(of(true));

      dialogs.openAfbreken(zaak);

      expect(readZaakSpy).toHaveBeenCalledWith(zaak.uuid);
      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: takenService.listTakenVoorZaakQuery(zaak.uuid).queryKey,
      });
    });
  });

  describe("openZaakHeropenenDialog", () => {
    beforeEach(() => {
      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();
    });

    it("writes the returned zaak into the cache when the dialog closes with one", () => {
      const cacheZaakSpy = jest
        .spyOn(zakenService, "cacheZaak")
        .mockImplementation();
      const fakeReturnedZaak = fromPartial<GeneratedType<"RestZaak">>({
        uuid: zaak.uuid,
      });
      jest
        .spyOn(dialogRef, "afterClosed")
        .mockReturnValue(of(fakeReturnedZaak));

      dialogs.openHeropenen(zaak);

      expect(cacheZaakSpy).toHaveBeenCalledWith(fakeReturnedZaak);
    });

    it("falls back to a refetch when the dialog closes with a confirmation-only result", () => {
      jest.spyOn(zakenService, "cacheZaak").mockImplementation();
      const readZaakSpy = jest
        .spyOn(zakenService, "readZaak")
        .mockReturnValue(of(zaak));
      const invalidateSpy = jest.spyOn(testQueryClient, "invalidateQueries");
      jest.spyOn(dialogRef, "afterClosed").mockReturnValue(of(true));

      dialogs.openHeropenen(zaak);

      expect(readZaakSpy).toHaveBeenCalledWith(zaak.uuid);
      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: takenService.listTakenVoorZaakQuery(zaak.uuid).queryKey,
      });
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
        expect(sideActions.activeAction()).toBe("actie.procesverloop.bekijken");
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

  describe("Menu item ordering", () => {
    it("should sort human task plan items alphabetically by their name", () => {
      jest
        .spyOn(planItemsService, "listHumanTaskPlanItems")
        .mockReturnValue(
          of(
            [
              "Goedkeuren",
              "Advies extern",
              "Document verzenden",
              "Advies intern",
            ].map((naam) =>
              fromPartial<GeneratedType<"RESTPlanItem">>({ naam }),
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
      fixture.detectChanges();

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

  describe("the panel each menu button opens", () => {
    const zaakWithEveryPanel = fromPartial<GeneratedType<"RestZaak">>({
      ...zaak,
      isOpen: true,
      isInIntakeFase: false,
      isBesluittypeAanwezig: true,
      heeftOntvangstbevestigingVerstuurd: false,
      zaakdata: { fakeZaakdataKey: "fakeZaakdataValue" },
      zaakgeometrie: undefined,
      bpmnProcessDefinition: fromPartial<
        GeneratedType<"RestZaakBpmnProcessDefinition">
      >({ processDefinitionKey: "fakeProcessDefinitionKey" }),
      rechten: {
        ...zaak.rechten,
        behandelen: true,
        wijzigen: true,
        wijzigenLocatie: true,
        creerenDocument: true,
        versturenEmail: true,
        versturenOntvangstbevestiging: true,
        bekijkenZaakdata: true,
        toevoegenBagObject: true,
        toevoegenInitiatorBedrijf: true,
      },
      zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
        ...zaak.zaaktype,
        zaakafhandelparameters: fromPartial<
          GeneratedType<"RestZaaktypeConfiguration">
        >({
          smartDocuments: { enabledForZaaktype: true, enabledGlobally: true },
          betrokkeneKoppelingen: { kvkKoppelen: true },
        }),
      }),
    });

    const panelsOpenedFromTheMenu = [
      ["actie.ontvangstbevestiging.versturen", "zac-ontvangstbevestiging"],
      ["actie.mail.versturen", "zac-mail-create"],
      ["actie.document.maken", "zac-informatie-object-create-attended"],
      ["actie.document.toevoegen", "zac-informatie-object-add"],
      ["actie.document.verzenden", "zac-informatie-verzenden"],
      ["actie.besluit.vastleggen", "zac-besluit-create"],
      ["actie.zaakdata.bekijken", "zac-zaakdata"],
      ["actie.procesverloop.bekijken", "zac-zaak-process-flow"],
      ["actie.betrokkene.koppelen", "zac-klant-koppel"],
      ["actie.bagObject.koppelen", "zac-bag-zoek"],
      ["actie.zaak.koppelen", "zac-zaak-link"],
      ["actie.zaak.locatie.koppelen", "zac-case-location-edit"],
    ] as const;

    beforeEach(() => {
      mockActivatedRoute.data.next({ zaak: zaakWithEveryPanel });
      fixture.detectChanges();
    });

    it.each(panelsOpenedFromTheMenu)(
      "shows %s in the sidenav",
      async (title, panel) => {
        const button = await loader.getHarness(
          MatNavListItemHarness.with({ title }),
        );

        await button.click();
        fixture.detectChanges();

        expect(await loader.getChildLoader(panel)).toBeTruthy();
      },
    );
  });
});

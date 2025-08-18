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
import { MatNavListItemHarness } from "@angular/material/list/testing";
import { MatSidenav } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import moment from "moment";
import { of, ReplaySubject } from "rxjs";
import { UtilService } from "src/app/core/service/util.service";
import { StaticTextComponent } from "src/app/shared/static-text/static-text.component";
import { ZaakafhandelParametersService } from "../../admin/zaakafhandel-parameters.service";
import { BAGService } from "../../bag/bag.service";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { KlantenService } from "../../klanten/klanten.service";
import { PersoonsgegevensComponent } from "../../klanten/persoonsgegevens/persoonsgegevens.component";
import { NotitiesComponent } from "../../notities/notities.component";
import { PlanItemsService } from "../../plan-items/plan-items.service";
import { PolicyService } from "../../policy/policy.service";
import { ZaakIndicatiesComponent } from "../../shared/indicaties/zaak-indicaties/zaak-indicaties.component";
import { MaterialModule } from "../../shared/material/material.module";
import { PipesModule } from "../../shared/pipes/pipes.module";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "../../shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TakenService } from "../../taken/taken.service";
import { ZaakDocumentenComponent } from "../zaak-documenten/zaak-documenten.component";
import { ZaakInitiatorToevoegenComponent } from "../zaak-initiator-toevoegen/zaak-initiator-toevoegen.component";
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
  let identityService: IdentityService;
  let websocketService: WebsocketService;
  let zaakafhandelParametersService: ZaakafhandelParametersService;

  const mockActivatedRoute = {
    data: new ReplaySubject<{ zaak: GeneratedType<"RestZaak"> }>(1),
  };

  const zaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "1234",
    zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
      omschrijving: "mock description",
    }),
    indicaties: [],
    rechten: {},
    groep: {},
    vertrouwelijkheidaanduiding: "OPENBAAR",
    gerelateerdeZaken: [],
    initiatorIdentificatieType: "BSN",
  });

  beforeEach(async () => {
    dialogRef = {
      afterClosed: jest.fn().mockReturnValue(of(undefined)),
    } as unknown as MatDialogRef<unknown>;

    const dialogMock = {
      open: jest.fn().mockReturnValue(dialogRef),
    };

    await TestBed.configureTestingModule({
      declarations: [
        ZaakViewComponent,
        ZaakIndicatiesComponent,
        ZaakDocumentenComponent,
        NotitiesComponent,
        SideNavComponent,
        PersoonsgegevensComponent,
        StaticTextComponent,
        ZaakInitiatorToevoegenComponent,
      ],
      imports: [
        TranslateModule.forRoot(),
        PipesModule,
        MaterialModule,
        VertrouwelijkaanduidingToTranslationKeyPipe,
        NoopAnimationsModule,
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
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
    jest.spyOn(zakenService, "listHistorieVoorZaak").mockReturnValue(of([]));
    jest.spyOn(zakenService, "listBetrokkenenVoorZaak").mockReturnValue(of([]));
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
      .mockReturnValue(of([]));
    jest
      .spyOn(planItemsService, "listHumanTaskPlanItems")
      .mockReturnValue(of([]));
    jest
      .spyOn(planItemsService, "listProcessTaskPlanItems")
      .mockReturnValue(of([]));

    takenService = TestBed.inject(TakenService);
    jest.spyOn(takenService, "listTakenVoorZaak").mockReturnValue(of([]));

    identityService = TestBed.inject(IdentityService);
    jest
      .spyOn(identityService, "readLoggedInUser")
      .mockReturnValue(of(fromPartial<GeneratedType<"RestLoggedInUser">>({})));

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
      isEerderOpgeschort: false,
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

    describe("isEerderOpgeschort", () => {
      beforeEach(() => {
        mockActivatedRoute.data.next({
          zaak: {
            ...opschortenZaak,
            isEerderOpgeschort: true,
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
          einddatum: undefined,
          einddatumGepland: undefined,
          uiterlijkeEinddatumAfdoening: yesterdayDate,
        },
        1,
      ],
      [
        {
          einddatum: undefined,
          einddatumGepland: yesterdayDate,
          uiterlijkeEinddatumAfdoening: yesterdayDate,
        },
        2,
      ],
      [
        {
          einddatum: undefined,
          einddatumGepland: undefined,
          uiterlijkeEinddatumAfdoening: yesterdayDate,
        },
        1,
      ],
      [
        {
          einddatum: undefined,
          einddatumGepland: undefined,
          uiterlijkeEinddatumAfdoening: undefined,
        },
        0,
      ],
      [
        {
          einddatum: undefined,
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

  describe("openPlanItemStartenDialog", () => {
    const mockPlanItem = fromPartial<GeneratedType<"RESTPlanItem">>({
      userEventListenerActie: "ZAAK_AFHANDELEN",
    });

    beforeEach(() => {
      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();
    });

    it("should open side menu and set action when dialog returns 'openBesluitVastleggen'", () => {
      const openSpy = jest.spyOn(
        fixture.componentInstance.actionsSidenav,
        "open",
      );
      jest
        .spyOn(dialogRef, "afterClosed")
        .mockReturnValue(of("openBesluitVastleggen"));

      fixture.componentInstance.openPlanItemStartenDialog(mockPlanItem);

      expect(openSpy).toHaveBeenCalled();
      expect(fixture.componentInstance.activeSideAction).toBe(
        "actie.besluit.vastleggen",
      );
    });

    it("should show snackbar when dialog returns other value", () => {
      const spy = jest.spyOn(utilService, "openSnackbar");
      jest.spyOn(dialogRef, "afterClosed").mockReturnValue(of("otherValue"));

      fixture.componentInstance.openPlanItemStartenDialog(mockPlanItem);

      expect(spy).toHaveBeenCalledWith(
        "msg.planitem.uitgevoerd.ZAAK_AFHANDELEN",
      );
      expect(fixture.componentInstance.activeSideAction).toBe(null);
    });
  });
});

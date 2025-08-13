/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { Component, Input } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormControl } from "@angular/forms";
import { MatNavListItemHarness } from "@angular/material/list/testing";
import { MatSidenav } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import moment from "moment";
import { of, ReplaySubject } from "rxjs";
import { UtilService } from "src/app/core/service/util.service";
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

  @Component({
    selector: "zac-static-text",
    template: `
      <mat-icon
        *ngIf="icon"
        [ngClass]="icon.styleClass"
        [attr.title]="icon.title"
      >
        {{ icon.icon }}
      </mat-icon>
    `,
  })
  class ZacStaticTextStub {
    @Input() icon: unknown;
  }

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
        ZacStaticTextStub,
        ZaakIndicatiesComponent,
        ZaakDocumentenComponent,
        NotitiesComponent,
        SideNavComponent,
        PersoonsgegevensComponent,
        ZaakInitiatorToevoegenComponent,
      ],
      imports: [
        TranslateModule.forRoot(),
        NoopAnimationsModule,
        PipesModule,
        MaterialModule,
        VertrouwelijkaanduidingToTranslationKeyPipe,
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

    describe("dateFieldIconMap icon logic", () => {
      let component: ZaakViewComponent;
      const yesterdayDate = moment().subtract(1, "days").format("YYYY-MM-DD");
      const today = moment().format("YYYY-MM-DD");
      const tomorrowDate = moment().add(1, "days").format("YYYY-MM-DD");

      const yesterdayFormControl = new FormControl(yesterdayDate);
      const tomorrowFormControl = new FormControl(tomorrowDate);

      beforeEach(async () => {
        fixture = TestBed.createComponent(ZaakViewComponent);
        component = fixture.componentInstance;
        component.zaak = { ...zaak } as GeneratedType<"RestZaak">;

        jest
          .spyOn(
            component as unknown as { loadHistorie: () => void },
            "loadHistorie",
          )
          .mockImplementation(() => {});
        jest
          .spyOn(
            component as unknown as { loadBetrokkenen: () => void },
            "loadBetrokkenen",
          )
          .mockImplementation(() => {});
        jest
          .spyOn(
            component as unknown as { loadBagObjecten: () => void },
            "loadBagObjecten",
          )
          .mockImplementation(() => {});
        jest
          .spyOn(component as unknown as { setupMenu: () => void }, "setupMenu")
          .mockImplementation(() => {});
        jest
          .spyOn(
            component as unknown as { loadOpschorting: () => void },
            "loadOpschorting",
          )
          .mockImplementation(() => {});

        component.init(component.zaak);
        loader = TestbedHarnessEnvironment.loader(fixture);

        fixture.detectChanges();
        await fixture.whenStable();
      });

      it.only("shows icons for overdue dates in open case", async () => {
        // Arrange: set up overdue dates
        component.zaak = {
          ...component.zaak,
          einddatum: undefined,
          einddatumGepland: yesterdayDate,
          uiterlijkeEinddatumAfdoening: yesterdayDate,
        };

        fixture.detectChanges();
        await fixture.whenStable();

        const staticTextIcons = fixture.nativeElement.querySelectorAll(
          "zac-static-text mat-icon",
        );

        const streefdatumIcon = Array.from(staticTextIcons).find((icon) =>
          (icon as HTMLElement).className.includes("warning"),
        ) as HTMLElement;
        const fataledatumIcon = Array.from(staticTextIcons).find((icon) =>
          (icon as HTMLElement).className.includes("error"),
        ) as HTMLElement;

        expect(streefdatumIcon.textContent?.trim()).toBe("report_problem");
        expect(streefdatumIcon.getAttribute("title")).toContain("overschreden");

        expect(fataledatumIcon.textContent?.trim()).toBe("report_problem");
        expect(fataledatumIcon.getAttribute("title")).toContain("overschreden");

        expect(
          component.dateFieldIconMap
            .get("einddatumGepland")!
            .showIcon(yesterdayFormControl),
        ).toBe(true);

        expect(
          component.dateFieldIconMap
            .get("uiterlijkeEinddatumAfdoening")!
            .showIcon(yesterdayFormControl),
        ).toBe(true);
      });

      it("shows icons for overdue dates when case is closed after deadlines", async () => {
        component.zaak = {
          ...component.zaak,
          einddatum: today,
          einddatumGepland: yesterdayDate,
          uiterlijkeEinddatumAfdoening: yesterdayDate,
        };

        fixture.detectChanges();
        await fixture.whenStable();

        const staticTextIcons = fixture.nativeElement.querySelectorAll(
          "zac-static-text mat-icon",
        );

        const streefdatumIcon = Array.from(staticTextIcons).find((icon) =>
          (icon as HTMLElement).className.includes("warning"),
        ) as HTMLElement;
        const fataledatumIcon = Array.from(staticTextIcons).find((icon) =>
          (icon as HTMLElement).className.includes("error"),
        ) as HTMLElement;

        expect(streefdatumIcon.textContent?.trim()).toBe("report_problem");
        expect(streefdatumIcon.getAttribute("title")).toContain("overschreden");

        expect(fataledatumIcon.textContent?.trim()).toBe("report_problem");
        expect(fataledatumIcon.getAttribute("title")).toContain("overschreden");

        expect(
          component.dateFieldIconMap
            .get("einddatumGepland")!
            .showIcon(yesterdayFormControl),
        ).toBe(true);

        expect(
          component.dateFieldIconMap
            .get("uiterlijkeEinddatumAfdoening")!
            .showIcon(yesterdayFormControl),
        ).toBe(true);
      });

      it("does not show icons for future dates in open case", async () => {
        component.zaak = {
          ...component.zaak,
          einddatum: undefined,
          einddatumGepland: tomorrowDate,
          uiterlijkeEinddatumAfdoening: tomorrowDate,
        };

        fixture.detectChanges();
        await fixture.whenStable();

        const staticTextIcons = fixture.nativeElement.querySelectorAll(
          "zac-static-text mat-icon",
        );

        const streefdatumIcon = Array.from(staticTextIcons).find((icon) =>
          (icon as HTMLElement).className.includes("warning"),
        ) as HTMLElement;

        const fataledatumIcon = Array.from(staticTextIcons).find((icon) =>
          (icon as HTMLElement).className.includes("error"),
        ) as HTMLElement;

        expect(streefdatumIcon).toBeUndefined();
        expect(fataledatumIcon).toBeUndefined();

        expect(
          component.dateFieldIconMap
            .get("einddatumGepland")!
            .showIcon(tomorrowFormControl),
        ).toBe(false);

        expect(
          component.dateFieldIconMap
            .get("uiterlijkeEinddatumAfdoening")!
            .showIcon(tomorrowFormControl),
        ).toBe(false);
      });

      it("does not show icons when case is closed before deadlines", async () => {
        component.zaak = {
          ...component.zaak,
          einddatum: today,
          einddatumGepland: tomorrowDate,
          uiterlijkeEinddatumAfdoening: tomorrowDate,
        };

        fixture.detectChanges();
        await fixture.whenStable();

        const staticTextIcons = fixture.nativeElement.querySelectorAll(
          "zac-static-text mat-icon",
        );

        const streefdatumIcon = Array.from(staticTextIcons).find((icon) =>
          (icon as HTMLElement).className.includes("warning"),
        );
        const fataledatumIcon = Array.from(staticTextIcons).find((icon) =>
          (icon as HTMLElement).className.includes("error"),
        );

        expect(streefdatumIcon).toBeUndefined();
        expect(fataledatumIcon).toBeUndefined();

        expect(
          component.dateFieldIconMap
            .get("einddatumGepland")!
            .showIcon(tomorrowFormControl),
        ).toBe(false);

        expect(
          component.dateFieldIconMap
            .get("uiterlijkeEinddatumAfdoening")!
            .showIcon(tomorrowFormControl),
        ).toBe(false);
      });
    });
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

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
import { MatNavListItemHarness } from "@angular/material/list/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import { of, ReplaySubject } from "rxjs";
import { UtilService } from "src/app/core/service/util.service";
import { BAGService } from "../../bag/bag.service";
import { PersoonsgegevensComponent } from "../../klanten/persoonsgegevens/persoonsgegevens.component";
import { NotitiesComponent } from "../../notities/notities.component";
import { PlanItemsService } from "../../plan-items/plan-items.service";
import { ZaakIndicatiesComponent } from "../../shared/indicaties/zaak-indicaties/zaak-indicaties.component";
import { MaterialModule } from "../../shared/material/material.module";
import { PipesModule } from "../../shared/pipes/pipes.module";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "../../shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakDocumentenComponent } from "../zaak-documenten/zaak-documenten.component";
import { ZaakInitiatorToevoegenComponent } from "../zaak-initiator-toevoegen/zaak-initiator-toevoegen.component";
import { ZakenService } from "../zaken.service";
import { ZaakViewComponent } from "./zaak-view.component";
import { ConditionalFn } from "src/app/shared/utils/date-conditionals";
import { FormControl } from "@angular/forms";

describe(ZaakViewComponent.name, () => {
  let fixture: ComponentFixture<ZaakViewComponent>;
  let loader: HarnessLoader;

  let utilService: UtilService;
  let zakenService: ZakenService;
  let bagService: BAGService;
  let planItemsService: PlanItemsService;

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
    await TestBed.configureTestingModule({
      declarations: [
        ZaakViewComponent,
        ZaakIndicatiesComponent,
        StaticTextComponent,
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
        of(fromPartial<GeneratedType<"RESTZaakOpschorting">>({}))
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

    fixture = TestBed.createComponent(ZaakViewComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
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
        MatNavListItemHarness.with({ title: "actie.zaak.opschorten" })
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
          MatNavListItemHarness.with({ title: "actie.zaak.opschorten" })
        );
        expect(button).toBeNull();
      });
    });

    describe("dateFieldIconMap icon logic", () => {
      let component: ZaakViewComponent;
      const yesterdayDate = new Date(Date.now() - 86400000)
        .toISOString()
        .split("T")[0];
      const today = new Date().toISOString().slice(0, 10);
      const tomorrowDate = new Date(Date.now() + 86400000)
        .toISOString()
        .slice(0, 10);

      const yesterdayFormControl = new FormControl(yesterdayDate);
      const todayFormControl = new FormControl(today);
      const tomorrowFormControl = new FormControl(tomorrowDate);


      // interface FormControlStub {
      //   value: string;
      // }

      beforeEach(() => {
        component = fixture.componentInstance;
        component.zaak = { ...zaak } as GeneratedType<"RestZaak">;
        (
          component as unknown as { setDateFieldIconSet: () => void }
        ).setDateFieldIconSet();
      });

      function setZaakDates({
        einddatum,
        einddatumGepland,
        uiterlijkeEinddatumAfdoening,
      }: {
        einddatum: string | undefined;
        einddatumGepland: string | undefined;
        uiterlijkeEinddatumAfdoening: string | undefined;
      }): void {
        component.zaak.einddatum = einddatum;
        component.zaak.einddatumGepland = einddatumGepland;
        component.zaak.uiterlijkeEinddatumAfdoening = uiterlijkeEinddatumAfdoening;

        (component as unknown as { setDateFieldIconSet: () => void }).setDateFieldIconSet();
      }

      it("shows icons for overdue dates in open case", () => {
        setZaakDates({
          einddatum: undefined,
          einddatumGepland: yesterdayDate,
          uiterlijkeEinddatumAfdoening: yesterdayDate,
        });
        expect(
          component.dateFieldIconMap
            .get("einddatumGepland")!
            .showIcon(yesterdayFormControl)
        ).toBe(true);

        expect(
          component.dateFieldIconMap
            .get("uiterlijkeEinddatumAfdoening")!
            .showIcon(yesterdayFormControl)
        ).toBe(true);
      });

      it("shows icons for overdue dates when case is closed after deadlines", () => {
        setZaakDates({
          einddatum: today,
          einddatumGepland: yesterdayDate,
          uiterlijkeEinddatumAfdoening: yesterdayDate,
        });

        expect(
          component.dateFieldIconMap
            .get("einddatumGepland")!
            .showIcon(yesterdayFormControl)
        ).toBe(true);

        expect(
          component.dateFieldIconMap
            .get("uiterlijkeEinddatumAfdoening")!
            .showIcon(yesterdayFormControl)
        ).toBe(true);
      });

      it("does not show icons for future dates in open case", () => {
        setZaakDates({
          einddatum: undefined,
          einddatumGepland: tomorrowDate,
          uiterlijkeEinddatumAfdoening: tomorrowDate,
        });

        expect(
          component.dateFieldIconMap
            .get("einddatumGepland")!
            .showIcon(tomorrowFormControl)
        ).toBe(false);

        expect(
          component.dateFieldIconMap
            .get("uiterlijkeEinddatumAfdoening")!
            .showIcon(tomorrowFormControl)
        ).toBe(false);
      });

      it("does not show icons when case is closed before deadlines", () => {
        setZaakDates({
          einddatum: today,
          einddatumGepland: tomorrowDate,
          uiterlijkeEinddatumAfdoening: tomorrowDate,
        });

        expect(
          component.dateFieldIconMap
            .get("einddatumGepland")!
            .showIcon(tomorrowFormControl)
        ).toBe(false);

        expect(
          component.dateFieldIconMap
            .get("uiterlijkeEinddatumAfdoening")!
            .showIcon(tomorrowFormControl)
        ).toBe(false);
      });
    });
  });
});

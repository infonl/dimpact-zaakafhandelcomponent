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
import { MatNavListItemHarness } from "@angular/material/list/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import moment from "moment";
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
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakDocumentenComponent } from "../zaak-documenten/zaak-documenten.component";
import { ZaakInitiatorToevoegenComponent } from "../zaak-initiator-toevoegen/zaak-initiator-toevoegen.component";
import { ZakenService } from "../zaken.service";
import { ZaakViewComponent } from "./zaak-view.component";
import {MatIconHarness} from "@angular/material/icon/testing";

describe(ZaakViewComponent.name, () => {
  let fixture: ComponentFixture<ZaakViewComponent>;
  let loader: HarnessLoader;

  // @Component({
  //   selector: "zac-static-text",
  //   template: `
  //     <mat-icon
  //       *ngIf="icon"
  //       [ngClass]="icon.styleClass"
  //       [attr.title]="icon.title"
  //     >
  //       {{ icon.icon }}
  //     </mat-icon>
  //   `,
  // })
  // class ZacStaticTextStub {
  //   @Input() icon: unknown;
  //   @Input() value!: string;
  //   @Input() label!: string;
  // }

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
        // ZacStaticTextStub,
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
      mockActivatedRoute.data.next({zaak: opschortenZaak});
    });

    it("should show the button", async () => {
      const button = await loader.getHarness(
          MatNavListItemHarness.with({title: "actie.zaak.opschorten"}),
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
            MatNavListItemHarness.with({title: "actie.zaak.opschorten"}),
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
      [ {einddatum: undefined, einddatumGepland: undefined, uiterlijkeEinddatumAfdoening: yesterdayDate}, 1],
      // [ {einddatum: undefined, einddatumGepland: yesterdayDate, uiterlijkeEinddatumAfdoening: yesterdayDate}, 2],
      // [ {einddatum: undefined, einddatumGepland: undefined, uiterlijkeEinddatumAfdoening: yesterdayDate}, 1],
      // [ {einddatum: undefined, einddatumGepland: undefined, uiterlijkeEinddatumAfdoening: undefined}, 0],
      // [ {einddatum: undefined, einddatumGepland: tomorrowDate, uiterlijkeEinddatumAfdoening: tomorrowDate}, 0],
      // [ {einddatum: today, einddatumGepland: tomorrowDate, uiterlijkeEinddatumAfdoening: tomorrowDate}, 0],
    ])("shows the correct warning icons for overdue data", async (zaakData, expectedIcons) => {
      mockActivatedRoute.data.next({ zaak: {...zaak, ...zaakData } });
      
      component.zaak = { ...zaak, ...zaakData } as GeneratedType<"RestZaak">;

      console.log(component.zaak);

      component.init(component.zaak);

      fixture.detectChanges();
      await fixture.whenStable();

      const icons = await loader.getAllHarnesses(MatIconHarness.with({ name: 'report_problem' }));
      console.log('icons: ', icons);

      expect(icons.length).toBe(expectedIcons);
    })
  });
});
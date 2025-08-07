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
import { MatDialog } from "@angular/material/dialog";
import { MatButtonHarness } from "@angular/material/button/testing";
import { ZaakAfhandelenDialogComponent } from "../zaak-afhandelen-dialog/zaak-afhandelen-dialog.component";
import { MatSelectHarness } from "@angular/material/select/testing";

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

  const userEventPlanItems = [
    fromPartial<GeneratedType<"RESTPlanItem">>({
      actief: false,
      id: "5062",
      naam: "Zaak afhandelen",
      tabellen: {},
      type: "USER_EVENT_LISTENER",
      userEventListenerActie: "ZAAK_AFHANDELEN",
      zaakUuid: "a0b7ea92-7976-4bf2-a6fc-e883741eaf9e",
    }),
  ];

  const resultTypes = [
    {
      archiefNominatie: "BLIJVEND_BEWAREN",
      besluitVerplicht: true,
      id: "62178b42-d40f-4ee5-8d4d-3a7916ee3f33",
      naam: "Toegekend",
      naamGeneriek: "Toegekend",
      toelichting: "Toelichting bij resultaat Toegekend",
      vervaldatumBesluitVerplicht: false,
    },
    {
      archiefNominatie: "VERNIETIGEN",
      archiefTermijn: "1 jaar",
      besluitVerplicht: false,
      id: "03ffce63-0942-49f5-8a6e-031827f0ff19",
      naam: "Verleend",
      naamGeneriek: "Verleend",
      toelichting: "Toelichting bij resultaat Verleend",
      vervaldatumBesluitVerplicht: false,
    },
  ];

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
        ZaakAfhandelenDialogComponent,
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
      .mockReturnValue(of(userEventPlanItems));
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

  describe("planitem.ZAAK_AFHANDELEN", () => {
    const afhandelenZaak = {
      ...zaak,
      isOpen: true,
      rechten: {
        ...zaak.rechten,
        behandelen: true,
      },
      zaaktype: {
        ...zaak.zaaktype,
      },
      isHeropend: false,
      isOpgeschort: false,
      isEerderOpgeschort: false,
      isProcesGestuurd: false,
      status: {
        naam: "In behandeling",
        toelichting: "Status gewijzigd",
      },
    } satisfies GeneratedType<"RestZaak">;

    beforeEach(() => {
      mockActivatedRoute.data.next({ zaak: afhandelenZaak });
    });

    it("should show the button", async () => {
      const button = await loader.getHarness(
        MatNavListItemHarness.with({ title: "planitem.ZAAK_AFHANDELEN" }),
      );
      expect(button).toBeTruthy();
    });

    it("should open the dialog and find the button, dropdown and text inside", async () => {
      jest
        .spyOn(zakenService, "listResultaattypes")
        .mockReturnValue(of(resultTypes));

      const mainButton = await loader.getHarness(
        MatNavListItemHarness.with({ title: "planitem.ZAAK_AFHANDELEN" }),
      );

      const dialog = TestBed.inject(MatDialog);
      const openSpy = jest.spyOn(dialog, "open");

      await mainButton.click();
      fixture.detectChanges();
      await fixture.whenStable();

      expect(openSpy).toHaveBeenCalledWith(
        ZaakAfhandelenDialogComponent,
        expect.objectContaining({ data: expect.anything() }),
      );

      const overlayLoader =
        TestbedHarnessEnvironment.documentRootLoader(fixture);

      const dialogButton = await overlayLoader.getHarnessOrNull(
        MatNavListItemHarness.with({ title: "planitem.ZAAK_AFHANDELEN" }),
      );
      expect(dialogButton).not.toBeNull();

      const select = await overlayLoader.getHarness(MatSelectHarness);
      await select.open();
      const options = await select.getOptions();
      await options[0].click();
      expect(await select.getValueText()).toBe(await options[0].getText());

      await fixture.whenStable();
      fixture.detectChanges();

      // DEBUG: Check the dialog HTML structure
      const overlayContainerElement = document.querySelector(
        ".cdk-overlay-container",
      );
      expect(overlayContainerElement).not.toBeNull();
      const dialogHtml = overlayContainerElement!.outerHTML;
      expect(dialogHtml).toMatchSnapshot();
    });
  });
});

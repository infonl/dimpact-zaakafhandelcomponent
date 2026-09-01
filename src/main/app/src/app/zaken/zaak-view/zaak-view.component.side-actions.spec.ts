/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { LOCALE_ID } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import { By } from "@angular/platform-browser";
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
import { ZaakDetailsCardComponent } from "./zaak-details-card/zaak-details-card.component";
import { ZaakSideActionService } from "./services/zaak-side-action.service";
import { ZaakViewComponent } from "./zaak-view.component";

describe(ZaakViewComponent.name, () => {
  let fixture: ComponentFixture<ZaakViewComponent>;
  let sideActions: ZaakSideActionService;

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

  describe("closing the side action panel after an action finished", () => {
    let closeSideNav: jest.SpyInstance;

    beforeEach(() => {
      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();
      closeSideNav = jest
        .spyOn(fixture.componentInstance.actionsSidenav, "close")
        .mockResolvedValue("close");
      sideActions.activeAction.set("actie.mail.versturen");
      sideActions.actiefPlanItem.set(
        fromPartial<GeneratedType<"RESTPlanItem">>({ id: "fakePlanItemId" }),
      );
    });

    const finishedActions = [
      ["taakGestart", () => fixture.componentInstance["taakGestart"]()],
      ["mailVerstuurd", () => fixture.componentInstance["mailVerstuurd"](true)],
      [
        "ontvangstBevestigd",
        () => fixture.componentInstance["ontvangstBevestigd"](true),
      ],
      ["documentCreated", () => fixture.componentInstance["documentCreated"]()],
      ["documentSent", () => fixture.componentInstance["documentSent"]()],
      ["zaakLinked", () => fixture.componentInstance["zaakLinked"]()],
      [
        "locationSelected",
        () => fixture.componentInstance["locationSelected"](),
      ],
      [
        "besluitVastgelegd",
        () => fixture.componentInstance["besluitVastgelegd"](),
      ],
    ] as const;

    it.each(finishedActions)(
      "closes the sidenav and forgets both the panel and the plan item on %s",
      (_name, runAction) => {
        runAction();

        expect(closeSideNav).toHaveBeenCalled();
        expect(sideActions.activeAction()).toBeNull();
        expect(sideActions.actiefPlanItem()).toBeNull();
      },
    );

    it("does not refetch the zaak when the mail was not sent", () => {
      const readZaak = jest.spyOn(zakenService, "readZaak");

      fixture.componentInstance["mailVerstuurd"](false);

      expect(closeSideNav).toHaveBeenCalled();
      expect(readZaak).not.toHaveBeenCalled();
    });

    it("does not refetch the zaak when the ontvangstbevestiging was not sent", () => {
      const readZaak = jest.spyOn(zakenService, "readZaak");

      fixture.componentInstance["ontvangstBevestigd"](false);

      expect(closeSideNav).toHaveBeenCalled();
      expect(readZaak).not.toHaveBeenCalled();
    });

    it("refetches the zaak once a taak has been started", () => {
      const readZaak = jest
        .spyOn(zakenService, "readZaak")
        .mockReturnValue(of(zaak));

      fixture.componentInstance["taakGestart"]();

      expect(readZaak).toHaveBeenCalledWith(zaak.uuid);
    });
  });

  describe("panels the view switches to itself", () => {
    let openSideNav: jest.SpyInstance;

    beforeEach(() => {
      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();
      openSideNav = jest
        .spyOn(fixture.componentInstance.actionsSidenav, "open")
        .mockResolvedValue("open");
    });

    it("opens the initiator panel when the view asks to add or edit the initiator", () => {
      fixture.componentInstance["addOrEditZaakInitiator"]();

      expect(openSideNav).toHaveBeenCalled();
      expect(sideActions.activeAction()).toBe("actie.initiator.koppelen");
    });

    it("opens the besluit wijzigen panel with the besluit it was handed", () => {
      const besluit = fromPartial<GeneratedType<"RestBesluit">>({
        uuid: "fakeBesluitUuid",
      });

      fixture.componentInstance["besluitWijzigen"](besluit);

      expect(openSideNav).toHaveBeenCalled();
      expect(sideActions.activeAction()).toBe("actie.besluit.wijzigen");
      expect(fixture.componentInstance.teWijzigenBesluit).toBe(besluit);
    });

    it("opens the verplaatsen panel with the document it was handed", () => {
      const document = fromPartial<
        GeneratedType<"RestEnkelvoudigInformatieobject">
      >({ uuid: "fakeInformatieobjectUuid" });

      fixture.componentInstance["documentMoveToCase"](document);

      expect(openSideNav).toHaveBeenCalled();
      expect(sideActions.activeAction()).toBe("actie.document.verplaatsen");
      expect(fixture.componentInstance.documentToMove).toBe(document);
    });

    it("follows the panel the side nav reports without opening the sidenav itself", async () => {
      await fixture.componentInstance["menuItemChanged"](
        "actie.document.toevoegen",
      );

      expect(sideActions.activeAction()).toBe("actie.document.toevoegen");
      expect(openSideNav).not.toHaveBeenCalled();
    });
  });

  describe("starting a human task from the menu", () => {
    const humanTaskPlanItem = fromPartial<GeneratedType<"RESTPlanItem">>({
      id: "fakeHumanTaskPlanItemId",
      naam: "fakeHumanTaskNaam",
    });

    let openSideNav: jest.SpyInstance;
    let readHumanTaskPlanItem: jest.SpyInstance;

    beforeEach(() => {
      readHumanTaskPlanItem = jest
        .spyOn(planItemsService, "readHumanTaskPlanItem")
        .mockReturnValue(of(humanTaskPlanItem));
      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();
      openSideNav = jest
        .spyOn(fixture.componentInstance.actionsSidenav, "open")
        .mockResolvedValue("open");
    });

    it("fetches the full plan item and opens its own panel", () => {
      fixture.componentInstance["startHumanTaskPlanItem"](humanTaskPlanItem);

      expect(readHumanTaskPlanItem).toHaveBeenCalledWith(humanTaskPlanItem.id);
      expect(sideActions.actiefPlanItem()).toBe(humanTaskPlanItem);
      expect(sideActions.activeAction()).toBe("fakeHumanTaskNaam");
      expect(openSideNav).toHaveBeenCalled();
    });

    it("reopens the panel of the plan item it already loaded without fetching it again", () => {
      fixture.componentInstance["startHumanTaskPlanItem"](humanTaskPlanItem);
      readHumanTaskPlanItem.mockClear();
      sideActions.clear();

      fixture.componentInstance["startHumanTaskPlanItem"](humanTaskPlanItem);

      expect(readHumanTaskPlanItem).not.toHaveBeenCalled();
      expect(sideActions.activeAction()).toBe("fakeHumanTaskNaam");
    });
  });

  describe("updateDocumentList", () => {
    it("reloads the documenten table and invalidates the zaak historie", () => {
      mockActivatedRoute.data.next({ zaak });
      fixture.detectChanges();
      const reloadDocumenten = jest
        .spyOn(
          fixture.componentInstance.zaakDocumentenComponent,
          "updateDocumentList",
        )
        .mockResolvedValue(undefined);
      const invalidateQueries = jest.spyOn(
        testQueryClient,
        "invalidateQueries",
      );

      fixture.componentInstance["updateDocumentList"]();

      expect(reloadDocumenten).toHaveBeenCalled();
      expect(invalidateQueries).toHaveBeenCalledWith({
        queryKey: zakenService.listHistorieVoorZaakQuery(zaak.uuid).queryKey,
      });
    });
  });

  describe("wiring between the zaak view and the details card", () => {
    const detailsCard = () =>
      fixture.debugElement.query(By.directive(ZaakDetailsCardComponent))
        .componentInstance as ZaakDetailsCardComponent;

    let openSideNav: jest.SpyInstance;

    beforeEach(() => {
      mockActivatedRoute.data.next({
        zaak: {
          ...zaak,
          rechten: { ...zaak.rechten, wijzigen: true, wijzigenLocatie: true },
        },
      });
      fixture.detectChanges();
      openSideNav = jest
        .spyOn(fixture.componentInstance.actionsSidenav, "open")
        .mockResolvedValue("open");
    });

    it("opens the wijzigen side action when the card asks to edit the zaak", () => {
      detailsCard().editCaseDetails.emit();

      expect(openSideNav).toHaveBeenCalled();
      expect(sideActions.activeAction()).toBe("actie.zaak.wijzigen");
    });

    it("opens the locatie side action when the card asks to edit the locatie", () => {
      detailsCard().editLocationDetails.emit();

      expect(openSideNav).toHaveBeenCalled();
      expect(sideActions.activeAction()).toBe("actie.zaak.locatie.koppelen");
    });

    it("opens the ontkoppelen dialog for the gerelateerde zaak the card reports", () => {
      const dialog = TestBed.inject(MatDialog);
      jest.mocked(dialog.open).mockClear();

      detailsCard().zaakOntkoppelen.emit(
        fromPartial<GeneratedType<"RestGerelateerdeZaak">>({
          identificatie: "fakeGerelateerdeZaakIdentificatie",
          relatieType: "VERVOLG",
        }),
      );

      expect(jest.mocked(dialog.open).mock.calls.at(-1)![1]).toMatchObject({
        data: {
          zaakUuid: zaak.uuid,
          gekoppeldeZaakIdentificatie: "fakeGerelateerdeZaakIdentificatie",
          relatieType: "VERVOLG",
        },
      });
    });

    it("opens the verwijder dialog for the bag object the card reports", () => {
      const zaakDialogService = TestBed.inject(ZaakDialogService);
      const openVerwijderBagObject = jest
        .spyOn(zaakDialogService, "openVerwijderBagObject")
        .mockReturnValue(
          fromPartial<MatDialogRef<RedenDialogFormComponent>>({
            afterClosed: () => of(undefined),
          }),
        );

      detailsCard().bagObjectVerwijderen.emit(
        fromPartial<GeneratedType<"RESTBAGObjectGegevens">>({
          uuid: "fakeBagObjectGegevensUuid",
          zaakobject: fromPartial<GeneratedType<"RESTBAGObject">>({
            omschrijving: "fakeBagObjectOmschrijving",
          }),
        }),
      );

      expect(openVerwijderBagObject).toHaveBeenCalledWith(
        "fakeBagObjectOmschrijving",
        expect.any(Function),
      );
    });
  });
});

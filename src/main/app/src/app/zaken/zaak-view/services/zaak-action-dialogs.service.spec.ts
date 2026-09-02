/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { MatSidenav } from "@angular/material/sidenav";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { of, Subject } from "rxjs";
import { testQueryClient } from "../../../../../setupJest";
import { fromPartial } from "../../../../test-helpers";
import { UtilService } from "../../../core/service/util.service";
import { ActieOnmogelijkDialogComponent } from "../../../fout-afhandeling/dialog/actie-onmogelijk-dialog.component";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { TakenService } from "../../../taken/taken.service";
import { IntakeAfrondenDialogComponent } from "../../intake-afronden-dialog/intake-afronden-dialog.component";
import { ZaakAfhandelenDialogComponent } from "../../zaak-afhandelen-dialog/zaak-afhandelen-dialog.component";
import { ZaakBrondatumZettenDialogComponent } from "../../zaak-brondatum-zetten-dialog/zaak-brondatum-zetten-dialog.component";
import { ZaakDialogService } from "../../zaak-dialog.service";
import { ZaakOntkoppelenDialogComponent } from "../../zaak-ontkoppelen/zaak-ontkoppelen-dialog.component";
import { ZaakVerlengenDialogComponent } from "../../zaak-verlengen-dialog/zaak-verlengen-dialog.component";
import { ZakenService } from "../../zaken.service";
import { ZaakActionDialogsService } from "./zaak-action-dialogs.service";
import { ZaakSideActionService } from "./zaak-side-action.service";

describe(ZaakActionDialogsService.name, () => {
  let service: ZaakActionDialogsService;
  let sideActions: ZaakSideActionService;
  let zakenService: ZakenService;
  let takenService: TakenService;
  let invalidateSpy: jest.SpyInstance;
  let utilService: UtilService;
  let zaakDialogService: ZaakDialogService;
  let openDialog: jest.SpyInstance;
  let sidenav: { open: jest.Mock; close: jest.Mock };
  let closed: Subject<unknown>;

  const zaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "fakeZaakUuid",
    zaaktype: fromPartial({ uuid: "fakeZaaktypeUuid" }),
  });

  /** Every dialog these tests open closes through the shared `closed` subject. */
  function dialogRefClosingWith<T>() {
    return fromPartial<MatDialogRef<T>>({
      afterClosed: () => closed.asObservable(),
    });
  }

  function closedWith(result: unknown) {
    closed.next(result);
    closed.complete();
  }

  beforeEach(() => {
    closed = new Subject<unknown>();

    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        ZaakSideActionService,
        ZaakActionDialogsService,
      ],
    });

    openDialog = jest
      .spyOn(TestBed.inject(MatDialog), "open")
      .mockImplementation(() => dialogRefClosingWith());

    service = TestBed.inject(ZaakActionDialogsService);
    sideActions = TestBed.inject(ZaakSideActionService);
    zakenService = TestBed.inject(ZakenService);
    takenService = TestBed.inject(TakenService);
    utilService = TestBed.inject(UtilService);
    zaakDialogService = TestBed.inject(ZaakDialogService);

    sidenav = { open: jest.fn(), close: jest.fn() };
    sideActions.register(fromPartial<MatSidenav>(sidenav));

    jest.spyOn(utilService, "openSnackbar").mockImplementation();
    jest.spyOn(zakenService, "cacheZaak").mockImplementation();
    invalidateSpy = jest.spyOn(testQueryClient, "invalidateQueries");
  });

  describe("openAfsluiten", () => {
    it("closes the side action panel before opening the dialog", () => {
      service.openAfsluiten(zaak);

      expect(sidenav.close).toHaveBeenCalled();
      expect(openDialog).toHaveBeenCalledWith(ZaakAfhandelenDialogComponent, {
        data: { zaak },
      });
    });

    it("refetches the zaak, refreshes the taken and reports success when confirmed", () => {
      service.openAfsluiten(zaak);

      closedWith(true);

      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: zakenService.readZaakQuery(zaak.uuid).queryKey,
      });
      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: takenService.listTakenVoorZaakQuery(zaak.uuid).queryKey,
      });
      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.zaak.afgesloten",
      );
    });

    it("only forgets the active panel when the dialog is cancelled", () => {
      sideActions.activeAction.set("actie.zaak.afsluiten");
      service.openAfsluiten(zaak);

      closedWith(undefined);

      expect(sideActions.activeAction()).toBeNull();
      expect(utilService.openSnackbar).not.toHaveBeenCalled();
      expect(invalidateSpy).not.toHaveBeenCalledWith({
        queryKey: zakenService.readZaakQuery(zaak.uuid).queryKey,
      });
    });
  });

  describe("openAfbreken", () => {
    it("refuses on an opgeschorte zaak by showing the actie-onmogelijk dialog instead", () => {
      const afbrekenSpy = jest.spyOn(zaakDialogService, "openAfbreken");

      service.openAfbreken(fromPartial({ ...zaak, isOpgeschort: true }));

      expect(openDialog).toHaveBeenCalledWith(ActieOnmogelijkDialogComponent);
      expect(afbrekenSpy).not.toHaveBeenCalled();
    });

    it("caches the zaak the dialog returns rather than refetching it", () => {
      const returnedZaak = fromPartial<GeneratedType<"RestZaak">>({
        uuid: zaak.uuid,
      });
      jest
        .spyOn(zaakDialogService, "openAfbreken")
        .mockReturnValue(dialogRefClosingWith());

      service.openAfbreken(zaak);
      closedWith(returnedZaak);

      expect(zakenService.cacheZaak).toHaveBeenCalledWith(returnedZaak);
      expect(invalidateSpy).not.toHaveBeenCalledWith({
        queryKey: zakenService.readZaakQuery(zaak.uuid).queryKey,
      });
    });

    it("refetches the zaak when the dialog only confirms", () => {
      jest
        .spyOn(zaakDialogService, "openAfbreken")
        .mockReturnValue(dialogRefClosingWith());

      service.openAfbreken(zaak);
      closedWith(true);

      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: zakenService.readZaakQuery(zaak.uuid).queryKey,
      });
      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.zaak.afgebroken",
      );
    });
  });

  describe("loadOpschorting", () => {
    it("fetches the details for a zaak that is opgeschort", () => {
      const opschorting = fromPartial<GeneratedType<"RESTZaakOpschorting">>({
        duurDagen: 14,
      });
      const readSpy = jest
        .spyOn(zakenService, "readOpschortingZaak")
        .mockReturnValue(of(opschorting));

      service.loadOpschorting(fromPartial({ ...zaak, isOpgeschort: true }));

      expect(readSpy).toHaveBeenCalledWith(zaak.uuid);
      expect(service.opschorting()).toBe(opschorting);
    });

    it("does not call the backend for a zaak that is not opgeschort", () => {
      const readSpy = jest.spyOn(zakenService, "readOpschortingZaak");

      service.loadOpschorting(zaak);

      expect(readSpy).not.toHaveBeenCalled();
      expect(service.opschorting()).toBeUndefined();
    });

    it("clears previously loaded opschorting when the zaak is no longer opgeschort", () => {
      service.opschorting.set(
        fromPartial<GeneratedType<"RESTZaakOpschorting">>({ duurDagen: 14 }),
      );

      service.loadOpschorting(fromPartial({ ...zaak, isOpgeschort: false }));

      expect(service.opschorting()).toBeUndefined();
    });
  });

  describe("openHervatten", () => {
    it("passes the expected opschort duration from the loaded opschorting", () => {
      service.opschorting.set(
        fromPartial<GeneratedType<"RESTZaakOpschorting">>({ duurDagen: 14 }),
      );
      const hervattenSpy = jest
        .spyOn(zaakDialogService, "openHervatten")
        .mockReturnValue(dialogRefClosingWith());

      service.openHervatten(zaak);

      expect(hervattenSpy).toHaveBeenCalledWith(
        expect.objectContaining({ verwachteDuur: 14 }),
        expect.any(Function),
      );
    });

    it("reloads the opschorting after a successful hervatten", () => {
      service.opschorting.set(
        fromPartial<GeneratedType<"RESTZaakOpschorting">>({ duurDagen: 14 }),
      );
      const loadSpy = jest.spyOn(service, "loadOpschorting");
      jest
        .spyOn(zaakDialogService, "openHervatten")
        .mockReturnValue(dialogRefClosingWith());

      service.openHervatten(zaak);
      closedWith(true);

      expect(utilService.openSnackbar).toHaveBeenCalledWith("msg.zaak.hervat");
      expect(loadSpy).toHaveBeenCalledWith(zaak);
    });
  });

  describe("openOpschorten", () => {
    it("caches the returned zaak without refreshing the taken", () => {
      const returnedZaak = fromPartial<GeneratedType<"RestZaak">>({
        uuid: zaak.uuid,
      });

      service.openOpschorten(zaak);
      closedWith(returnedZaak);

      expect(zakenService.cacheZaak).toHaveBeenCalledWith(returnedZaak);
      expect(invalidateSpy).not.toHaveBeenCalled();
      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.zaak.opgeschort",
      );
    });
  });

  describe("openVerlengen", () => {
    it("closes the side action panel before opening the dialog", () => {
      service.openVerlengen(zaak);

      expect(sidenav.close).toHaveBeenCalled();
      expect(openDialog).toHaveBeenCalledWith(ZaakVerlengenDialogComponent, {
        data: { zaak },
      });
    });

    it("caches the returned zaak without refreshing the taken", () => {
      const returnedZaak = fromPartial<GeneratedType<"RestZaak">>({
        uuid: zaak.uuid,
      });

      service.openVerlengen(zaak);
      closedWith(returnedZaak);

      expect(zakenService.cacheZaak).toHaveBeenCalledWith(returnedZaak);
      expect(invalidateSpy).not.toHaveBeenCalled();
      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.zaak.verlengd",
      );
    });

    it("only forgets the active panel when the dialog is cancelled", () => {
      sideActions.activeAction.set("actie.zaak.verlengen");

      service.openVerlengen(zaak);
      closedWith(undefined);

      expect(sideActions.activeAction()).toBeNull();
      expect(zakenService.cacheZaak).not.toHaveBeenCalled();
      expect(utilService.openSnackbar).not.toHaveBeenCalled();
    });
  });

  describe("openHeropenen", () => {
    it("refreshes the taken and reports success when the dialog confirms", () => {
      jest
        .spyOn(zaakDialogService, "openHeropenen")
        .mockReturnValue(dialogRefClosingWith());

      service.openHeropenen(zaak);
      closedWith(true);

      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: zakenService.readZaakQuery(zaak.uuid).queryKey,
      });
      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: takenService.listTakenVoorZaakQuery(zaak.uuid).queryKey,
      });
      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.zaak.heropend",
      );
    });
  });

  describe("openBrondatumZetten", () => {
    it("closes the side action panel before opening the dialog", () => {
      service.openBrondatumZetten(zaak);

      expect(sidenav.close).toHaveBeenCalled();
      expect(openDialog).toHaveBeenCalledWith(
        ZaakBrondatumZettenDialogComponent,
        { data: { zaak } },
      );
    });

    it("refetches the zaak, refreshes the taken and reports success when confirmed", () => {
      service.openBrondatumZetten(zaak);
      closedWith(true);

      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: zakenService.readZaakQuery(zaak.uuid).queryKey,
      });
      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: takenService.listTakenVoorZaakQuery(zaak.uuid).queryKey,
      });
      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.zaak.brondatum.gezet",
      );
    });
  });

  describe("openPlanItemStarten", () => {
    const intakePlanItem = fromPartial<GeneratedType<"RESTPlanItem">>({
      userEventListenerActie: "INTAKE_AFRONDEN",
    });

    it("opens the intake afronden dialog for an INTAKE_AFRONDEN plan item", () => {
      service.openPlanItemStarten(zaak, intakePlanItem);

      expect(openDialog).toHaveBeenCalledWith(IntakeAfrondenDialogComponent, {
        data: { zaak, planItem: intakePlanItem },
      });
    });

    it("substitutes the actie-onmogelijk dialog when afhandelen is asked of an opgeschorte zaak", () => {
      const opgeschorteZaak = fromPartial<GeneratedType<"RestZaak">>({
        ...zaak,
        isOpgeschort: true,
      });
      const planItem = fromPartial<GeneratedType<"RESTPlanItem">>({
        userEventListenerActie: "ZAAK_AFHANDELEN",
      });

      service.openPlanItemStarten(opgeschorteZaak, planItem);

      expect(openDialog).toHaveBeenCalledWith(ActieOnmogelijkDialogComponent, {
        data: { zaak: opgeschorteZaak, planItem },
      });
    });

    it("throws on a plan item whose actie it does not know", () => {
      expect(() =>
        service.openPlanItemStarten(
          zaak,
          fromPartial<GeneratedType<"RESTPlanItem">>({
            userEventListenerActie: undefined,
          }),
        ),
      ).toThrow("Niet bestaande UserEventListenerActie");
    });

    it("switches to the besluit vastleggen panel when the dialog asks for it", () => {
      service.openPlanItemStarten(zaak, intakePlanItem);

      closedWith("openBesluitVastleggen");

      expect(sideActions.activeAction()).toBe("actie.besluit.vastleggen");
      expect(sidenav.open).toHaveBeenCalled();
      expect(utilService.openSnackbar).not.toHaveBeenCalled();
    });

    it("reports the executed plan item and refetches the zaak otherwise", () => {
      service.openPlanItemStarten(zaak, intakePlanItem);

      closedWith(true);

      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.planitem.uitgevoerd.INTAKE_AFRONDEN",
      );
      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: zakenService.readZaakQuery(zaak.uuid).queryKey,
      });
    });
  });

  describe("openZaakOntkoppelen", () => {
    const gerelateerdeZaak = fromPartial<GeneratedType<"RestGerelateerdeZaak">>(
      {
        identificatie: "ZAAK-002",
        relatieType: "HOOFDZAAK",
      },
    );

    it("hands the dialog the zaak uuid and the relation it should undo", () => {
      service.openZaakOntkoppelen(zaak, gerelateerdeZaak);

      expect(openDialog).toHaveBeenCalledWith(ZaakOntkoppelenDialogComponent, {
        data: {
          zaakUuid: zaak.uuid,
          gekoppeldeZaakIdentificatie: "ZAAK-002",
          relatieType: "HOOFDZAAK",
        },
      });
    });

    it("refetches the zaak and reports success when confirmed", () => {
      service.openZaakOntkoppelen(zaak, gerelateerdeZaak);

      closedWith(true);

      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: zakenService.readZaakQuery(zaak.uuid).queryKey,
      });
      expect(utilService.openSnackbar).toHaveBeenCalledWith(
        "msg.zaak.ontkoppelen.uitgevoerd",
      );
    });
  });
});
